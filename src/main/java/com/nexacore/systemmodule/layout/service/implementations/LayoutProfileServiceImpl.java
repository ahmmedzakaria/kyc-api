package com.nexacore.systemmodule.layout.service.implementations;

import com.nexacore.gatewaymodule.auth.service.interfaces.AuthModuleGateway;
import com.nexacore.systemmodule.layout.dto.FontConfigDto;
import com.nexacore.systemmodule.layout.dto.LayoutBrandDto;
import com.nexacore.systemmodule.layout.dto.LayoutProfileDto;
import com.nexacore.systemmodule.layout.dto.LayoutProfileRequestDto;
import com.nexacore.systemmodule.layout.dto.SizeConfigDto;
import com.nexacore.systemmodule.layout.dto.ThemeChromeOverridesDto;
import com.nexacore.systemmodule.layout.dto.ThemeColorPrimariesDto;
import com.nexacore.systemmodule.layout.dto.ThemeConfigEntryDto;
import com.nexacore.systemmodule.layout.entity.SysLayoutProfile;
import com.nexacore.systemmodule.layout.entity.SysLayoutProfileBranding;
import com.nexacore.systemmodule.layout.entity.SysLayoutProfileFonts;
import com.nexacore.systemmodule.layout.entity.SysLayoutProfileSizes;
import com.nexacore.systemmodule.layout.entity.SysLayoutProfileTheme;
import com.nexacore.systemmodule.layout.entity.SysLayoutThemeChromeOverrides;
import com.nexacore.systemmodule.layout.entity.SysLayoutThemePrimaries;
import com.nexacore.systemmodule.layout.enums.FontSource;
import com.nexacore.systemmodule.layout.enums.LayoutDensity;
import com.nexacore.systemmodule.layout.enums.LayoutType;
import com.nexacore.systemmodule.layout.enums.NavigationMode;
import com.nexacore.systemmodule.layout.enums.ThemeMode;
import com.nexacore.systemmodule.layout.repository.LayoutProfileBrandingRepository;
import com.nexacore.systemmodule.layout.repository.LayoutProfileFontsRepository;
import com.nexacore.systemmodule.layout.repository.LayoutProfileRepository;
import com.nexacore.systemmodule.layout.repository.LayoutProfileSizesRepository;
import com.nexacore.systemmodule.layout.repository.LayoutProfileThemeRepository;
import com.nexacore.systemmodule.layout.repository.LayoutThemeChromeOverridesRepository;
import com.nexacore.systemmodule.layout.repository.LayoutThemePrimariesRepository;
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
    private final LayoutProfileThemeRepository themeRepository;
    private final LayoutThemePrimariesRepository primariesRepository;
    private final LayoutThemeChromeOverridesRepository chromeOverridesRepository;
    private final LayoutProfileSizesRepository sizesRepository;
    private final LayoutProfileFontsRepository fontsRepository;
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

        SysLayoutProfile saved = layoutProfileRepository.save(profile);
        if (request.getThemes() != null) {
            replaceThemes(saved, request.getThemes(), userId);
        }
        if (request.getSizes() != null) {
            upsertSizes(saved, request.getSizes(), userId);
        }
        if (request.getFonts() != null) {
            upsertFonts(saved, request.getFonts(), userId);
        }

        return toDto(saved);
    }

    private void replaceThemes(SysLayoutProfile profile, List<ThemeConfigEntryDto> themes, Long userId) {
        for (SysLayoutProfileTheme existing : themeRepository.findByLayoutProfileId(profile.getId())) {
            primariesRepository.deleteByLayoutProfileThemeId(existing.getId());
            chromeOverridesRepository.deleteByLayoutProfileThemeId(existing.getId());
        }
        themeRepository.deleteAll(themeRepository.findByLayoutProfileId(profile.getId()));

        for (ThemeConfigEntryDto themeDto : themes) {
            SysLayoutProfileTheme theme = SysLayoutProfileTheme.builder()
                    .layoutProfile(profile)
                    .themeId(required(themeDto.getId(), "themes[].id"))
                    .themeLabel(required(themeDto.getLabel(), "themes[].label"))
                    .base(required(themeDto.getBase(), "themes[].base"))
                    .swatch(required(themeDto.getSwatch(), "themes[].swatch"))
                    .active(true)
                    .build();
            theme.setCreatedBy(userId);
            theme.setUpdatedBy(userId);
            theme = themeRepository.save(theme);

            ThemeColorPrimariesDto primariesDto = themeDto.getPrimaries();
            if (primariesDto != null) {
                SysLayoutThemePrimaries primaries = SysLayoutThemePrimaries.builder()
                        .layoutProfileTheme(theme)
                        .textColor(primariesDto.getText())
                        .paperColor(primariesDto.getPaper())
                        .cardColor(primariesDto.getCard())
                        .accentColor(primariesDto.getAccent())
                        .amberColor(primariesDto.getAmber())
                        .redColor(primariesDto.getRed())
                        .successColor(primariesDto.getSuccess())
                        .infoColor(primariesDto.getInfo())
                        .active(true)
                        .build();
                primaries.setCreatedBy(userId);
                primaries.setUpdatedBy(userId);
                primariesRepository.save(primaries);
            }

            ThemeChromeOverridesDto overridesDto = themeDto.getChromeOverrides();
            if (overridesDto != null) {
                SysLayoutThemeChromeOverrides overrides = SysLayoutThemeChromeOverrides.builder()
                        .layoutProfileTheme(theme)
                        .accentSoft(overridesDto.getAccentSoft())
                        .bg(overridesDto.getBg())
                        .border(overridesDto.getBorder())
                        .borderStrong(overridesDto.getBorderStrong())
                        .hoverBg(overridesDto.getHoverBg())
                        .activeBg(overridesDto.getActiveBg())
                        .searchBg(overridesDto.getSearchBg())
                        .searchBorder(overridesDto.getSearchBorder())
                        .searchText(overridesDto.getSearchText())
                        .searchPlaceholder(overridesDto.getSearchPlaceholder())
                        .active(true)
                        .build();
                overrides.setCreatedBy(userId);
                overrides.setUpdatedBy(userId);
                chromeOverridesRepository.save(overrides);
            }
        }
    }

    private void upsertSizes(SysLayoutProfile profile, SizeConfigDto sizesDto, Long userId) {
        SysLayoutProfileSizes sizes = sizesRepository.findFirstByLayoutProfileIdAndActiveTrue(profile.getId())
                .orElseGet(() -> {
                    SysLayoutProfileSizes created = SysLayoutProfileSizes.builder().layoutProfile(profile).active(true).build();
                    created.setCreatedBy(userId);
                    return created;
                });
        sizes.setSpaceUnit(sizesDto.getSpaceUnit());
        sizes.setRadiusBase(sizesDto.getRadiusBase());
        sizes.setFontSizeBase(sizesDto.getFontSizeBase());
        sizes.setHeaderHeight(sizesDto.getHeaderHeight());
        sizes.setStatusBarHeight(sizesDto.getStatusBarHeight());
        sizes.setRailWidthCollapsed(sizesDto.getRailWidthCollapsed());
        sizes.setRailWidthExpanded(sizesDto.getRailWidthExpanded());
        sizes.setUpdatedBy(userId);
        sizesRepository.save(sizes);
    }

    private void upsertFonts(SysLayoutProfile profile, FontConfigDto fontsDto, Long userId) {
        SysLayoutProfileFonts fonts = fontsRepository.findFirstByLayoutProfileIdAndActiveTrue(profile.getId())
                .orElseGet(() -> {
                    SysLayoutProfileFonts created = SysLayoutProfileFonts.builder().layoutProfile(profile).active(true).build();
                    created.setCreatedBy(userId);
                    return created;
                });
        fonts.setBodyFamily(fontsDto.getBodyFamily());
        fonts.setHeadingFamily(fontsDto.getHeadingFamily());
        fonts.setMonoFamily(fontsDto.getMonoFamily());
        fonts.setFontSource(fontsDto.getFontSource() == null ? FontSource.SYSTEM : fontsDto.getFontSource());
        fonts.setFontUrl(fontsDto.getFontUrl());
        fonts.setFallbackStack(fontsDto.getFallbackStack());
        fonts.setUpdatedBy(userId);
        fontsRepository.save(fonts);
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
                .themes(themeRepository.findByLayoutProfileIdAndActiveTrueOrderByIdAsc(profile.getId()).stream()
                        .map(this::toThemeDto)
                        .toList())
                .sizes(sizesRepository.findFirstByLayoutProfileIdAndActiveTrue(profile.getId())
                        .map(this::toSizesDto)
                        .orElse(null))
                .fonts(fontsRepository.findFirstByLayoutProfileIdAndActiveTrue(profile.getId())
                        .map(this::toFontsDto)
                        .orElse(null))
                .build();
    }

    private ThemeConfigEntryDto toThemeDto(SysLayoutProfileTheme theme) {
        return ThemeConfigEntryDto.builder()
                .id(theme.getThemeId())
                .label(theme.getThemeLabel())
                .base(theme.getBase())
                .swatch(theme.getSwatch())
                .primaries(primariesRepository.findFirstByLayoutProfileThemeIdAndActiveTrue(theme.getId())
                        .map(this::toPrimariesDto)
                        .orElse(null))
                .chromeOverrides(chromeOverridesRepository.findFirstByLayoutProfileThemeIdAndActiveTrue(theme.getId())
                        .map(this::toChromeOverridesDto)
                        .orElse(null))
                .build();
    }

    private ThemeColorPrimariesDto toPrimariesDto(SysLayoutThemePrimaries primaries) {
        return ThemeColorPrimariesDto.builder()
                .text(primaries.getTextColor())
                .paper(primaries.getPaperColor())
                .card(primaries.getCardColor())
                .accent(primaries.getAccentColor())
                .amber(primaries.getAmberColor())
                .red(primaries.getRedColor())
                .success(primaries.getSuccessColor())
                .info(primaries.getInfoColor())
                .build();
    }

    private ThemeChromeOverridesDto toChromeOverridesDto(SysLayoutThemeChromeOverrides overrides) {
        return ThemeChromeOverridesDto.builder()
                .accentSoft(overrides.getAccentSoft())
                .bg(overrides.getBg())
                .border(overrides.getBorder())
                .borderStrong(overrides.getBorderStrong())
                .hoverBg(overrides.getHoverBg())
                .activeBg(overrides.getActiveBg())
                .searchBg(overrides.getSearchBg())
                .searchBorder(overrides.getSearchBorder())
                .searchText(overrides.getSearchText())
                .searchPlaceholder(overrides.getSearchPlaceholder())
                .build();
    }

    private SizeConfigDto toSizesDto(SysLayoutProfileSizes sizes) {
        return SizeConfigDto.builder()
                .spaceUnit(sizes.getSpaceUnit())
                .radiusBase(sizes.getRadiusBase())
                .fontSizeBase(sizes.getFontSizeBase())
                .headerHeight(sizes.getHeaderHeight())
                .statusBarHeight(sizes.getStatusBarHeight())
                .railWidthCollapsed(sizes.getRailWidthCollapsed())
                .railWidthExpanded(sizes.getRailWidthExpanded())
                .build();
    }

    private FontConfigDto toFontsDto(SysLayoutProfileFonts fonts) {
        return FontConfigDto.builder()
                .bodyFamily(fonts.getBodyFamily())
                .headingFamily(fonts.getHeadingFamily())
                .monoFamily(fonts.getMonoFamily())
                .fontSource(fonts.getFontSource())
                .fontUrl(fonts.getFontUrl())
                .fallbackStack(fonts.getFallbackStack())
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
