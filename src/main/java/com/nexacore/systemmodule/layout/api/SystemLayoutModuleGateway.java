package com.nexacore.systemmodule.layout.api;

import com.nexacore.gatewaymodule.layout.dto.PublicFontConfigDto;
import com.nexacore.gatewaymodule.layout.dto.PublicLayoutBrandDto;
import com.nexacore.gatewaymodule.layout.dto.PublicLayoutContextDto;
import com.nexacore.gatewaymodule.layout.dto.PublicLayoutProfileDto;
import com.nexacore.gatewaymodule.layout.dto.PublicSizeConfigDto;
import com.nexacore.gatewaymodule.layout.dto.PublicThemeChromeDto;
import com.nexacore.gatewaymodule.layout.dto.PublicThemeDto;
import com.nexacore.gatewaymodule.layout.dto.PublicThemePrimariesDto;
import com.nexacore.gatewaymodule.layout.service.interfaces.LayoutModuleGateway;
import com.nexacore.systemmodule.layout.dto.LayoutBrandDto;
import com.nexacore.systemmodule.layout.dto.LayoutContextDto;
import com.nexacore.systemmodule.layout.dto.LayoutProfileDto;
import com.nexacore.systemmodule.layout.dto.ThemeChromeOverridesDto;
import com.nexacore.systemmodule.layout.dto.ThemeColorPrimariesDto;
import com.nexacore.systemmodule.layout.dto.ThemeConfigEntryDto;
import com.nexacore.systemmodule.layout.service.interfaces.LayoutContextService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(transactionManager = "systemTransactionManager", readOnly = true)
public class SystemLayoutModuleGateway implements LayoutModuleGateway {

    private final LayoutContextService layoutContextService;

    @Override
    public PublicLayoutContextDto getPublicLayout(String clientCode, String origin) {
        LayoutContextDto context = layoutContextService.getPublicLayout(clientCode, origin);
        return new PublicLayoutContextDto(
                context.getActiveProfileCode(),
                context.getAvailableProfiles().stream().map(this::profile).toList(),
                context.getThemes().stream().map(this::theme).toList(),
                context.getSizes() == null ? null : new PublicSizeConfigDto(
                        context.getSizes().getSpaceUnit(), context.getSizes().getRadiusBase(),
                        context.getSizes().getFontSizeBase(), context.getSizes().getHeaderHeight(),
                        context.getSizes().getStatusBarHeight(), context.getSizes().getRailWidthCollapsed(),
                        context.getSizes().getRailWidthExpanded()),
                context.getFonts() == null ? null : new PublicFontConfigDto(
                        context.getFonts().getBodyFamily(), context.getFonts().getHeadingFamily(),
                        context.getFonts().getMonoFamily(), enumName(context.getFonts().getFontSource()),
                        context.getFonts().getFontUrl(), context.getFonts().getFallbackStack()));
    }

    private PublicLayoutProfileDto profile(LayoutProfileDto profile) {
        return new PublicLayoutProfileDto(
                profile.getCode(), profile.getName(), enumName(profile.getLayoutType()),
                enumName(profile.getNavigationMode()), enumName(profile.getThemeMode()),
                enumName(profile.getDensity()), Boolean.TRUE.equals(profile.getTopbarEnabled()),
                Boolean.TRUE.equals(profile.getSidebarEnabled()), Boolean.TRUE.equals(profile.getSidebarCollapsed()),
                Boolean.TRUE.equals(profile.getFooterEnabled()), Boolean.TRUE.equals(profile.getBreadcrumbEnabled()),
                Boolean.TRUE.equals(profile.getCommandBarEnabled()), Boolean.TRUE.equals(profile.getRtlEnabled()),
                brand(profile.getBrand()));
    }

    private PublicLayoutBrandDto brand(LayoutBrandDto brand) {
        return brand == null ? null : new PublicLayoutBrandDto(
                brand.getDisplayName(), brand.getShortName(), brand.getLogoUrl(),
                brand.getLogoDarkUrl(), brand.getFaviconUrl(), brand.getSupportUrl());
    }

    private PublicThemeDto theme(ThemeConfigEntryDto theme) {
        return new PublicThemeDto(theme.getId(), theme.getLabel(), theme.getBase(), theme.getSwatch(),
                primaries(theme.getPrimaries()), chrome(theme.getChromeOverrides()));
    }

    private PublicThemePrimariesDto primaries(ThemeColorPrimariesDto colors) {
        return colors == null ? null : new PublicThemePrimariesDto(
                colors.getText(), colors.getPaper(), colors.getCard(), colors.getAccent(),
                colors.getAmber(), colors.getRed(), colors.getSuccess(), colors.getInfo());
    }

    private PublicThemeChromeDto chrome(ThemeChromeOverridesDto chrome) {
        return chrome == null ? null : new PublicThemeChromeDto(
                chrome.getAccentSoft(), chrome.getBg(), chrome.getBorder(), chrome.getBorderStrong(),
                chrome.getHoverBg(), chrome.getActiveBg(), chrome.getSearchBg(), chrome.getSearchBorder(),
                chrome.getSearchText(), chrome.getSearchPlaceholder());
    }

    private String enumName(Enum<?> value) {
        return value == null ? null : value.name();
    }
}
