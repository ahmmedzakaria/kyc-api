package com.nexacore.catalogmodule.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogPropertiesBindingTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(CatalogConfiguration.class);

    @Test
    void bindsSystemNavigationFlagThroughTheCanonicalRecordConstructor() {
        contextRunner.withPropertyValues(
                        "catalog.default-locale=bn",
                        "catalog.supported-locales[0]=bn",
                        "catalog.media.namespace=tenant-catalog",
                        "catalog.media.public-path=/media",
                        "catalog.system-navigation-enabled=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    CatalogProperties properties = context.getBean(CatalogProperties.class);
                    assertThat(properties.defaultLocale()).isEqualTo("bn");
                    assertThat(properties.supportedLocales()).containsExactly("bn");
                    assertThat(properties.media().namespace()).isEqualTo("tenant-catalog");
                    assertThat(properties.systemNavigationEnabled()).isTrue();
                });
    }
}
