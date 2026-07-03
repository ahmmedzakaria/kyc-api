package com.nexacore.commonmodule.i18n.service.implementations;

import com.nexacore.commonmodule.i18n.config.I18nProperties;
import com.nexacore.commonmodule.i18n.dto.LocalizedMessage;
import com.nexacore.commonmodule.i18n.service.interfaces.MessageLocalizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class MessageLocalizationServiceImpl implements MessageLocalizationService {

    private final MessageSource messageSource;
    private final I18nProperties properties;

    @Override
    public String getMessage(String code, Object... args) {
        return getMessage(code, LocaleContextHolder.getLocale(), args);
    }

    @Override
    public String getMessage(String code, Locale locale, Object... args) {
        Locale resolvedLocale = locale == null ? Locale.forLanguageTag(properties.getDefaultLocale()) : locale;
        String resolvedCode = StringUtils.hasText(code) ? code : "common.error.unexpected";
        return messageSource.getMessage(resolvedCode, args, resolvedCode, resolvedLocale);
    }

    @Override
    public LocalizedMessage getLocalizedMessage(String code, Object... args) {
        return getLocalizedMessage(code, LocaleContextHolder.getLocale(), args);
    }

    @Override
    public LocalizedMessage getLocalizedMessage(String code, Locale locale, Object... args) {
        Locale resolvedLocale = locale == null ? Locale.forLanguageTag(properties.getDefaultLocale()) : locale;
        return LocalizedMessage.builder()
                .code(code)
                .text(getMessage(code, resolvedLocale, args))
                .locale(resolvedLocale.toLanguageTag())
                .build();
    }
}
