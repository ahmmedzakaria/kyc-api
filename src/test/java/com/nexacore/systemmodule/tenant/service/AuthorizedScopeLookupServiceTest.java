package com.nexacore.systemmodule.tenant.service;

import com.nexacore.systemmodule.accesscontrol.security.DataScopeAccessDeniedException;
import com.nexacore.systemmodule.accesscontrol.security.DataScopeService;
import com.nexacore.systemmodule.accesscontrol.security.UserScopeAssignment;
import com.nexacore.systemmodule.tenant.entity.SysTenantBranch;
import com.nexacore.systemmodule.tenant.entity.SysTenantBusiness;
import com.nexacore.systemmodule.tenant.repository.TenantBranchRepository;
import com.nexacore.systemmodule.tenant.repository.TenantBusinessRepository;
import com.nexacore.systemmodule.tenant.repository.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class AuthorizedScopeLookupServiceTest {
    private final TenantRepository tenants=mock(TenantRepository.class);
    private final TenantBusinessRepository businesses=mock(TenantBusinessRepository.class);
    private final TenantBranchRepository branches=mock(TenantBranchRepository.class);
    private final DataScopeService dataScopes=mock(DataScopeService.class);
    private AuthorizedScopeLookupService service;

    @BeforeEach void setUp() {
        reset(tenants,businesses,branches,dataScopes);
        service=new AuthorizedScopeLookupService(tenants,businesses,branches,dataScopes);
    }

    @Test void tenantWideAuthorityReturnsAllActiveBusinesses() {
        when(dataScopes.currentAssignments()).thenReturn(Set.of(new UserScopeAssignment(10L,null,null)));
        SysTenantBusiness first=business(100L,10L,"A");
        SysTenantBusiness second=business(101L,10L,"B");
        when(businesses.findByTenantIdAndActiveTrueOrderByDisplayNameAsc(10L)).thenReturn(List.of(first,second));

        assertThat(service.businesses(10L)).extracting(scope -> scope.id()).containsExactly(100L,101L);
        verify(businesses,never()).findByTenantIdAndIdInAndActiveTrueOrderByDisplayNameAsc(anyLong(),anyCollection());
    }

    @Test void branchAuthorityExcludesSiblingBranches() {
        when(dataScopes.currentAssignments()).thenReturn(Set.of(new UserScopeAssignment(10L,100L,1000L)));
        when(businesses.findByIdAndTenantIdAndActiveTrue(100L,10L)).thenReturn(Optional.of(business(100L,10L,"A")));
        when(branches.findByTenantIdAndBusinessIdAndIdInAndActiveTrueOrderByDisplayNameAsc(10L,100L,Set.of(1000L)))
                .thenReturn(List.of(branch(1000L,10L,100L,"ONE")));

        assertThat(service.branches(10L,100L)).extracting(scope -> scope.id()).containsExactly(1000L);
        verify(branches,never()).findByTenantIdAndBusinessIdAndActiveTrueOrderByDisplayNameAsc(anyLong(),anyLong());
    }

    @Test void siblingBusinessFailsClosed() {
        when(dataScopes.currentAssignments()).thenReturn(Set.of(new UserScopeAssignment(10L,100L,null)));
        assertThatThrownBy(() -> service.branches(10L,101L)).isInstanceOf(DataScopeAccessDeniedException.class);
        verifyNoInteractions(branches);
    }

    @Test void invalidBranchParentIsRejected() {
        when(businesses.findByIdAndTenantIdAndActiveTrue(100L,10L)).thenReturn(Optional.of(business(100L,10L,"A")));
        when(branches.existsByIdAndTenantIdAndBusinessIdAndActiveTrue(1000L,10L,100L)).thenReturn(false);

        assertThatThrownBy(() -> service.validateAssignment(10L,100L,1000L))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Branch does not belong");
        verify(dataScopes).requireWritableScope(10L,100L,1000L);
    }

    private SysTenantBusiness business(Long id,Long tenantId,String code) {
        SysTenantBusiness value=new SysTenantBusiness(); value.setId(id); value.setTenantId(tenantId);
        value.setBusinessCode(code); value.setDisplayName(code); return value;
    }
    private SysTenantBranch branch(Long id,Long tenantId,Long businessId,String code) {
        SysTenantBranch value=new SysTenantBranch(); value.setId(id); value.setTenantId(tenantId); value.setBusinessId(businessId);
        value.setBranchCode(code); value.setDisplayName(code); return value;
    }
}
