package com.nexacore.systemmodule.layout.service.implementations;

import com.nexacore.gatewaymodule.auth.service.interfaces.AuthModuleGateway;
import com.nexacore.systemmodule.accesscontrol.entity.SysAccClientApplication;
import com.nexacore.systemmodule.accesscontrol.enums.ClientApplicationStatus;
import com.nexacore.systemmodule.accesscontrol.repository.ClientApplicationRepository;
import com.nexacore.systemmodule.accesscontrol.repository.ClientFeaturePermissionRepository;
import com.nexacore.systemmodule.layout.dto.LayoutNavigationCategoryOrderRequestDto;
import com.nexacore.systemmodule.layout.dto.LayoutNavigationNodeRequestDto;
import com.nexacore.systemmodule.layout.dto.NavNodeDto;
import com.nexacore.systemmodule.layout.dto.LayoutNavigationIntegrityDto;
import com.nexacore.systemmodule.layout.entity.LayoutAuditInfo;
import com.nexacore.systemmodule.layout.entity.SysLayoutFeature;
import com.nexacore.systemmodule.layout.entity.SysLayoutFeatureGroup;
import com.nexacore.systemmodule.layout.entity.SysLayoutFeaturePrivilege;
import com.nexacore.systemmodule.layout.entity.SysLayoutModuleGroup;
import com.nexacore.systemmodule.layout.entity.SysLayoutNavigationCategory;
import com.nexacore.systemmodule.layout.entity.SysLayoutNavigationModule;
import com.nexacore.systemmodule.layout.entity.SysLayoutRoutePolicy;
import com.nexacore.systemmodule.layout.enums.PrivilegeMatchMode;
import com.nexacore.systemmodule.layout.repository.LayoutFeatureGroupRepository;
import com.nexacore.systemmodule.layout.repository.LayoutFeaturePrivilegeRepository;
import com.nexacore.systemmodule.layout.repository.LayoutFeatureRepository;
import com.nexacore.systemmodule.layout.repository.LayoutModuleGroupRepository;
import com.nexacore.systemmodule.layout.repository.LayoutNavigationCategoryRepository;
import com.nexacore.systemmodule.layout.repository.LayoutNavigationModuleRepository;
import com.nexacore.systemmodule.layout.repository.LayoutRoutePolicyRepository;
import com.nexacore.systemmodule.layout.service.interfaces.LayoutCodeGenerationService;
import com.nexacore.systemmodule.layout.service.interfaces.LayoutNavigationService;
import com.nexacore.systemmodule.privilege.catalog.entity.SysPrivPrivilege;
import com.nexacore.systemmodule.privilege.catalog.repository.PrivilegeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Objects;
import java.util.function.Function;
import org.springframework.dao.OptimisticLockingFailureException;

@Service
@RequiredArgsConstructor
public class LayoutNavigationServiceImpl implements LayoutNavigationService {
    private final LayoutModuleGroupRepository moduleGroupRepository;
    private final LayoutNavigationModuleRepository navigationModuleRepository;
    private final LayoutNavigationCategoryRepository categoryRepository;
    private final LayoutFeatureGroupRepository featureGroupRepository;
    private final LayoutFeatureRepository featureRepository;
    private final LayoutFeaturePrivilegeRepository featurePrivilegeRepository;
    private final LayoutRoutePolicyRepository layoutRoutePolicyRepository;
    private final ClientApplicationRepository clientApplicationRepository;
    private final ClientFeaturePermissionRepository clientFeaturePermissionRepository;
    private final PrivilegeRepository privilegeRepository;
    private final LayoutCodeGenerationService codeGenerationService;
    private final AuthModuleGateway authModuleGateway;

    /**
     * Fetches each of the 5 levels in one query (instead of one query per parent,
     * which was O(nodes) queries), groups children by parent id, then builds the
     * tree top-down. Each level's node-building is its own small method so no
     * single method nests more than one loop deep.
     */
    @Override
    @Transactional(transactionManager = "systemTransactionManager", readOnly = true)
    public List<NavNodeDto> getNavigationTree(String clientCode, String username, Set<String> privilegeCodes) {
        Set<String> effectivePrivilegeCodes = resolveClientPrivileges(clientCode, privilegeCodes);
        Map<Long, List<SysLayoutNavigationModule>> modulesByGroup = navigationModuleRepository
                .findByActiveTrueOrderByDisplayOrderAscNavigationModuleNameAsc().stream()
                .collect(Collectors.groupingBy(module -> module.getModuleGroup().getId()));
        Map<Long, List<SysLayoutNavigationCategory>> categoriesByModule = categoryRepository
                .findByActiveTrueOrderByDisplayOrderAscCategoryNameAsc().stream()
                .collect(Collectors.groupingBy(category -> category.getNavigationModule().getId()));
        Map<Long, List<SysLayoutFeatureGroup>> featureGroupsByCategory = featureGroupRepository
                .findByActiveTrueOrderByDisplayOrderAscFeatureGroupNameAsc().stream()
                .collect(Collectors.groupingBy(featureGroup -> featureGroup.getNavigationCategory().getId()));
        Map<Long, List<SysLayoutFeature>> featuresByFeatureGroup = featureRepository
                .findByActiveTrueOrderByDisplayOrderAscFeatureNameAsc().stream()
                .collect(Collectors.groupingBy(feature -> feature.getFeatureGroup().getId()));

        List<NavNodeDto> groups = new ArrayList<>();
        for (SysLayoutModuleGroup group : moduleGroupRepository.findByActiveTrueOrderByDisplayOrderAscGroupNameAsc()) {
            NavNodeDto groupNode = buildModuleGroupNode(group, modulesByGroup, categoriesByModule,
                    featureGroupsByCategory, featuresByFeatureGroup, effectivePrivilegeCodes, false);
            if (!groupNode.getChildren().isEmpty()) {
                groups.add(groupNode);
            }
        }
        return groups;
    }

    private Set<String> resolveClientPrivileges(String clientCode, Set<String> userPrivilegeCodes) {
        if (clientCode == null || clientCode.isBlank()) {
            return Set.of();
        }
        SysAccClientApplication client = clientApplicationRepository.findByClientCode(clientCode.trim())
                .filter(candidate -> candidate.getStatus() == ClientApplicationStatus.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("Active client application not found: " + clientCode));
        Set<String> allowed = clientFeaturePermissionRepository.findActivePrivilegeCodesByClientApplicationId(client.getId());
        return userPrivilegeCodes.stream().filter(allowed::contains).collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Unpruned mirror of {@link #getNavigationTree}, for the admin Navigation
     * Tree CRUD page — that page needs to show/edit every node (including a
     * freshly-created, still-childless branch) rather than only the branches
     * that terminate in a privilege-visible feature.
     */
    @Override
    @Transactional(transactionManager = "systemTransactionManager", readOnly = true)
    public List<NavNodeDto> getFullNavigationTree() {
        Map<Long, List<SysLayoutNavigationModule>> modulesByGroup = navigationModuleRepository
                .findByActiveTrueOrderByDisplayOrderAscNavigationModuleNameAsc().stream()
                .collect(Collectors.groupingBy(module -> module.getModuleGroup().getId()));
        Map<Long, List<SysLayoutNavigationCategory>> categoriesByModule = categoryRepository
                .findByActiveTrueOrderByDisplayOrderAscCategoryNameAsc().stream()
                .collect(Collectors.groupingBy(category -> category.getNavigationModule().getId()));
        Map<Long, List<SysLayoutFeatureGroup>> featureGroupsByCategory = featureGroupRepository
                .findByActiveTrueOrderByDisplayOrderAscFeatureGroupNameAsc().stream()
                .collect(Collectors.groupingBy(featureGroup -> featureGroup.getNavigationCategory().getId()));
        Map<Long, List<SysLayoutFeature>> featuresByFeatureGroup = featureRepository
                .findByActiveTrueOrderByDisplayOrderAscFeatureNameAsc().stream()
                .collect(Collectors.groupingBy(feature -> feature.getFeatureGroup().getId()));

        List<NavNodeDto> groups = new ArrayList<>();
        for (SysLayoutModuleGroup group : moduleGroupRepository.findByActiveTrueOrderByDisplayOrderAscGroupNameAsc()) {
            groups.add(buildModuleGroupNode(group, modulesByGroup, categoriesByModule,
                    featureGroupsByCategory, featuresByFeatureGroup, Set.of(), true));
        }
        return groups;
    }

    private NavNodeDto buildModuleGroupNode(SysLayoutModuleGroup group,
                                            Map<Long, List<SysLayoutNavigationModule>> modulesByGroup,
                                            Map<Long, List<SysLayoutNavigationCategory>> categoriesByModule,
                                            Map<Long, List<SysLayoutFeatureGroup>> featureGroupsByCategory,
                                            Map<Long, List<SysLayoutFeature>> featuresByFeatureGroup,
                                            Set<String> privilegeCodes, boolean includeAll) {
        NavNodeDto groupNode = node(group.getId(), null, group.getGroupCode(), null, group.getGroupName(),
                "group", group.getIcon(), null, null, group.isActive(), group.getDisplayOrder());
        for (SysLayoutNavigationModule module : modulesByGroup.getOrDefault(group.getId(), List.of())) {
            NavNodeDto moduleNode = buildModuleNode(module, categoriesByModule, featureGroupsByCategory,
                    featuresByFeatureGroup, privilegeCodes, includeAll);
            if (includeAll || !moduleNode.getChildren().isEmpty()) {
                groupNode.getChildren().add(moduleNode);
            }
        }
        return groupNode;
    }

    private NavNodeDto buildModuleNode(SysLayoutNavigationModule module,
                                       Map<Long, List<SysLayoutNavigationCategory>> categoriesByModule,
                                       Map<Long, List<SysLayoutFeatureGroup>> featureGroupsByCategory,
                                       Map<Long, List<SysLayoutFeature>> featuresByFeatureGroup,
                                       Set<String> privilegeCodes, boolean includeAll) {
        NavNodeDto moduleNode = node(module.getId(), module.getModuleGroup().getId(), module.getNavigationModuleCode(),
                null, module.getNavigationModuleName(), "module", module.getIcon(), module.getDescription(), null,
                module.isActive(), module.getDisplayOrder());
        for (SysLayoutNavigationCategory category : categoriesByModule.getOrDefault(module.getId(), List.of())) {
            NavNodeDto categoryNode = buildCategoryNode(category, featureGroupsByCategory, featuresByFeatureGroup,
                    privilegeCodes, includeAll);
            if (includeAll || !categoryNode.getChildren().isEmpty()) {
                moduleNode.getChildren().add(categoryNode);
            }
        }
        return moduleNode;
    }

    private NavNodeDto buildCategoryNode(SysLayoutNavigationCategory category,
                                         Map<Long, List<SysLayoutFeatureGroup>> featureGroupsByCategory,
                                         Map<Long, List<SysLayoutFeature>> featuresByFeatureGroup,
                                         Set<String> privilegeCodes, boolean includeAll) {
        NavNodeDto categoryNode = node(category.getId(), category.getNavigationModule().getId(), category.getCategoryCode(),
                null, category.getCategoryName(), "category", category.getIcon(), null, null,
                category.isActive(), category.getDisplayOrder());
        for (SysLayoutFeatureGroup featureGroup : featureGroupsByCategory.getOrDefault(category.getId(), List.of())) {
            NavNodeDto featureGroupNode = buildFeatureGroupNode(featureGroup, featuresByFeatureGroup, privilegeCodes, includeAll);
            if (includeAll || !featureGroupNode.getChildren().isEmpty()) {
                categoryNode.getChildren().add(featureGroupNode);
            }
        }
        return categoryNode;
    }

    private NavNodeDto buildFeatureGroupNode(SysLayoutFeatureGroup featureGroup,
                                             Map<Long, List<SysLayoutFeature>> featuresByFeatureGroup,
                                             Set<String> privilegeCodes, boolean includeAll) {
        NavNodeDto featureGroupNode = node(featureGroup.getId(), featureGroup.getNavigationCategory().getId(),
                featureGroup.getFeatureGroupCode(), null, featureGroup.getFeatureGroupName(), "featureGroup", null,
                null, null, featureGroup.isActive(), featureGroup.getDisplayOrder());
        for (SysLayoutFeature feature : featuresByFeatureGroup.getOrDefault(featureGroup.getId(), List.of())) {
            NavNodeDto featureNode = featureNode(feature, privilegeCodes, includeAll);
            if (featureNode != null) {
                featureGroupNode.getChildren().add(featureNode);
            }
        }
        return featureGroupNode;
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager")
    public NavNodeDto saveGroup(LayoutNavigationNodeRequestDto request, String actor) {
        Long userId = authModuleGateway.getUserId(actor);
        SysLayoutModuleGroup group = request.getId() == null
                ? new SysLayoutModuleGroup()
                : moduleGroupRepository.findById(request.getId())
                .orElseThrow(() -> new IllegalArgumentException("Layout module group not found: " + request.getId()));
        group.setGroupCode(codeGenerationService.normalizeBusinessCode(request.getCode(), request.getName()));
        group.setGroupName(required(request.getName(), "name"));
        group.setIcon(request.getIcon());
        group.setDisplayOrder(defaultOrder(request.getDisplayOrder()));
        group.setActive(defaultActive(request.getActive()));
        audit(group, userId);
        SysLayoutModuleGroup saved = moduleGroupRepository.save(group);
        return node(saved.getId(), null, saved.getGroupCode(), null, saved.getGroupName(), "group", saved.getIcon(),
                null, null, saved.isActive(), saved.getDisplayOrder());
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager")
    public NavNodeDto saveModule(LayoutNavigationNodeRequestDto request, String actor) {
        Long userId = authModuleGateway.getUserId(actor);
        SysLayoutModuleGroup parent = moduleGroupRepository.findById(requiredParent(request))
                .orElseThrow(() -> new IllegalArgumentException("Layout module group not found: " + request.getParentId()));
        requireActiveParent(parent.isActive(), request);
        SysLayoutNavigationModule module = request.getId() == null
                ? new SysLayoutNavigationModule()
                : navigationModuleRepository.findById(request.getId())
                .orElseThrow(() -> new IllegalArgumentException("Layout navigation module not found: " + request.getId()));
        module.setModuleGroup(parent);
        module.setNavigationModuleCode(codeGenerationService.normalizeBusinessCode(request.getCode(), request.getName()));
        module.setNavigationModuleName(required(request.getName(), "name"));
        module.setPhysicalModuleCode(request.getPhysicalModuleCode());
        module.setIcon(request.getIcon());
        module.setDescription(request.getDescription());
        module.setDisplayOrder(defaultOrder(request.getDisplayOrder()));
        module.setActive(defaultActive(request.getActive()));
        audit(module, userId);
        SysLayoutNavigationModule saved = navigationModuleRepository.save(module);
        return node(saved.getId(), parent.getId(), saved.getNavigationModuleCode(), null, saved.getNavigationModuleName(),
                "module", saved.getIcon(), saved.getDescription(), null, saved.isActive(), saved.getDisplayOrder());
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager")
    public NavNodeDto saveCategory(LayoutNavigationNodeRequestDto request, String actor) {
        Long userId = authModuleGateway.getUserId(actor);
        SysLayoutNavigationModule parent = navigationModuleRepository.findById(requiredParent(request))
                .orElseThrow(() -> new IllegalArgumentException("Layout navigation module not found: " + request.getParentId()));
        requireActiveParent(parent.isActive(), request);
        SysLayoutNavigationCategory category = request.getId() == null
                ? new SysLayoutNavigationCategory()
                : categoryRepository.findById(request.getId())
                .orElseThrow(() -> new IllegalArgumentException("Layout navigation category not found: " + request.getId()));
        category.setNavigationModule(parent);
        category.setCategoryCode(codeGenerationService.normalizeBusinessCode(request.getCode(), request.getName()));
        category.setCategoryName(required(request.getName(), "name"));
        category.setCategoryKind(request.getCategoryKind());
        category.setIcon(request.getIcon());
        category.setDisplayOrder(defaultOrder(request.getDisplayOrder()));
        category.setActive(defaultActive(request.getActive()));
        audit(category, userId);
        SysLayoutNavigationCategory saved = categoryRepository.save(category);
        return node(saved.getId(), parent.getId(), saved.getCategoryCode(), null, saved.getCategoryName(), "category",
                saved.getIcon(), null, null, saved.isActive(), saved.getDisplayOrder());
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager", readOnly = true)
    public List<NavNodeDto> listCategories() {
        return categoryRepository.findAll().stream()
                .map(category -> node(category.getId(), category.getNavigationModule().getId(), category.getCategoryCode(),
                        null, category.getCategoryName(), "category", category.getIcon(), null, null,
                        category.isActive(), category.getDisplayOrder()))
                .toList();
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager")
    public void reorderCategories(LayoutNavigationCategoryOrderRequestDto request, String actor) {
        Long userId = authModuleGateway.getUserId(actor);
        Set<Long> requestedIds = request.getCategories().stream().map(LayoutNavigationCategoryOrderRequestDto.CategoryOrder::getId)
                .collect(Collectors.toSet());
        if (requestedIds.size() != request.getCategories().size()) {
            throw new IllegalArgumentException("Category reorder contains duplicate ids");
        }
        List<SysLayoutNavigationCategory> requestedCategories = categoryRepository.findAllById(requestedIds);
        if (requestedCategories.size() != requestedIds.size()
                || requestedCategories.stream().map(category -> category.getNavigationModule().getId()).distinct().count() > 1) {
            throw new IllegalArgumentException("Category reorder must reference existing siblings");
        }
        for (LayoutNavigationCategoryOrderRequestDto.CategoryOrder order : request.getCategories()) {
            SysLayoutNavigationCategory category = categoryRepository.findById(order.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Layout navigation category not found: " + order.getId()));
            category.setDisplayOrder(defaultOrder(order.getDisplayOrder()));
            audit(category, userId);
            categoryRepository.save(category);
        }
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager")
    public NavNodeDto saveFeatureGroup(LayoutNavigationNodeRequestDto request, String actor) {
        Long userId = authModuleGateway.getUserId(actor);
        SysLayoutNavigationCategory parent = categoryRepository.findById(requiredParent(request))
                .orElseThrow(() -> new IllegalArgumentException("Layout navigation category not found: " + request.getParentId()));
        requireActiveParent(parent.isActive(), request);
        SysLayoutFeatureGroup featureGroup = request.getId() == null
                ? new SysLayoutFeatureGroup()
                : featureGroupRepository.findById(request.getId())
                .orElseThrow(() -> new IllegalArgumentException("Layout feature group not found: " + request.getId()));
        featureGroup.setNavigationCategory(parent);
        featureGroup.setFeatureGroupCode(codeGenerationService.normalizeBusinessCode(request.getCode(), request.getName()));
        featureGroup.setFeatureGroupName(required(request.getName(), "name"));
        featureGroup.setDisplayOrder(defaultOrder(request.getDisplayOrder()));
        featureGroup.setActive(defaultActive(request.getActive()));
        audit(featureGroup, userId);
        SysLayoutFeatureGroup saved = featureGroupRepository.save(featureGroup);
        return node(saved.getId(), parent.getId(), saved.getFeatureGroupCode(), null, saved.getFeatureGroupName(),
                "featureGroup", null, null, null, saved.isActive(), saved.getDisplayOrder());
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager")
    public NavNodeDto saveFeature(LayoutNavigationNodeRequestDto request, String actor) {
        Long userId = authModuleGateway.getUserId(actor);
        SysLayoutFeatureGroup parent = featureGroupRepository.findById(requiredParent(request))
                .orElseThrow(() -> new IllegalArgumentException("Layout feature group not found: " + request.getParentId()));
        requireActiveParent(parent.isActive(), request);
        SysLayoutFeature feature = request.getId() == null
                ? new SysLayoutFeature()
                : featureRepository.findById(request.getId())
                .orElseThrow(() -> new IllegalArgumentException("Layout feature not found: " + request.getId()));
        if (feature.getId() != null && !Objects.equals(feature.getVersion(), request.getVersion())) {
            throw new OptimisticLockingFailureException("Layout feature changed since it was loaded");
        }
        String tCode = codeGenerationService.normalizeTCode(request.getTCode());
        if (tCode == null) {
            tCode = feature.getId() == null ? generateTCode() : feature.getTCode();
        }
        codeGenerationService.validateTCode(tCode);
        feature.setFeatureGroup(parent);
        feature.setFeatureCode(codeGenerationService.normalizeBusinessCode(request.getCode(), request.getName()));
        feature.setTCode(tCode);
        feature.setFeatureName(required(request.getName(), "name"));
        String route = required(request.getRoute(), "route");
        if (defaultActive(request.getActive()) && layoutRoutePolicyRepository.findAll().stream()
                .noneMatch(policy -> policy.isActive() && route.equals(policy.getRouteUrl()))) {
            throw new IllegalArgumentException("Active feature route has no active route policy: " + route);
        }
        if (featureRepository.findAll().stream().anyMatch(candidate -> candidate.isActive()
                && !Objects.equals(candidate.getId(), feature.getId()) && route.equals(candidate.getRoute()))) {
            throw new IllegalArgumentException("Active feature route is already assigned: " + route);
        }
        feature.setRoute(route);
        feature.setIcon(request.getIcon());
        feature.setPhysicalModuleCode(request.getPhysicalModuleCode());
        feature.setPhysicalSubmoduleCode(request.getPhysicalSubmoduleCode());
        feature.setPhysicalFeatureTypeCode(request.getPhysicalFeatureTypeCode());
        feature.setPhysicalFeatureCode(request.getPhysicalFeatureCode());
        feature.setDisplayOrder(defaultOrder(request.getDisplayOrder()));
        feature.setActive(defaultActive(request.getActive()));
        audit(feature, userId);
        SysLayoutFeature saved = featureRepository.save(feature);
        syncFeaturePrivileges(saved, request.getPrivilegeCodes(), userId);
        return featureNode(saved, Set.copyOf(request.getPrivilegeCodes()), true);
    }

    private String generateTCode() {
        String candidate;
        do {
            candidate = "T" + featureRepository.nextTCodeValue();
        } while (featureRepository.existsActiveTCode(candidate));
        return candidate;
    }

    private NavNodeDto featureNode(SysLayoutFeature feature, Set<String> userPrivilegeCodes, boolean includeAll) {
        List<SysLayoutFeaturePrivilege> links = featurePrivilegeRepository.findByLayoutFeatureIdAndActiveTrue(feature.getId());
        List<String> requiredCodes = links.stream()
                .map(link -> link.getPrivilege().getPrivilegeCode())
                .toList();

        if (!includeAll && !isVisible(links, requiredCodes, userPrivilegeCodes)) {
            return null;
        }

        return NavNodeDto.builder()
                .id(feature.getId())
                .version(feature.getVersion())
                .parentId(feature.getFeatureGroup().getId())
                .code(feature.getFeatureCode())
                .tCode(feature.getTCode())
                .label(feature.getFeatureName())
                .type("feature")
                .icon(feature.getIcon())
                .route(feature.getRoute())
                .active(feature.isActive())
                .displayOrder(feature.getDisplayOrder())
                .privilegeCodes(requiredCodes)
                .children(new ArrayList<>())
                .build();
    }

    private boolean isVisible(List<SysLayoutFeaturePrivilege> links, List<String> requiredCodes, Set<String> userPrivilegeCodes) {
        if (links.isEmpty()) {
            return true;
        }
        boolean allMode = links.stream().anyMatch(link -> link.getMatchMode() == PrivilegeMatchMode.ALL);
        if (allMode) {
            return userPrivilegeCodes.containsAll(requiredCodes);
        }
        return requiredCodes.stream().anyMatch(userPrivilegeCodes::contains);
    }

    private NavNodeDto node(Long id, Long parentId, String code, String tCode, String label, String type, String icon,
                            String description, String route, boolean active, Integer displayOrder) {
        return NavNodeDto.builder()
                .id(id)
                .parentId(parentId)
                .code(code)
                .tCode(tCode)
                .label(label)
                .type(type)
                .icon(icon)
                .moduleGroupIconName("group".equals(type) ? icon : null)
                .moduleIconName("module".equals(type) ? icon : null)
                .description(description)
                .route(route)
                .active(active)
                .displayOrder(displayOrder)
                .children(new ArrayList<>())
                .privilegeCodes(new ArrayList<>())
                .build();
    }

    private void syncFeaturePrivileges(SysLayoutFeature feature, List<String> privilegeCodes, Long userId) {
        Set<String> distinctCodes = (privilegeCodes == null ? List.<String>of() : privilegeCodes).stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(code -> !code.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<SysPrivPrivilege> privileges = privilegeRepository.findByPrivilegeCodeIn(distinctCodes);
        if (privileges.size() != distinctCodes.size()) {
            Set<String> found = privileges.stream().map(SysPrivPrivilege::getPrivilegeCode).collect(Collectors.toSet());
            Set<String> missing = distinctCodes.stream().filter(code -> !found.contains(code)).collect(Collectors.toSet());
            throw new IllegalArgumentException("Privileges not found: " + missing);
        }
        featurePrivilegeRepository.deleteByLayoutFeatureId(feature.getId());
        for (SysPrivPrivilege privilege : privileges) {
            SysLayoutFeaturePrivilege link = SysLayoutFeaturePrivilege.builder()
                    .layoutFeature(feature)
                    .privilege(privilege)
                    .matchMode(PrivilegeMatchMode.ANY)
                    .active(true)
                    .build();
            link.setCreatedBy(userId);
            link.setUpdatedBy(userId);
            featurePrivilegeRepository.save(link);
        }
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager", readOnly = true)
    public LayoutNavigationIntegrityDto diagnoseIntegrity() {
        List<String> invalidParents = new ArrayList<>();
        navigationModuleRepository.findAll().stream()
                .filter(node -> node.isActive() && !node.getModuleGroup().isActive())
                .forEach(node -> invalidParents.add("module:" + node.getNavigationModuleCode()));
        categoryRepository.findAll().stream()
                .filter(node -> node.isActive() && !node.getNavigationModule().isActive())
                .forEach(node -> invalidParents.add("category:" + node.getCategoryCode()));
        featureGroupRepository.findAll().stream()
                .filter(node -> node.isActive() && !node.getNavigationCategory().isActive())
                .forEach(node -> invalidParents.add("feature-group:" + node.getFeatureGroupCode()));
        List<SysLayoutFeature> features = featureRepository.findAll();
        features.stream().filter(node -> node.isActive() && !node.getFeatureGroup().isActive())
                .forEach(node -> invalidParents.add("feature:" + node.getFeatureCode()));

        List<String> duplicateCodes = new ArrayList<>();
        collectDuplicates(moduleGroupRepository.findAll(), SysLayoutModuleGroup::getGroupCode, "group", duplicateCodes);
        collectDuplicates(navigationModuleRepository.findAll(), SysLayoutNavigationModule::getNavigationModuleCode, "module", duplicateCodes);
        collectDuplicates(categoryRepository.findAll(), SysLayoutNavigationCategory::getCategoryCode, "category", duplicateCodes);
        collectDuplicates(featureGroupRepository.findAll(), SysLayoutFeatureGroup::getFeatureGroupCode, "feature-group", duplicateCodes);
        collectDuplicates(features, SysLayoutFeature::getFeatureCode, "feature", duplicateCodes);

        List<String> duplicateRoutes = new ArrayList<>();
        collectDuplicates(features.stream().filter(SysLayoutFeature::isActive).toList(), SysLayoutFeature::getRoute, "route", duplicateRoutes);
        Set<String> activeRoutes = features.stream().filter(SysLayoutFeature::isActive).map(SysLayoutFeature::getRoute).collect(Collectors.toSet());
        List<String> unknownRoutes = new ArrayList<>();
        // A feature route without a route policy is inaccessible by design and therefore invalid.
        // Route-policy-only entries are permitted for pages that are intentionally absent from navigation.
        Set<String> policyRoutes = layoutRoutePolicyRepository.findAll().stream().filter(SysLayoutRoutePolicy::isActive)
                .map(SysLayoutRoutePolicy::getRouteUrl).collect(Collectors.toSet());
        activeRoutes.stream().filter(route -> !policyRoutes.contains(route)).sorted().forEach(unknownRoutes::add);

        List<String> invalidPrivilegeReferences = featurePrivilegeRepository.findAll().stream()
                .filter(link -> link.isActive() && !link.getPrivilege().isActive())
                .map(link -> link.getLayoutFeature().getFeatureCode() + ":" + link.getPrivilege().getPrivilegeCode())
                .sorted().toList();
        boolean valid = invalidParents.isEmpty() && duplicateCodes.isEmpty() && duplicateRoutes.isEmpty()
                && unknownRoutes.isEmpty() && invalidPrivilegeReferences.isEmpty();
        return LayoutNavigationIntegrityDto.builder().valid(valid).invalidParents(invalidParents)
                .duplicateCodes(duplicateCodes).duplicateRoutes(duplicateRoutes).unknownRoutes(unknownRoutes)
                .invalidPrivilegeReferences(invalidPrivilegeReferences).build();
    }

    private <T> void collectDuplicates(List<T> values, Function<T, String> keyOf, String prefix, List<String> target) {
        values.stream().map(keyOf).filter(Objects::nonNull)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream().filter(entry -> entry.getValue() > 1).map(entry -> prefix + ":" + entry.getKey())
                .sorted().forEach(target::add);
    }

    private void audit(LayoutAuditInfo entity, Long userId) {
        if (entity.getCreatedBy() == null) {
            entity.setCreatedBy(userId);
        }
        entity.setUpdatedBy(userId);
    }

    private Long requiredParent(LayoutNavigationNodeRequestDto request) {
        if (request.getParentId() == null) {
            throw new IllegalArgumentException("parentId is required");
        }
        return request.getParentId();
    }

    private String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private Integer defaultOrder(Integer value) {
        return value == null ? 100 : value;
    }

    private boolean defaultActive(Boolean value) {
        return value == null || value;
    }

    private void requireActiveParent(boolean parentActive, LayoutNavigationNodeRequestDto request) {
        if (defaultActive(request.getActive()) && !parentActive) {
            throw new IllegalArgumentException("An active navigation node requires an active parent");
        }
    }
}
