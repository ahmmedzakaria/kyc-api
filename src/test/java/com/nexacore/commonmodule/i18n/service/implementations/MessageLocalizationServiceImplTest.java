package com.nexacore.commonmodule.i18n.service.implementations;

import com.nexacore.commonmodule.i18n.config.I18nProperties;
import com.nexacore.commonmodule.i18n.dto.LocalizedMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class MessageLocalizationServiceImplTest {

    private MessageLocalizationServiceImpl service;

    @BeforeEach
    void setUp() {
        I18nProperties properties = new I18nProperties();
        properties.setDefaultLocale("en");

        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setBasenames("classpath:i18n/messages", "classpath:i18n/validation");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setUseCodeAsDefaultMessage(true);
        messageSource.setFallbackToSystemLocale(false);

        service = new MessageLocalizationServiceImpl(messageSource, properties);
    }

    @Test
    void getMessageResolvesEnglishMessage() {
        String message = service.getMessage("common.success", Locale.ENGLISH);

        assertThat(message).isEqualTo("Success");
    }

    @Test
    void getMessageResolvesBanglaMessage() {
        String message = service.getMessage("common.success", Locale.forLanguageTag("bn"));

        assertThat(message).isEqualTo("সফল");
    }

    @Test
    void getMessageResolvesParameterizedMessage() {
        String message = service.getMessage("common.error.type_mismatch", Locale.ENGLISH, "page", "abc");

        assertThat(message).isEqualTo("Parameter 'page' has invalid value 'abc'");
    }

    @Test
    void getMessageFallsBackToCodeWhenMissing() {
        String message = service.getMessage("missing.message.code", Locale.ENGLISH);

        assertThat(message).isEqualTo("missing.message.code");
    }

    @Test
    void getLocalizedMessageIncludesCodeTextAndLocale() {
        LocalizedMessage message = service.getLocalizedMessage("validation.required", Locale.forLanguageTag("bn"));

        assertThat(message.getCode()).isEqualTo("validation.required");
        assertThat(message.getText()).isEqualTo("এই তথ্যটি আবশ্যক");
        assertThat(message.getLocale()).isEqualTo("bn");
    }
}
