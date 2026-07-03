package com.nexacore.commonmodule.i18n.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "i18n")
public class I18nProperties {

    private String defaultLocale = "en";

    private List<String> supportedLocales = new ArrayList<>(List.of("en", "bn"));

    private String basename = "i18n/messages,i18n/validation";

    private String encoding = "UTF-8";

    private boolean fallbackToSystemLocale = false;

    private String languageParameter = "lang";
}
