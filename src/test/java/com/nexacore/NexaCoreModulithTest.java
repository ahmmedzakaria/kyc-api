package com.nexacore;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class NexaCoreModulithTest {

    @Test
    void discoversApplicationModules() {
        ApplicationModules modules = ApplicationModules.of(NexaCoreApplication.class);

        AtomicInteger moduleCount = new AtomicInteger();
        modules.forEach(module -> moduleCount.incrementAndGet());

        assertThat(moduleCount.get()).isGreaterThanOrEqualTo(8);
    }

    @Test
    void verifiesApplicationModuleStructure() {
        ApplicationModules.of(NexaCoreApplication.class).verify();
    }
}
