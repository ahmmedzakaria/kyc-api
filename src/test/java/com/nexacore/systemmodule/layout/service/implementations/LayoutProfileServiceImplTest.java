package com.nexacore.systemmodule.layout.service.implementations;

import com.nexacore.gatewaymodule.auth.service.interfaces.AuthModuleGateway;
import com.nexacore.systemmodule.layout.dto.LayoutProfileRequestDto;
import com.nexacore.systemmodule.layout.dto.ThemeConfigEntryDto;
import com.nexacore.systemmodule.layout.entity.SysLayoutProfile;
import com.nexacore.systemmodule.layout.entity.SysLayoutProfileTheme;
import com.nexacore.systemmodule.layout.repository.LayoutProfileBrandingRepository;
import com.nexacore.systemmodule.layout.repository.LayoutProfileFontsRepository;
import com.nexacore.systemmodule.layout.repository.LayoutProfileRepository;
import com.nexacore.systemmodule.layout.repository.LayoutProfileSizesRepository;
import com.nexacore.systemmodule.layout.repository.LayoutProfileThemeRepository;
import com.nexacore.systemmodule.layout.repository.LayoutThemeChromeOverridesRepository;
import com.nexacore.systemmodule.layout.repository.LayoutThemePrimariesRepository;
import com.nexacore.systemmodule.layout.service.interfaces.LayoutCodeGenerationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LayoutProfileServiceImplTest {

    @Mock LayoutProfileRepository layoutProfileRepository;
    @Mock LayoutProfileBrandingRepository brandingRepository;
    @Mock LayoutProfileThemeRepository themeRepository;
    @Mock LayoutThemePrimariesRepository primariesRepository;
    @Mock LayoutThemeChromeOverridesRepository chromeOverridesRepository;
    @Mock LayoutProfileSizesRepository sizesRepository;
    @Mock LayoutProfileFontsRepository fontsRepository;
    @Mock LayoutCodeGenerationService codeGenerationService;
    @Mock AuthModuleGateway authModuleGateway;

    @InjectMocks LayoutProfileServiceImpl service;

    private SysLayoutProfile profile;

    @BeforeEach
    void setUp() {
        profile = new SysLayoutProfile();
        profile.setId(1L);
        when(authModuleGateway.getUserId("alice")).thenReturn(7L);
        when(layoutProfileRepository.findById(1L)).thenReturn(Optional.of(profile));
        when(layoutProfileRepository.save(profile)).thenReturn(profile);
        when(codeGenerationService.normalizeBusinessCode(any(), any())).thenReturn("DEFAULT");
    }

    @Test
    void updateFlushesExistingThemeDeletesBeforeInsertingReplacementWithSameId() {
        SysLayoutProfileTheme existing = SysLayoutProfileTheme.builder()
                .id(10L).layoutProfile(profile).themeId("purple").build();
        when(themeRepository.findByLayoutProfileId(1L)).thenReturn(List.of(existing));
        when(themeRepository.findByLayoutProfileIdAndActiveTrueOrderByIdAsc(1L)).thenReturn(List.of());
        when(brandingRepository.findFirstByLayoutProfileIdAndActiveTrueOrderByIdAsc(1L)).thenReturn(Optional.empty());
        when(sizesRepository.findFirstByLayoutProfileIdAndActiveTrue(1L)).thenReturn(Optional.empty());
        when(fontsRepository.findFirstByLayoutProfileIdAndActiveTrue(1L)).thenReturn(Optional.empty());

        service.save(updateRequest(List.of(theme("purple"))), "alice");

        InOrder order = inOrder(themeRepository);
        order.verify(themeRepository).deleteAll(List.of(existing));
        order.verify(themeRepository).flush();
        order.verify(themeRepository).save(any(SysLayoutProfileTheme.class));
    }

    @Test
    void updateRejectsDuplicateNormalizedThemeIdsBeforeDeletingExistingThemes() {
        LayoutProfileRequestDto request = updateRequest(List.of(theme(" purple "), theme("purple")));

        assertThatThrownBy(() -> service.save(request, "alice"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Duplicate theme id: purple");

        verify(themeRepository, never()).findByLayoutProfileId(any());
        verify(themeRepository, never()).deleteAll(any());
        verify(themeRepository, never()).flush();
    }

    private LayoutProfileRequestDto updateRequest(List<ThemeConfigEntryDto> themes) {
        LayoutProfileRequestDto request = new LayoutProfileRequestDto();
        request.setId(1L);
        request.setProfileName("Default");
        request.setThemes(themes);
        return request;
    }

    private ThemeConfigEntryDto theme(String id) {
        return ThemeConfigEntryDto.builder()
                .id(id).label("Purple").base("light").swatch("#800080")
                .build();
    }
}
