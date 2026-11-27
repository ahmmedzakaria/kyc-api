package com.nexacore.systemmodule.layout.service.implementations;

import com.nexacore.systemmodule.accesscontrol.repository.ClientApplicationRepository;
import com.nexacore.systemmodule.layout.enums.LayoutType;
import com.nexacore.systemmodule.layout.repository.ClientLayoutProfileRepository;
import com.nexacore.systemmodule.layout.repository.LayoutProfileBrandingRepository;
import com.nexacore.systemmodule.layout.repository.LayoutProfileFontsRepository;
import com.nexacore.systemmodule.layout.repository.LayoutProfileRepository;
import com.nexacore.systemmodule.layout.repository.LayoutProfileSizesRepository;
import com.nexacore.systemmodule.layout.repository.LayoutProfileThemeRepository;
import com.nexacore.systemmodule.layout.repository.LayoutThemeChromeOverridesRepository;
import com.nexacore.systemmodule.layout.repository.LayoutThemePrimariesRepository;
import com.nexacore.systemmodule.layout.service.interfaces.LayoutNavigationService;
import com.nexacore.systemmodule.tenant.security.ResolvedTenantContext;
import com.nexacore.systemmodule.tenant.security.ResolvedTenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LayoutContextPublicFallbackTest {
    @Mock ClientApplicationRepository clientApplicationRepository;
    @Mock ClientLayoutProfileRepository clientLayoutProfileRepository;
    @Mock LayoutProfileRepository layoutProfileRepository;
    @Mock LayoutProfileBrandingRepository brandingRepository;
    @Mock LayoutProfileThemeRepository themeRepository;
    @Mock LayoutThemePrimariesRepository primariesRepository;
    @Mock LayoutThemeChromeOverridesRepository chromeOverridesRepository;
    @Mock LayoutProfileSizesRepository sizesRepository;
    @Mock LayoutProfileFontsRepository fontsRepository;
    @Mock LayoutNavigationService navigationService;

    @InjectMocks LayoutContextServiceImpl service;

    @AfterEach
    void clearTenant() {
        ResolvedTenantContextHolder.clear();
    }

    @Test
    void missingPublicAssignmentUsesPublicSafeFallback() {
        ResolvedTenantContextHolder.set(new ResolvedTenantContext(7L, "arsv", "abd.localhost"));
        when(clientLayoutProfileRepository
                .findFirstByTenantIdAndClientApplicationClientCodeAndDefaultProfileTrueAndActiveTrueOrderByDisplayOrderAscIdAsc(
                        7L, "CATALOG"))
                .thenReturn(Optional.empty());

        var context = service.getPublicLayout("CATALOG", "http://abd.localhost:4400");
        var profile = context.getAvailableProfiles().getFirst();

        assertThat(context.getActiveProfileCode()).isEqualTo("PUBLIC_SAFE_DEFAULT");
        assertThat(context.getNavTree()).isEmpty();
        assertThat(profile.getLayoutType()).isEqualTo(LayoutType.PUBLIC);
        assertThat(profile.getSidebarEnabled()).isFalse();
        assertThat(profile.getCommandBarEnabled()).isFalse();
        assertThat(profile.getTopbarEnabled()).isTrue();
        assertThat(profile.getFooterEnabled()).isTrue();
        assertThat(profile.getBreadcrumbEnabled()).isTrue();
    }
}
