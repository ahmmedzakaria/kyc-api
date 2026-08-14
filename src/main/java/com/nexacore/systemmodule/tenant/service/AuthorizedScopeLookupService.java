package com.nexacore.systemmodule.tenant.service;

import com.nexacore.systemmodule.accesscontrol.security.DataScopeAccessDeniedException;
import com.nexacore.systemmodule.accesscontrol.security.DataScopeService;
import com.nexacore.systemmodule.accesscontrol.security.UserScopeAssignment;
import com.nexacore.systemmodule.tenant.dto.ScopeLookupDto;
import com.nexacore.systemmodule.tenant.entity.TenantStatus;
import com.nexacore.systemmodule.tenant.repository.TenantBranchRepository;
import com.nexacore.systemmodule.tenant.repository.TenantBusinessRepository;
import com.nexacore.systemmodule.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor
public class AuthorizedScopeLookupService {
    private final TenantRepository tenants;
    private final TenantBusinessRepository businesses;
    private final TenantBranchRepository branches;
    private final DataScopeService dataScopes;

    @Transactional(transactionManager="systemTransactionManager",readOnly=true)
    public List<ScopeLookupDto> tenants() {
        if (isPlatformAdministrator()) {
            return tenants.findByStatusOrderByTenantCodeAsc(TenantStatus.ACTIVE).stream()
                    .map(t->new ScopeLookupDto(t.getId(),t.getTenantCode(),t.getDisplayName(),t.getId(),null)).toList();
        }
        Set<Long> ids=dataScopes.currentAssignments().stream().map(UserScopeAssignment::tenantId).collect(Collectors.toSet());
        if(ids.isEmpty()) return List.of();
        return tenants.findByIdInAndStatusOrderByTenantCodeAsc(ids,TenantStatus.ACTIVE).stream()
                .map(t->new ScopeLookupDto(t.getId(),t.getTenantCode(),t.getDisplayName(),t.getId(),null)).toList();
    }

    @Transactional(transactionManager="systemTransactionManager",readOnly=true)
    public List<ScopeLookupDto> businesses(Long tenantId) {
        if (isPlatformAdministrator()) {
            requireActiveTenant(tenantId);
            return businesses.findByTenantIdAndActiveTrueOrderByDisplayNameAsc(tenantId).stream()
                    .map(b->new ScopeLookupDto(b.getId(),b.getBusinessCode(),b.getDisplayName(),tenantId,b.getId())).toList();
        }
        Set<UserScopeAssignment> scopes=tenantScopes(tenantId);
        boolean tenantWide=scopes.stream().anyMatch(s->s.businessId()==null);
        var rows=tenantWide ? businesses.findByTenantIdAndActiveTrueOrderByDisplayNameAsc(tenantId)
                : businesses.findByTenantIdAndIdInAndActiveTrueOrderByDisplayNameAsc(tenantId,
                    scopes.stream().map(UserScopeAssignment::businessId).collect(Collectors.toSet()));
        return rows.stream().map(b->new ScopeLookupDto(b.getId(),b.getBusinessCode(),b.getDisplayName(),tenantId,b.getId())).toList();
    }

    @Transactional(transactionManager="systemTransactionManager",readOnly=true)
    public List<ScopeLookupDto> branches(Long tenantId,Long businessId) {
        if(businessId==null) throw new IllegalArgumentException("businessId is required");
        if (isPlatformAdministrator()) {
            requireActiveTenant(tenantId);
            if(businesses.findByIdAndTenantIdAndActiveTrue(businessId,tenantId).isEmpty())
                throw new IllegalArgumentException("Business does not belong to the tenant");
            return branches.findByTenantIdAndBusinessIdAndActiveTrueOrderByDisplayNameAsc(tenantId,businessId).stream()
                    .map(b->new ScopeLookupDto(b.getId(),b.getBranchCode(),b.getDisplayName(),tenantId,businessId)).toList();
        }
        Set<UserScopeAssignment> scopes=tenantScopes(tenantId);
        boolean businessWide=scopes.stream().anyMatch(s->s.businessId()==null || (businessId.equals(s.businessId()) && s.branchId()==null));
        Set<Long> branchIds=scopes.stream().filter(s->businessId.equals(s.businessId()) && s.branchId()!=null)
                .map(UserScopeAssignment::branchId).collect(Collectors.toSet());
        if(!businessWide && branchIds.isEmpty()) throw new DataScopeAccessDeniedException("Business is outside the effective scope");
        if(businesses.findByIdAndTenantIdAndActiveTrue(businessId,tenantId).isEmpty())
            throw new IllegalArgumentException("Business does not belong to the tenant");
        var rows=businessWide ? branches.findByTenantIdAndBusinessIdAndActiveTrueOrderByDisplayNameAsc(tenantId,businessId)
                : branches.findByTenantIdAndBusinessIdAndIdInAndActiveTrueOrderByDisplayNameAsc(tenantId,businessId,branchIds);
        return rows.stream().map(b->new ScopeLookupDto(b.getId(),b.getBranchCode(),b.getDisplayName(),tenantId,businessId)).toList();
    }

    public void validateAssignment(Long tenantId,Long businessId,Long branchId) {
        if (isPlatformAdministrator()) requireActiveTenant(tenantId);
        else dataScopes.requireWritableScope(tenantId,businessId,branchId);
        if(businessId==null) { if(branchId!=null) throw new IllegalArgumentException("branchId requires businessId"); return; }
        if(businesses.findByIdAndTenantIdAndActiveTrue(businessId,tenantId).isEmpty())
            throw new IllegalArgumentException("Business does not belong to the tenant");
        if(branchId!=null && !branches.existsByIdAndTenantIdAndBusinessIdAndActiveTrue(branchId,tenantId,businessId))
            throw new IllegalArgumentException("Branch does not belong to the business and tenant");
    }

    public boolean canManageAssignment(Long tenantId, Long businessId, Long branchId) {
        if (isPlatformAdministrator()) {
            try {
                validateAssignment(tenantId,businessId,branchId);
                return true;
            } catch (DataScopeAccessDeniedException | IllegalArgumentException exception) {
                return false;
            }
        }
        try {
            dataScopes.requireWritableScope(tenantId, businessId, branchId);
            return true;
        } catch (DataScopeAccessDeniedException exception) {
            return false;
        }
    }

    private Set<UserScopeAssignment> tenantScopes(Long tenantId) {
        if(tenantId==null) throw new IllegalArgumentException("tenantId is required");
        Set<UserScopeAssignment> scopes=dataScopes.currentAssignments().stream().filter(s->tenantId.equals(s.tenantId())).collect(Collectors.toSet());
        if(scopes.isEmpty()) throw new DataScopeAccessDeniedException("Tenant is outside the effective scope");
        return scopes;
    }

    private void requireActiveTenant(Long tenantId) {
        if (tenantId == null) throw new IllegalArgumentException("tenantId is required");
        if (tenants.findById(tenantId).filter(tenant -> tenant.getStatus() == TenantStatus.ACTIVE).isEmpty())
            throw new DataScopeAccessDeniedException("Tenant is not active or does not exist");
    }

    private boolean isPlatformAdministrator() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated()
                && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_SYSTEM_ADMIN".equals(authority.getAuthority()));
    }
}
