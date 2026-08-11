package com.nexacore.systemmodule.accesscontrol.security;

import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class DataScopeService {

    public void requireCurrentClient(Long clientApplicationId) {
        AuthenticatedRequestContext context = AuthenticatedRequestContextHolder.get().orElse(null);
        if (clientApplicationId == null || context == null
                || !clientApplicationId.equals(context.clientApplicationId())) {
            throw new DataScopeAccessDeniedException("The requested client scope does not match the authenticated client");
        }
    }

    public Set<UserScopeAssignment> currentAssignments() {
        return EffectiveTenantAccessContextHolder.get()
                .map(EffectiveTenantAccessContext::effectiveScopes)
                .orElseGet(() -> AuthenticatedRequestContextHolder.get()
                        .map(AuthenticatedRequestContext::scopeAssignments)
                        .orElse(Set.of()));
    }

    public UserScopeAssignment requireWritableScope(Long tenantId, Long businessId, Long branchId) {
        Set<UserScopeAssignment> assignments = currentAssignments();
        if (assignments.isEmpty()) {
            throw new DataScopeAccessDeniedException("No organizational scope is assigned to the authenticated user");
        }

        if (tenantId == null) {
            if (assignments.size() != 1) {
                throw new DataScopeAccessDeniedException("An explicit organizational scope is required when the user has multiple assignments");
            }
            return assignments.iterator().next();
        }

        Long resolvedTenantId = tenantId;
        boolean allowed = assignments.stream().anyMatch(scope -> scope.tenantId().equals(resolvedTenantId)
                && (scope.businessId() == null || scope.businessId().equals(businessId))
                && (scope.branchId() == null || scope.branchId().equals(branchId)));
        if (!allowed) {
            throw new DataScopeAccessDeniedException("The requested organizational scope is not assigned to the authenticated user");
        }
        return new UserScopeAssignment(tenantId, businessId, branchId);
    }

    public Long requireEffectiveTenant(Long requestedTenantId) {
        Set<Long> assignedTenantIds = currentAssignments().stream()
                .map(UserScopeAssignment::tenantId)
                .collect(Collectors.toUnmodifiableSet());
        if (assignedTenantIds.isEmpty()) {
            throw new DataScopeAccessDeniedException("No tenant scope is assigned to the authenticated user");
        }
        if (requestedTenantId != null) {
            if (!assignedTenantIds.contains(requestedTenantId)) {
                throw new DataScopeAccessDeniedException("The requested tenant is not assigned to the authenticated user");
            }
            return requestedTenantId;
        }
        if (assignedTenantIds.size() != 1) {
            throw new DataScopeAccessDeniedException("An explicit tenant selection is required when the user has multiple tenants");
        }
        return assignedTenantIds.iterator().next();
    }
    public <T> Specification<T> restrictToCurrentScopes(String tenantAttribute,
                                                         String businessAttribute,
                                                         String branchAttribute) {
        Set<UserScopeAssignment> assignments = currentAssignments();
        return (root, query, cb) -> {
            if (assignments.isEmpty()) return cb.disjunction();
            Path<Long> tenant = root.get(tenantAttribute);
            Path<Long> business = root.get(businessAttribute);
            Path<Long> branch = root.get(branchAttribute);
            Predicate[] allowed = assignments.stream()
                    .sorted(Comparator.comparing(UserScopeAssignment::tenantId)
                            .thenComparing(scope -> scope.businessId() == null ? Long.MIN_VALUE : scope.businessId())
                            .thenComparing(scope -> scope.branchId() == null ? Long.MIN_VALUE : scope.branchId()))
                    .map(scope -> {
                        Predicate predicate = cb.equal(tenant, scope.tenantId());
                        if (scope.businessId() != null) predicate = cb.and(predicate, cb.equal(business, scope.businessId()));
                        if (scope.branchId() != null) predicate = cb.and(predicate, cb.equal(branch, scope.branchId()));
                        return predicate;
                    })
                    .toArray(Predicate[]::new);
            return cb.or(allowed);
        };
    }
}
