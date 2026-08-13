package com.nexacore.commonmodule.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class AssignmentVersionTest {
    @Test
    void isOrderIndependentAndRejectsStaleReplacementTokens() {
        String version = AssignmentVersion.of(List.of(3L, 1L, 2L));
        assertThat(version).isEqualTo(AssignmentVersion.of(List.of(1L, 2L, 3L)));
        AssignmentVersion.requireCurrent(version, List.of(2L, 3L, 1L));
        assertThatIllegalArgumentException().isThrownBy(() ->
                AssignmentVersion.requireCurrent(version, List.of(1L, 2L)));
    }
}
