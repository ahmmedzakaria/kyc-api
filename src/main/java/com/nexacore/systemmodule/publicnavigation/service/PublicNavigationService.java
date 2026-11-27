package com.nexacore.systemmodule.publicnavigation.service;

import com.nexacore.gatewaymodule.auth.service.interfaces.AuthModuleGateway;
import com.nexacore.gatewaymodule.navigation.dto.PublicNavigationContextDto;
import com.nexacore.gatewaymodule.navigation.enums.PublicNavigationSurface;
import com.nexacore.gatewaymodule.navigation.service.interfaces.PublicNavigationModuleGateway;
import com.nexacore.systemmodule.accesscontrol.entity.SysAccClientApplication;
import com.nexacore.systemmodule.accesscontrol.enums.ClientApplicationStatus;
import com.nexacore.systemmodule.accesscontrol.repository.ClientApplicationRepository;
import com.nexacore.systemmodule.accesscontrol.repository.ClientApplicationTenantRepository;
import com.nexacore.systemmodule.accesscontrol.security.DataScopeService;
import com.nexacore.systemmodule.publicnavigation.dto.*;
import com.nexacore.systemmodule.publicnavigation.entity.ClientPublicNavigationItem;
import com.nexacore.systemmodule.publicnavigation.entity.PublicNavigationGroup;
import com.nexacore.systemmodule.publicnavigation.entity.PublicNavigationItem;
import com.nexacore.systemmodule.publicnavigation.enums.PublicNavigationItemType;
import com.nexacore.systemmodule.publicnavigation.enums.PublicNavigationTarget;
import com.nexacore.systemmodule.publicnavigation.repository.ClientPublicNavigationItemRepository;
import com.nexacore.systemmodule.publicnavigation.repository.PublicNavigationGroupRepository;
import com.nexacore.systemmodule.publicnavigation.repository.PublicNavigationItemRepository;
import com.nexacore.systemmodule.tenant.security.ResolvedTenantContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PublicNavigationService implements PublicNavigationModuleGateway, PublicNavigationAdministrationService {
    private static final int MAX_DEPTH = 12;

    private final PublicNavigationGroupRepository groupRepository;
    private final PublicNavigationItemRepository itemRepository;
    private final ClientPublicNavigationItemRepository assignmentRepository;
    private final ClientApplicationRepository clientRepository;
    private final ClientApplicationTenantRepository clientTenantRepository;
    private final DataScopeService dataScopeService;
    private final AuthModuleGateway authModuleGateway;
    private final Map<CacheKey, PublicNavigationContextDto> effectiveTreeCache = new ConcurrentHashMap<>();

    @Override
    public PublicNavigationContextDto getPublicNavigation(String clientCode, Set<PublicNavigationSurface> surfaces) {
        long tenantId = ResolvedTenantContextHolder.get()
                .orElseThrow(() -> notFound("Public navigation tenant context is unavailable")).tenantId();
        return cachedResolve(tenantId, clientCode, normalizedSurfaces(surfaces));
    }

    @Override
    public List<PublicNavigationGroupDto> listGroups(PublicNavigationScopeRequest request) {
        long tenantId = scopedTenant(request.tenantId());
        return groupRepository.findByTenantIdOrderBySurfaceAscDisplayOrderAscIdAsc(tenantId).stream()
                .filter(group -> request.surfaces() == null || request.surfaces().isEmpty()
                        || request.surfaces().contains(group.getSurface()))
                .map(this::groupDto).toList();
    }

    @Override
    @Transactional
    public PublicNavigationGroupDto saveGroup(PublicNavigationGroupRequest request, String username) {
        long tenantId = scopedTenant(request.tenantId());
        PublicNavigationSurface surface = parseSurface(request.surface());
        String code = businessCode(request.groupCode());
        long actorId = actorId(username);
        PublicNavigationGroup group = request.id() == null ? new PublicNavigationGroup()
                : groupRepository.findByIdAndTenantId(request.id(), tenantId)
                .orElseThrow(() -> notFound("Public navigation group was not found"));
        if (request.id() != null && !Objects.equals(group.getVersion(), request.version())) {
            throw conflict("Public navigation group was changed by another user");
        }
        long excludedId = request.id() == null ? -1L : request.id();
        if (groupRepository.existsByTenantIdAndGroupCodeIgnoreCaseAndIdNot(tenantId, code, excludedId)) {
            throw conflict("Public navigation group code already exists for this tenant");
        }
        group.setTenantId(tenantId);
        group.setGroupCode(code);
        group.setGroupName(required(request.groupName(), "Group name"));
        group.setSurface(surface.name());
        group.setDisplayOrder(request.displayOrder() == null ? 100 : request.displayOrder());
        group.setActive(request.active() == null || request.active());
        if (group.getId() == null) group.setCreatedBy(actorId);
        group.setUpdatedBy(actorId);
        PublicNavigationGroupDto saved = groupDto(groupRepository.saveAndFlush(group));
        evictTenant(tenantId);
        return saved;
    }

    @Override
    public List<PublicNavigationItemDto> listItems(PublicNavigationScopeRequest request) {
        long tenantId = scopedTenant(request.tenantId());
        return itemRepository.findByTenantIdOrderByNavigationGroupIdAscDisplayOrderAscIdAsc(tenantId).stream()
                .map(this::itemDto).toList();
    }

    @Override
    @Transactional
    public PublicNavigationItemDto saveItem(PublicNavigationItemRequest request, String username) {
        long tenantId = scopedTenant(request.tenantId());
        PublicNavigationGroup group = groupRepository.findByIdAndTenantId(request.navigationGroupId(), tenantId)
                .orElseThrow(() -> notFound("Public navigation group was not found"));
        PublicNavigationItemType type = parseType(request.itemType());
        PublicNavigationTarget target = parseTarget(request.target());
        PublicNavigationItem item = request.id() == null ? new PublicNavigationItem()
                : itemRepository.findByIdAndTenantId(request.id(), tenantId)
                .orElseThrow(() -> notFound("Public navigation item was not found"));
        if (request.id() != null && !Objects.equals(item.getVersion(), request.version())) {
            throw conflict("Public navigation item was changed by another user");
        }
        long excludedId = request.id() == null ? -1L : request.id();
        String code = businessCode(request.itemCode());
        if (itemRepository.existsByNavigationGroupIdAndItemCodeIgnoreCaseAndIdNot(group.getId(), code, excludedId)) {
            throw conflict("Public navigation item code already exists in this group");
        }
        PublicNavigationItem parent = validateParent(tenantId, group.getId(), request.parentId(), request.id());
        validateDestination(type, request.routePath(), request.externalUrl());
        if ((request.active() == null || request.active()) && parent != null && !parent.isActive()) {
            throw badRequest("An active item cannot have an inactive parent");
        }
        long actorId = actorId(username);
        item.setTenantId(tenantId);
        item.setNavigationGroupId(group.getId());
        item.setParentId(parent == null ? null : parent.getId());
        item.setItemCode(code);
        item.setItemType(type.name());
        item.setLabel(required(request.label(), "Label"));
        item.setDescription(trimToNull(request.description()));
        item.setAriaLabel(trimToNull(request.ariaLabel()));
        item.setRoutePath(type == PublicNavigationItemType.INTERNAL_ROUTE ? normalizeRoute(request.routePath()) : null);
        item.setExternalUrl(type == PublicNavigationItemType.EXTERNAL_URL ? request.externalUrl().trim() : null);
        item.setTarget(target.name());
        item.setIcon(trimToNull(request.icon()));
        item.setDisplayOrder(request.displayOrder() == null ? 100 : request.displayOrder());
        item.setActive(request.active() == null || request.active());
        if (item.getId() == null) item.setCreatedBy(actorId);
        item.setUpdatedBy(actorId);
        PublicNavigationItemDto saved = itemDto(itemRepository.saveAndFlush(item));
        evictTenant(tenantId);
        return saved;
    }

    @Override
    @Transactional
    public PublicNavigationItemDto deactivateItem(Long tenantId, Long itemId, Long version, String username) {
        long scopedTenant = scopedTenant(tenantId);
        PublicNavigationItem item = itemRepository.findByIdAndTenantId(itemId, scopedTenant)
                .orElseThrow(() -> notFound("Public navigation item was not found"));
        if (!Objects.equals(item.getVersion(), version)) throw conflict("Public navigation item was changed by another user");
        item.setActive(false);
        item.setUpdatedBy(actorId(username));
        PublicNavigationItemDto saved = itemDto(itemRepository.saveAndFlush(item));
        evictTenant(scopedTenant);
        return saved;
    }

    @Override
    @Transactional
    public void deleteItem(Long tenantId, Long itemId) {
        long scopedTenant = scopedTenant(tenantId);
        PublicNavigationItem item = itemRepository.findByIdAndTenantId(itemId, scopedTenant)
                .orElseThrow(() -> notFound("Public navigation item was not found"));
        if (itemRepository.existsByTenantIdAndParentId(scopedTenant, itemId)
                || assignmentRepository.existsByTenantIdAndPublicNavigationItemId(scopedTenant, itemId)) {
            throw conflict("Public navigation item has children or client assignments and cannot be deleted");
        }
        itemRepository.delete(item);
        evictTenant(scopedTenant);
    }

    @Override
    public PublicNavigationAssignmentDto listAssignments(PublicNavigationScopeRequest request) {
        long tenantId = scopedTenant(request.tenantId());
        SysAccClientApplication client = activeClient(request.clientCode());
        requireClientTenant(client.getId(), tenantId);
        return new PublicNavigationAssignmentDto(tenantId, client.getClientCode(), assignmentRepository
                .findByTenantIdAndClientApplicationIdAndActiveTrue(tenantId, client.getId()).stream()
                .map(ClientPublicNavigationItem::getPublicNavigationItemId)
                .collect(Collectors.toCollection(LinkedHashSet::new)));
    }

    @Override
    @Transactional
    public PublicNavigationAssignmentDto replaceAssignments(PublicNavigationAssignmentRequest request, String username) {
        long tenantId = scopedTenant(request.tenantId());
        SysAccClientApplication client = activeClient(request.clientCode());
        requireClientTenant(client.getId(), tenantId);
        Set<Long> requested = new LinkedHashSet<>(request.itemIds());
        Map<Long, PublicNavigationItem> items = itemRepository.findByTenantIdOrderByNavigationGroupIdAscDisplayOrderAscIdAsc(tenantId)
                .stream().collect(Collectors.toMap(PublicNavigationItem::getId, Function.identity()));
        if (!items.keySet().containsAll(requested)) throw badRequest("One or more public navigation items do not belong to the tenant");
        if (requested.stream().map(items::get).anyMatch(item -> !item.isActive()
                || PublicNavigationItemType.GROUP.name().equals(item.getItemType()))) {
            throw badRequest("Only active link items may be assigned to a client");
        }
        long actorId = actorId(username);
        Map<Long, ClientPublicNavigationItem> existing = assignmentRepository
                .findByTenantIdAndClientApplicationId(tenantId, client.getId()).stream()
                .collect(Collectors.toMap(ClientPublicNavigationItem::getPublicNavigationItemId, Function.identity()));
        existing.values().forEach(link -> {
            link.setActive(requested.contains(link.getPublicNavigationItemId()));
            link.setUpdatedBy(actorId);
        });
        requested.stream().filter(id -> !existing.containsKey(id)).forEach(id -> {
            ClientPublicNavigationItem link = new ClientPublicNavigationItem();
            link.setTenantId(tenantId);
            link.setClientApplicationId(client.getId());
            link.setPublicNavigationItemId(id);
            link.setActive(true);
            link.setCreatedBy(actorId);
            link.setUpdatedBy(actorId);
            existing.put(id, link);
        });
        assignmentRepository.saveAll(existing.values());
        evictTenant(tenantId);
        return new PublicNavigationAssignmentDto(tenantId, client.getClientCode(), Set.copyOf(requested));
    }

    @Override
    public PublicNavigationIntegrityDto integrity(Long requestedTenantId) {
        long tenantId = scopedTenant(requestedTenantId);
        List<PublicNavigationItem> items = itemRepository.findByTenantIdOrderByNavigationGroupIdAscDisplayOrderAscIdAsc(tenantId);
        Map<Long, PublicNavigationItem> byId = items.stream().collect(Collectors.toMap(PublicNavigationItem::getId, Function.identity()));
        List<String> issues = new ArrayList<>();
        for (PublicNavigationItem item : items) {
            if (item.getParentId() != null) {
                PublicNavigationItem parent = byId.get(item.getParentId());
                if (parent == null) issues.add("Item " + item.getItemCode() + " has an orphaned parent");
                else if (!Objects.equals(parent.getNavigationGroupId(), item.getNavigationGroupId()))
                    issues.add("Item " + item.getItemCode() + " has a parent in another group");
                else if (item.isActive() && !parent.isActive()) issues.add("Active item " + item.getItemCode() + " has an inactive parent");
            }
            try { validateDestination(parseType(item.getItemType()), item.getRoutePath(), item.getExternalUrl()); }
            catch (ResponseStatusException ex) { issues.add("Item " + item.getItemCode() + " has an invalid destination"); }
            if (hasCycle(item, byId)) issues.add("Item " + item.getItemCode() + " participates in a cycle");
        }
        return new PublicNavigationIntegrityDto(issues.isEmpty(), List.copyOf(issues));
    }

    @Override
    public PublicNavigationContextDto preview(PublicNavigationScopeRequest request) {
        long tenantId = scopedTenant(request.tenantId());
        Set<PublicNavigationSurface> surfaces = request.surfaces() == null ? Set.of() : request.surfaces().stream()
                .map(this::parseSurface).collect(Collectors.toCollection(() -> EnumSet.noneOf(PublicNavigationSurface.class)));
        return cachedResolve(tenantId, request.clientCode(), normalizedSurfaces(surfaces));
    }

    private PublicNavigationContextDto cachedResolve(long tenantId, String clientCode, Set<PublicNavigationSurface> surfaces) {
        String normalizedClient = businessCode(clientCode);
        Set<PublicNavigationSurface> stableSurfaces = Set.copyOf(surfaces);
        return effectiveTreeCache.computeIfAbsent(new CacheKey(tenantId, normalizedClient, stableSurfaces),
                ignored -> resolve(tenantId, normalizedClient, stableSurfaces));
    }

    private void evictTenant(long tenantId) {
        effectiveTreeCache.keySet().removeIf(key -> key.tenantId() == tenantId);
    }

    private PublicNavigationContextDto resolve(long tenantId, String clientCode, Set<PublicNavigationSurface> surfaces) {
        SysAccClientApplication client = activeClient(clientCode);
        requireClientTenant(client.getId(), tenantId);
        Set<String> surfaceNames = surfaces.stream().map(Enum::name).collect(Collectors.toSet());
        List<PublicNavigationGroup> groups = groupRepository
                .findByTenantIdAndSurfaceInAndActiveTrueOrderByDisplayOrderAscIdAsc(tenantId, surfaceNames);
        if (groups.isEmpty()) return emptyContext(client.getClientCode(), surfaces);
        List<PublicNavigationItem> items = itemRepository
                .findByTenantIdAndNavigationGroupIdInAndActiveTrueOrderByDisplayOrderAscIdAsc(
                        tenantId, groups.stream().map(PublicNavigationGroup::getId).toList());
        Set<Long> assigned = assignmentRepository.findByTenantIdAndClientApplicationIdAndActiveTrue(tenantId, client.getId())
                .stream().map(ClientPublicNavigationItem::getPublicNavigationItemId).collect(Collectors.toSet());
        Map<Long, List<PublicNavigationItem>> children = items.stream()
                .collect(Collectors.groupingBy(item -> item.getParentId() == null ? 0L : item.getParentId(), LinkedHashMap::new, Collectors.toList()));
        Map<Long, List<PublicNavigationItem>> rootsByGroup = items.stream().filter(item -> item.getParentId() == null)
                .collect(Collectors.groupingBy(PublicNavigationItem::getNavigationGroupId, LinkedHashMap::new, Collectors.toList()));
        List<PublicNavigationContextDto.Region> regions = new ArrayList<>();
        for (PublicNavigationSurface surface : surfaces) {
            List<PublicNavigationContextDto.Group> groupDtos = groups.stream().filter(group -> surface.name().equals(group.getSurface()))
                    .map(group -> {
                        List<PublicNavigationContextDto.Item> roots = rootsByGroup.getOrDefault(group.getId(), List.of()).stream()
                                .map(item -> treeItem(item, children, assigned, new HashSet<>())).filter(Objects::nonNull).toList();
                        return roots.isEmpty() ? null : new PublicNavigationContextDto.Group(group.getGroupCode(), group.getGroupName(), roots);
                    }).filter(Objects::nonNull).toList();
            regions.add(new PublicNavigationContextDto.Region(surface.name(), groupDtos));
        }
        return new PublicNavigationContextDto(client.getClientCode(), List.copyOf(regions));
    }

    private PublicNavigationContextDto.Item treeItem(PublicNavigationItem item, Map<Long, List<PublicNavigationItem>> children,
                                                     Set<Long> assigned, Set<Long> path) {
        if (!path.add(item.getId())) throw conflict("Public navigation hierarchy contains a cycle");
        List<PublicNavigationContextDto.Item> childDtos = children.getOrDefault(item.getId(), List.of()).stream()
                .map(child -> treeItem(child, children, assigned, new HashSet<>(path))).filter(Objects::nonNull).toList();
        boolean group = PublicNavigationItemType.GROUP.name().equals(item.getItemType());
        if ((group && childDtos.isEmpty()) || (!group && !assigned.contains(item.getId()))) return null;
        return new PublicNavigationContextDto.Item(item.getItemCode(), item.getItemType(), item.getLabel(),
                item.getDescription(), item.getAriaLabel(), item.getRoutePath(), item.getExternalUrl(),
                item.getTarget(), item.getIcon(), childDtos);
    }

    private PublicNavigationItem validateParent(long tenantId, long groupId, Long parentId, Long itemId) {
        if (parentId == null) return null;
        if (Objects.equals(parentId, itemId)) throw badRequest("An item cannot be its own parent");
        PublicNavigationItem parent = itemRepository.findByIdAndTenantId(parentId, tenantId)
                .orElseThrow(() -> badRequest("Parent item was not found in the tenant"));
        if (!Objects.equals(parent.getNavigationGroupId(), groupId)) throw badRequest("Parent must belong to the same navigation group");
        if (!PublicNavigationItemType.GROUP.name().equals(parent.getItemType())) throw badRequest("Parent item must be a group");
        int depth = 1;
        Set<Long> seen = new HashSet<>();
        while (parent != null && parent.getParentId() != null) {
            if (!seen.add(parent.getId()) || Objects.equals(parent.getParentId(), itemId)) throw badRequest("Parent selection creates a cycle");
            if (++depth > MAX_DEPTH) throw badRequest("Public navigation hierarchy exceeds the maximum depth");
            parent = itemRepository.findByIdAndTenantId(parent.getParentId(), tenantId).orElse(null);
        }
        return itemRepository.findByIdAndTenantId(parentId, tenantId).orElseThrow();
    }

    private void validateDestination(PublicNavigationItemType type, String route, String externalUrl) {
        if (type == PublicNavigationItemType.GROUP && (trimToNull(route) != null || trimToNull(externalUrl) != null))
            throw badRequest("Group items cannot have a destination");
        if (type == PublicNavigationItemType.INTERNAL_ROUTE) normalizeRoute(route);
        if (type == PublicNavigationItemType.EXTERNAL_URL) {
            String value = required(externalUrl, "External URL");
            try {
                URI uri = URI.create(value);
                if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) throw new IllegalArgumentException();
            } catch (IllegalArgumentException ex) { throw badRequest("External URL must be an absolute HTTPS URL"); }
        }
    }

    private boolean hasCycle(PublicNavigationItem start, Map<Long, PublicNavigationItem> byId) {
        Set<Long> seen = new HashSet<>();
        PublicNavigationItem current = start;
        while (current != null && current.getParentId() != null) {
            if (!seen.add(current.getId())) return true;
            current = byId.get(current.getParentId());
        }
        return false;
    }

    private long scopedTenant(Long tenantId) { return dataScopeService.requireEffectiveTenant(tenantId); }
    private long actorId(String username) { return authModuleGateway.getUserId(username); }
    private SysAccClientApplication activeClient(String code) {
        String normalized = businessCode(code);
        SysAccClientApplication client = clientRepository.findByClientCode(normalized)
                .orElseThrow(() -> notFound("Public navigation client was not found"));
        if (client.getStatus() != ClientApplicationStatus.ACTIVE) throw notFound("Public navigation client is inactive");
        return client;
    }
    private void requireClientTenant(Long clientId, long tenantId) {
        if (!clientTenantRepository.existsByClientApplicationIdAndTenantIdAndActiveTrue(clientId, tenantId))
            throw notFound("No active client mapping exists for the resolved tenant");
    }
    private Set<PublicNavigationSurface> normalizedSurfaces(Set<PublicNavigationSurface> surfaces) {
        return surfaces == null || surfaces.isEmpty() ? EnumSet.allOf(PublicNavigationSurface.class) : EnumSet.copyOf(surfaces);
    }
    private PublicNavigationContextDto emptyContext(String code, Set<PublicNavigationSurface> surfaces) {
        return new PublicNavigationContextDto(code, surfaces.stream()
                .map(surface -> new PublicNavigationContextDto.Region(surface.name(), List.of())).toList());
    }
    private PublicNavigationGroupDto groupDto(PublicNavigationGroup value) {
        return new PublicNavigationGroupDto(value.getId(), value.getTenantId(), value.getGroupCode(), value.getGroupName(),
                value.getSurface(), value.getDisplayOrder(), value.isActive(), value.getVersion());
    }
    private PublicNavigationItemDto itemDto(PublicNavigationItem value) {
        return new PublicNavigationItemDto(value.getId(), value.getTenantId(), value.getNavigationGroupId(), value.getParentId(),
                value.getItemCode(), value.getItemType(), value.getLabel(), value.getDescription(), value.getAriaLabel(),
                value.getRoutePath(), value.getExternalUrl(), value.getTarget(), value.getIcon(), value.getDisplayOrder(),
                value.isActive(), value.getVersion());
    }
    private PublicNavigationSurface parseSurface(String value) {
        try { return PublicNavigationSurface.valueOf(required(value, "Surface").toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException ex) { throw badRequest("Unsupported public navigation surface"); }
    }
    private PublicNavigationItemType parseType(String value) {
        try { return PublicNavigationItemType.valueOf(required(value, "Item type").toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException ex) { throw badRequest("Unsupported public navigation item type"); }
    }
    private PublicNavigationTarget parseTarget(String value) {
        try { return PublicNavigationTarget.valueOf(value == null || value.isBlank() ? "SELF" : value.trim().toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException ex) { throw badRequest("Unsupported public navigation target"); }
    }
    private String normalizeRoute(String value) {
        String route = required(value, "Route path");
        if (!route.startsWith("/") || route.startsWith("//") || route.contains("://") || route.contains("\\"))
            throw badRequest("Internal route must be an application-relative absolute path");
        return route;
    }
    private String businessCode(String value) { return required(value, "Business code").toUpperCase(Locale.ROOT); }
    private String required(String value, String field) {
        String result = trimToNull(value);
        if (result == null) throw badRequest(field + " is required");
        return result;
    }
    private String trimToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private ResponseStatusException badRequest(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    private ResponseStatusException notFound(String message) { return new ResponseStatusException(HttpStatus.NOT_FOUND, message); }
    private ResponseStatusException conflict(String message) { return new ResponseStatusException(HttpStatus.CONFLICT, message); }

    private record CacheKey(long tenantId, String clientCode, Set<PublicNavigationSurface> surfaces) {}
}
