package com.nexacore.authmodule.core.service;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UsernameNormalizerTest {
    private final UsernameNormalizer normalizer = new UsernameNormalizer();

    @Test
    void trimsNormalizesCompatibilityCharactersAndLowercasesWithRootLocale() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            assertThat(normalizer.normalize("  ＲＡＨＩＭ.I  ")).isEqualTo("rahim.i");
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test
    void rejectsMissingBlankAndOversizedUsernames() {
        assertThatThrownBy(() -> normalizer.normalize(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> normalizer.normalize("   "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> normalizer.normalize("a".repeat(UsernameNormalizer.MAX_LENGTH + 1)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
