package com.nexacore.systemmodule.layout.service.implementations;

import com.nexacore.gatewaymodule.auth.service.interfaces.AuthModuleGateway;
import com.nexacore.systemmodule.layout.dto.LayoutNavigationCategoryOrderRequestDto;
import com.nexacore.systemmodule.layout.dto.LayoutNavigationNodeRequestDto;
import com.nexacore.systemmodule.layout.dto.NavNodeDto;
import com.nexacore.systemmodule.layout.entity.LayoutAuditInfo;
import com.nexacore.systemmodule.layout.entity.SysLayoutFeature;
import com.nexacore.systemmodule.layout.entity.SysLayoutFeatureGroup;
import com.nexacore.systemmodule.layout.entity.SysLayoutFeaturePrivilege;
import com.nexacore.systemmodule.layout.entity.SysLayoutModuleGroup;
import com.nexacore.systemmodule.layout.entity.SysLayoutNavigationCategory;
import com.nexacore.systemmodule.layout.entity.SysLayoutNavigationModule;
import com.nexacore.systemmodule.layout.enums.PrivilegeMatchMode;
import com.nexacore.systemmodule.layout.repository.LayoutFeatureGroupRepository;
import com.nexacore.systemmodule.layout.repository.LayoutFeaturePrivilegeRepository;
import com.nexacore.systemmodule.layout.repository.LayoutFeatureRepository;
import com.nexacore.systemmodule.layout.repository.LayoutModuleGroupRepository;
import com.nexacore.systemmodule.layout.repository.LayoutNavigationCategoryRepository;
import com.nexacore.systemmodule.layout.repository.LayoutNavigationModuleRepository;
import com.nexacore.systemmodule.layout.service.interfaces.LayoutCodeGenerationService;
import com.nexacore.systemmodule.layout.service.interfaces.LayoutNavigationService;
import com.nexacore.systemmodule.privilege.catalog.entity.SysPrivPrivilege;
import com.nexacore.systemmodule.privilege.catalog.repository.PrivilegeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class LayoutNavigationServiceImpl implements LayoutNavigationService {
    private final LayoutModuleGroupRepository moduleGroupRepository;
    private final LayoutNavigationModuleRepository navigationModuleRepository;
    private final LayoutNavigationCategoryRepository categoryRepository;
    private final LayoutFeatureGroupRepository featureGroupRepository;
    private final LayoutFeatureRepository featureRepository;
    private final LayoutFeaturePrivilegeRepository featurePrivilegeRepository;
    private final PrivilegeRepository privilegeRepository;
    private final LayoutCodeGenerationService codeGenerationService;
    private final AuthModuleGateway authModuleGateway;

    @Override
    @Transactional(transactionManager = "systemTransactionManager", readOnly = true)
    public List<NavNodeDto> getNavigationTree(String clientCode, String username, Set<String> privilegeCodes) {
        List<NavNodeDto> groups = new ArrayList<>();
        for (SysLayoutModuleGroup group : moduleGroupRepository.findByActiveTrueOrderByDisplayOrderAscGroupNameAsc()) {
            NavNodeDto groupNode = node(group.getGroupCode(), null, group.getGroupName(), "group", group.getIcon(), null, null);
            for (SysLayoutNavigationModule module : navigationModuleRepository.findByModuleGroupIdAndActiveTrueOrderByDisplayOrderAscNavigationModuleNameAsc(group.getId())) {
                NavNodeDto moduleNode = node(module.getNavigationModuleCode(), null, module.getNavigationModuleName(), "module", module.getIcon(), module.getDescription(), null);
                for (SysLayoutNavigationCategory category : categoryRepository.findByNavigationModuleIdAndActiveTrueOrderByDisplayOrderAscCategoryNameAsc(module.getId())) {
                    NavNodeDto categoryNode = node(category.getCategoryCode(), null, category.getCategoryName(), "category", category.getIcon(), null, null);
                    for (SysLayoutFeatureGroup featureGroup : featureGroupRepository.findByNavigationCategoryIdAndActiveTrueOrderByDisplayOrderAscFeatureGroupNameAsc(category.getId())) {
                        NavNodeDto featureGroupNode = node(featureGroup.getFeatureGroupCode(), null, featureGroup.getFeatureGroupName(), "featureGroup", null, null, null);
                        for (SysLayoutFeature feature : featureRepository.findByFeatureGroupIdAndActiveTrueOrderByDisplayOrderAscFeatureNameAsc(featureGroup.getId())) {
                            NavNodeDto featureNode = featureNode(feature, privilegeCodes);
                            if (featureNode != null) {
                                featureGroupNode.getChildren().add(featureNode);
                            }
                        }
                        if (!featureGroupNode.getChildren().isEmpty()) {
                            categoryNode.getChildren().add(featureGroupNode);
                        }
                    }
                    if (!categoryNode.getChildren().isEmpty()) {
                        moduleNode.getChildren().add(categoryNode);
                    }
                }
                if (!moduleNode.getChildren().isEmpty()) {
                    groupNode.getChildren().add(moduleNode);
                }
            }
            if (!groupNode.getChildren().isEmpty()) {
                groups.add(groupNode);
            }
        }
        return groups;
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
        return node(saved.getGroupCode(), null, saved.getGroupName(), "group", saved.getIcon(), null, null);
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager")
    public NavNodeDto saveModule(LayoutNavigationNodeRequestDto request, String actor) {
        Long userId = authModuleGateway.getUserId(actor);
        SysLayoutModuleGroup parent = moduleGroupRepository.findById(requiredParent(request))
                .orElseThrow(() -> new IllegalArgumentException("Layout module group not found: " + request.getParentId()));
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
        return node(saved.getNavigationModuleCode(), null, saved.getNavigationModuleName(), "module", saved.getIcon(), saved.getDescription(), null);
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager")
    public NavNodeDto saveCategory(LayoutNavigationNodeRequestDto request, String actor) {
        Long userId = authModuleGateway.getUserId(actor);
        SysLayoutNavigationModule parent = navigationModuleRepository.findById(requiredParent(request))
                .orElseThrow(() -> new IllegalArgumentException("Layout navigation module not found: " + request.getParentId()));
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
        return node(saved.getCategoryCode(), null, saved.getCategoryName(), "category", saved.getIcon(), null, null);
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager", readOnly = true)
    public List<NavNodeDto> listCategories() {
        return categoryRepository.findAll().stream()
                .map(category -> node(category.getCategoryCode(), null, category.getCategoryName(), "category", category.getIcon(), null, null))
                .toList();
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager")
    public void reorderCategories(LayoutNavigationCategoryOrderRequestDto request, String actor) {
        Long userId = authModuleGateway.getUserId(actor);
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
        return node(saved.getFeatureGroupCode(), null, saved.getFeatureGroupName(), "featureGroup", null, null, null);
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager")
    public NavNodeDto saveFeature(LayoutNavigationNodeRequestDto request, String actor) {
        Long userId = authModuleGateway.getUserId(actor);
        SysLayoutFeatureGroup parent = featureGroupRepository.findById(requiredParent(request))
                .orElseThrow(() -> new IllegalArgumentException("Layout feature group not found: " + request.getParentId()));
        String tCode = codeGenerationService.normalizeTCode(request.getTCode());
        codeGenerationService.validateTCode(tCode);
        SysLayoutFeature feature = request.getId() == null
                ? new SysLayoutFeature()
                : featureRepository.findById(request.getId())
                .orElseThrow(() -> new IllegalArgumentException("Layout feature not found: " + request.getId()));
        feature.setFeatureGroup(parent);
        feature.setFeatureCode(codeGenerationService.normalizeBusinessCode(request.getCode(), request.getName()));
        feature.setTCode(tCode);
        feature.setFeatureName(required(request.getName(), "name"));
        feature.setRoute(required(request.getRoute(), "route"));
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
        return featureNode(saved, Set.copyOf(request.getPrivilegeCodes()));
    }

    private NavNodeDto featureNode(SysLayoutFeature feature, Set<String> userPrivilegeCodes) {
        List<SysLayoutFeaturePrivilege> links = featurePrivilegeRepository.findByLayoutFeatureIdAndActiveTrue(feature.getId());
        List<String> requiredCodes = links.stream()
                .map(link -> link.getPrivilege().getPrivilegeCode())
                .toList();

        if (!isVisible(links, requiredCodes, userPrivilegeCodes)) {
            return null;
        }

        return NavNodeDto.builder()
                .code(feature.getFeatureCode())
                .tCode(feature.getTCode())
                .label(feature.getFeatureName())
                .type("feature")
                .icon(feature.getIcon())
                .route(feature.getRoute())
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

    private NavNodeDto node(String code, String tCode, String label, String type, String icon, String description, String route) {
        return NavNodeDto.builder()
                .code(code)
                .tCode(tCode)
                .label(label)
                .type(type)
                .icon(icon)
                .moduleGroupIconName("group".equals(type) ? icon : null)
                .moduleIconName("module".equals(type) ? icon : null)
                .description(description)
                .route(route)
                .children(new ArrayList<>())
                .privilegeCodes(new ArrayList<>())
                .build();
    }

    private void syncFeaturePrivileges(SysLayoutFeature feature, List<String> privilegeCodes, Long userId) {
        if (privilegeCodes == null || privilegeCodes.isEmpty()) {
            return;
        }
        for (String privilegeCode : privilegeCodes) {
            SysPrivPrivilege privilege = privilegeRepository.findByPrivilegeCode(privilegeCode)
                    .orElseThrow(() -> new IllegalArgumentException("Privilege not found: " + privilegeCode));
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
}
