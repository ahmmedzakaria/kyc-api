package com.nexacore.systemmodule.layout.service.implementations;

import com.nexacore.gatewaymodule.auth.service.interfaces.AuthModuleGateway;
import com.nexacore.systemmodule.layout.dto.LayoutBrandDto;
import com.nexacore.systemmodule.layout.dto.LayoutProfileDto;
import com.nexacore.systemmodule.layout.dto.LayoutProfileRequestDto;
import com.nexacore.systemmodule.layout.entity.SysLayoutProfile;
import com.nexacore.systemmodule.layout.entity.SysLayoutProfileBranding;
import com.nexacore.systemmodule.layout.enums.LayoutDensity;
import com.nexacore.systemmodule.layout.enums.LayoutType;
import com.nexacore.systemmodule.layout.enums.NavigationMode;
import com.nexacore.systemmodule.layout.enums.ThemeMode;
import com.nexacore.systemmodule.layout.repository.LayoutProfileBrandingRepository;
import com.nexacore.systemmodule.layout.repository.LayoutProfileRepository;
import com.nexacore.systemmodule.layout.service.interfaces.LayoutCodeGenerationService;
import com.nexacore.systemmodule.layout.service.interfaces.LayoutProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LayoutProfileServiceImpl implements LayoutProfileService {
    private final LayoutProfileRepository layoutProfileRepository;
    private final LayoutProfileBrandingRepository brandingRepository;
    private final LayoutCodeGenerationService codeGenerationService;
    private final AuthModuleGateway authModuleGateway;

    @Override
    @Transactional(transactionManager = "systemTransactionManager")
    public LayoutProfileDto save(LayoutProfileRequestDto request, String actor) {
        Long userId = authModuleGateway.getUserId(actor);
        SysLayoutProfile profile = request.getId() == null
                ? new SysLayoutProfile()
                : layoutProfileRepository.findById(request.getId())
                .orElseThrow(() -> new IllegalArgumentException("Layout profile not found: " + request.getId()));

        profile.setProfileCode(codeGenerationService.normalizeBusinessCode(request.getProfileCode(), request.getProfileName()));
        profile.setProfileName(required(request.getProfileName(), "profileName"));
        profile.setDescription(request.getDescription());
        profile.setLayoutType(defaultValue(request.getLayoutType(), LayoutType.RAIL));
        profile.setNavigationMode(defaultValue(request.getNavigationMode(), NavigationMode.MODULE_GROUP_MEGA_PANEL));
        profile.setThemeMode(defaultValue(request.getThemeMode(), ThemeMode.LIGHT));
        profile.setDensity(defaultValue(request.getDensity(), LayoutDensity.COMFORTABLE));
        profile.setTopbarEnabled(defaultBool(request.getTopbarEnabled(), true));
        profile.setSidebarEnabled(defaultBool(request.getSidebarEnabled(), true));
        profile.setSidebarCollapsed(defaultBool(request.getSidebarCollapsed(), false));
        profile.setFooterEnabled(defaultBool(request.getFooterEnabled(), true));
        profile.setBreadcrumbEnabled(defaultBool(request.getBreadcrumbEnabled(), true));
        profile.setCommandBarEnabled(defaultBool(request.getCommandBarEnabled(), true));
        profile.setRtlEnabled(defaultBool(request.getRtlEnabled(), false));
        profile.setActive(defaultBool(request.getActive(), true));
        if (profile.getId() == null) {
            profile.setCreatedBy(userId);
        }
        profile.setUpdatedBy(userId);

        return toDto(layoutProfileRepository.save(profile));
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager", readOnly = true)
    public List<LayoutProfileDto> list() {
        return layoutProfileRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager", readOnly = true)
    public LayoutProfileDto detail(String profileCode) {
        return layoutProfileRepository.findByProfileCode(profileCode)
                .map(this::toDto)
                .orElseThrow(() -> new IllegalArgumentException("Layout profile not found: " + profileCode));
    }

    LayoutProfileDto toDto(SysLayoutProfile profile) {
        return LayoutProfileDto.builder()
                .id(profile.getId())
                .code(profile.getProfileCode())
                .name(profile.getProfileName())
                .description(profile.getDescription())
                .layoutType(profile.getLayoutType())
                .navigationMode(profile.getNavigationMode())
                .themeMode(profile.getThemeMode())
                .density(profile.getDensity())
                .topbarEnabled(profile.isTopbarEnabled())
                .sidebarEnabled(profile.isSidebarEnabled())
                .sidebarCollapsed(profile.isSidebarCollapsed())
                .footerEnabled(profile.isFooterEnabled())
                .breadcrumbEnabled(profile.isBreadcrumbEnabled())
                .commandBarEnabled(profile.isCommandBarEnabled())
                .rtlEnabled(profile.isRtlEnabled())
                .active(profile.isActive())
                .brand(brandingRepository.findFirstByLayoutProfileIdAndActiveTrueOrderByIdAsc(profile.getId())
                        .map(this::toBrandDto)
                        .orElse(null))
                .build();
    }

    private LayoutBrandDto toBrandDto(SysLayoutProfileBranding branding) {
        return LayoutBrandDto.builder()
                .displayName(branding.getDisplayName())
                .shortName(branding.getShortName())
                .logoUrl(branding.getLogoUrl())
                .logoDarkUrl(branding.getLogoDarkUrl())
                .faviconUrl(branding.getFaviconUrl())
                .supportUrl(branding.getSupportUrl())
                .build();
    }

    private String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private boolean defaultBool(Boolean value, boolean fallback) {
        return value == null ? fallback : value;
    }

    private <T> T defaultValue(T value, T fallback) {
        return value == null ? fallback : value;
    }
}
