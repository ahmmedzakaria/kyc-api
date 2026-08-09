package com.nexacore.systemmodule.layout.service.implementations;

import com.nexacore.systemmodule.layout.dto.FontConfigDto;
import com.nexacore.systemmodule.layout.dto.LayoutBrandDto;
import com.nexacore.systemmodule.layout.dto.LayoutContextDto;
import com.nexacore.systemmodule.layout.dto.LayoutProfileDto;
import com.nexacore.systemmodule.layout.dto.SizeConfigDto;
import com.nexacore.systemmodule.layout.dto.ThemeChromeOverridesDto;
import com.nexacore.systemmodule.layout.dto.ThemeColorPrimariesDto;
import com.nexacore.systemmodule.layout.dto.ThemeConfigEntryDto;
import com.nexacore.systemmodule.layout.entity.SysClientLayoutProfile;
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
import com.nexacore.systemmodule.layout.repository.ClientLayoutProfileRepository;
import com.nexacore.systemmodule.layout.repository.LayoutProfileBrandingRepository;
import com.nexacore.systemmodule.layout.repository.LayoutProfileFontsRepository;
import com.nexacore.systemmodule.layout.repository.LayoutProfileRepository;
import com.nexacore.systemmodule.layout.repository.LayoutProfileSizesRepository;
import com.nexacore.systemmodule.layout.repository.LayoutProfileThemeRepository;
import com.nexacore.systemmodule.layout.repository.LayoutThemeChromeOverridesRepository;
import com.nexacore.systemmodule.layout.repository.LayoutThemePrimariesRepository;
import com.nexacore.systemmodule.layout.service.interfaces.LayoutContextService;
import com.nexacore.systemmodule.layout.service.interfaces.LayoutNavigationService;
import com.nexacore.systemmodule.accesscontrol.entity.SysAccClientApplication;
import com.nexacore.systemmodule.accesscontrol.repository.ClientApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class LayoutContextServiceImpl implements LayoutContextService {
    private static final String DEFAULT_PROFILE_CODE = "WEB_DEFAULT";

    private final ClientApplicationRepository clientApplicationRepository;
    private final ClientLayoutProfileRepository clientLayoutProfileRepository;
    private final LayoutProfileRepository layoutProfileRepository;
    private final LayoutProfileBrandingRepository brandingRepository;
    private final LayoutProfileThemeRepository themeRepository;
    private final LayoutThemePrimariesRepository primariesRepository;
    private final LayoutThemeChromeOverridesRepository chromeOverridesRepository;
    private final LayoutProfileSizesRepository sizesRepository;
    private final LayoutProfileFontsRepository fontsRepository;
    private final LayoutNavigationService navigationService;

    @Override
    @Transactional(transactionManager = "systemTransactionManager", readOnly = true)
    public LayoutContextDto getEffectiveLayout(String clientCode, String username, Set<String> privilegeCodes) {
        SysLayoutProfile activeProfile = resolveDefaultProfile(clientCode);
        List<LayoutProfileDto> profiles = resolveAvailableProfiles(clientCode);
        if (activeProfile == null) {
            return defaultContext(clientCode);
        }
        return LayoutContextDto.builder()
                .activeProfileCode(activeProfile.getProfileCode())
                .availableProfiles(profiles.isEmpty() ? List.of(toProfileDto(activeProfile)) : profiles)
                .navTree(navigationService.getNavigationTree(clientCode, username, privilegeCodes))
                .themes(resolveThemes(activeProfile))
                .sizes(resolveSizes(activeProfile))
                .fonts(resolveFonts(activeProfile))
                .build();
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager", readOnly = true)
    public LayoutContextDto getPublicLayout(String clientCode, String origin) {
        SysLayoutProfile activeProfile = resolveDefaultProfile(clientCode);
        if (activeProfile == null) {
            return defaultContext(clientCode);
        }
        return LayoutContextDto.builder()
                .activeProfileCode(activeProfile.getProfileCode())
                .availableProfiles(List.of(toProfileDto(activeProfile)))
                .navTree(new ArrayList<>())
                .themes(resolveThemes(activeProfile))
                .sizes(resolveSizes(activeProfile))
                .fonts(resolveFonts(activeProfile))
                .build();
    }

    private SysLayoutProfile resolveDefaultProfile(String clientCode) {
        if (clientCode != null && !clientCode.isBlank()) {
            return clientLayoutProfileRepository
                    .findFirstByClientApplicationClientCodeAndDefaultProfileTrueAndActiveTrueOrderByDisplayOrderAscIdAsc(clientCode)
                    .map(SysClientLayoutProfile::getLayoutProfile)
                    .orElseGet(() -> layoutProfileRepository.findByProfileCode(DEFAULT_PROFILE_CODE).orElse(null));
        }
        return layoutProfileRepository.findByProfileCode(DEFAULT_PROFILE_CODE).orElse(null);
    }

    private List<LayoutProfileDto> resolveAvailableProfiles(String clientCode) {
        if (clientCode == null || clientCode.isBlank()) {
            return layoutProfileRepository.findByActiveTrueOrderByProfileCodeAsc().stream()
                    .map(this::toProfileDto)
                    .toList();
        }
        return clientApplicationRepository.findByClientCode(clientCode)
                .map(SysAccClientApplication::getId)
                .map(clientLayoutProfileRepository::findByClientApplicationIdAndActiveTrueOrderByDisplayOrderAscIdAsc)
                .orElse(List.of())
                .stream()
                .filter(SysClientLayoutProfile::isSelectable)
                .map(SysClientLayoutProfile::getLayoutProfile)
                .filter(SysLayoutProfile::isActive)
                .map(this::toProfileDto)
                .toList();
    }

    private List<ThemeConfigEntryDto> resolveThemes(SysLayoutProfile profile) {
        List<ThemeConfigEntryDto> themes = themeRepository.findByLayoutProfileIdAndActiveTrueOrderByIdAsc(profile.getId()).stream()
                .map(this::toThemeDto)
                .toList();
        return themes.isEmpty() ? List.of(defaultTheme()) : themes;
    }

    private ThemeConfigEntryDto toThemeDto(SysLayoutProfileTheme theme) {
        return ThemeConfigEntryDto.builder()
                .id(theme.getThemeId())
                .label(theme.getThemeLabel())
                .base(theme.getBase())
                .swatch(theme.getSwatch())
                .primaries(primariesRepository.findFirstByLayoutProfileThemeIdAndActiveTrue(theme.getId())
                        .map(this::toPrimariesDto)
                        .orElse(defaultPrimaries()))
                .chromeOverrides(chromeOverridesRepository.findFirstByLayoutProfileThemeIdAndActiveTrue(theme.getId())
                        .map(this::toChromeOverridesDto)
                        .orElse(null))
                .build();
    }

    private LayoutProfileDto toProfileDto(SysLayoutProfile profile) {
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

    private SizeConfigDto resolveSizes(SysLayoutProfile profile) {
        return sizesRepository.findFirstByLayoutProfileIdAndActiveTrue(profile.getId())
                .map(this::toSizesDto)
                .orElse(defaultSizes());
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

    private FontConfigDto resolveFonts(SysLayoutProfile profile) {
        return fontsRepository.findFirstByLayoutProfileIdAndActiveTrue(profile.getId())
                .map(this::toFontsDto)
                .orElse(defaultFonts());
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

    private LayoutContextDto defaultContext(String clientCode) {
        LayoutProfileDto profile = LayoutProfileDto.builder()
                .code(DEFAULT_PROFILE_CODE)
                .name("Default")
                .layoutType(LayoutType.RAIL)
                .navigationMode(NavigationMode.MODULE_GROUP_MEGA_PANEL)
                .themeMode(ThemeMode.LIGHT)
                .density(LayoutDensity.COMFORTABLE)
                .topbarEnabled(true)
                .sidebarEnabled(true)
                .sidebarCollapsed(false)
                .footerEnabled(true)
                .breadcrumbEnabled(true)
                .commandBarEnabled(true)
                .rtlEnabled(false)
                .active(true)
                .brand(LayoutBrandDto.builder().displayName(clientCode == null || clientCode.isBlank() ? "NexaCore KYC" : clientCode).build())
                .build();
        return LayoutContextDto.builder()
                .activeProfileCode(DEFAULT_PROFILE_CODE)
                .availableProfiles(List.of(profile))
                .navTree(new ArrayList<>())
                .themes(List.of(defaultTheme()))
                .sizes(defaultSizes())
                .fonts(defaultFonts())
                .build();
    }

    private ThemeConfigEntryDto defaultTheme() {
        return ThemeConfigEntryDto.builder()
                .id("purple")
                .label("Purple Corporate")
                .base("light")
                .swatch("#6b3fa0")
                .primaries(defaultPrimaries())
                .build();
    }

    private ThemeColorPrimariesDto defaultPrimaries() {
        return ThemeColorPrimariesDto.builder()
                .text("#1a222c")
                .paper("#f3f5f7")
                .card("#ffffff")
                .accent("#6b3fa0")
                .amber("#a8630b")
                .red("#9f2b2b")
                .success("#1c7a4c")
                .info("#2f7dd1")
                .build();
    }

    private SizeConfigDto defaultSizes() {
        return SizeConfigDto.builder()
                .spaceUnit(2.0)
                .radiusBase(8.0)
                .fontSizeBase(13.5)
                .headerHeight(58.0)
                .statusBarHeight(28.0)
                .railWidthCollapsed(64.0)
                .railWidthExpanded(230.0)
                .build();
    }

    private FontConfigDto defaultFonts() {
        return FontConfigDto.builder()
                .bodyFamily("Inter")
                .headingFamily("Inter")
                .monoFamily("JetBrains Mono")
                .fontSource(FontSource.SYSTEM)
                .fallbackStack("system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif")
                .build();
    }
}
