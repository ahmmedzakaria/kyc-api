package com.nexacore.commonmodule.i18n.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;
import java.util.Locale;

@Configuration
@RequiredArgsConstructor
public class I18nConfig implements WebMvcConfigurer {

    private final I18nProperties properties;

    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setBasenames(resolveBasenames());
        messageSource.setDefaultEncoding(properties.getEncoding());
        messageSource.setFallbackToSystemLocale(properties.isFallbackToSystemLocale());
        messageSource.setUseCodeAsDefaultMessage(true);
        return messageSource;
    }

    @Bean
    public LocaleResolver localeResolver() {
        List<Locale> supportedLocales = resolveSupportedLocales();
        Locale defaultLocale = Locale.forLanguageTag(properties.getDefaultLocale());

        return new LocaleResolver() {
            @Override
            public Locale resolveLocale(HttpServletRequest request) {
                String languageParameter = request.getParameter(properties.getLanguageParameter());
                if (languageParameter != null && !languageParameter.isBlank()) {
                    return resolveSupportedLocale(Locale.forLanguageTag(languageParameter), supportedLocales, defaultLocale);
                }

                Locale requestLocale = request.getLocale();
                return resolveSupportedLocale(requestLocale, supportedLocales, defaultLocale);
            }

            @Override
            public void setLocale(HttpServletRequest request, HttpServletResponse response, Locale locale) {
                // Locale is resolved per request from lang parameter or Accept-Language header.
            }
        };
    }

    private String[] resolveBasenames() {
        return properties.getBasename().split("\\s*,\\s*");
    }

    private List<Locale> resolveSupportedLocales() {
        return properties.getSupportedLocales().stream()
                .map(Locale::forLanguageTag)
                .toList();
    }

    private Locale resolveSupportedLocale(Locale candidate, List<Locale> supportedLocales, Locale defaultLocale) {
        if (candidate == null) {
            return defaultLocale;
        }

        return supportedLocales.stream()
                .filter(locale -> locale.getLanguage().equalsIgnoreCase(candidate.getLanguage()))
                .findFirst()
                .orElse(defaultLocale);
    }
}
