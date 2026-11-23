package com.nexacore.catalogmodule.privilege;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogPrivilegeProviderTest {
    @Test
    void registersSeparateViewAndPublishPrivileges() {
        var feature = new CatalogPrivilegeProvider().getPrivilegeFeatures().getFirst();

        assertThat(feature.getActions()).extracting("privilegeCode")
                .contains("12010100101", "12010100123");
    }
}
