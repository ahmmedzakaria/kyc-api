package com.nexacore.systemmodule.tenant.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HostnameNormalizerTest {
    private final HostnameNormalizer normalizer = new HostnameNormalizer();

    @Test void normalizesCasePortTrailingDotAndInternationalNames() {
        assertThat(normalizer.normalize(" Example.COM.:5301 ")).isEqualTo("example.com");
        assertThat(normalizer.normalize("bücher.example")).isEqualTo("xn--bcher-kva.example");
        assertThat(normalizer.normalize("[::1]:5301")).isEqualTo("::1");
        assertThat(normalizer.normalize("http://bdcom.localhost:5301/")).isEqualTo("bdcom.localhost");
    }

    @Test void rejectsMissingHost() {
        assertThatThrownBy(() -> normalizer.normalize(" ")).isInstanceOf(IllegalArgumentException.class);
    }
}
