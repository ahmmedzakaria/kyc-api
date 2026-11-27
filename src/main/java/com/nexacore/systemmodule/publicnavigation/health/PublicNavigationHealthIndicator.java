package com.nexacore.systemmodule.publicnavigation.health;

import com.nexacore.systemmodule.publicnavigation.repository.ClientPublicNavigationItemRepository;
import com.nexacore.systemmodule.publicnavigation.repository.PublicNavigationGroupRepository;
import com.nexacore.systemmodule.publicnavigation.repository.PublicNavigationItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("publicNavigation")
@RequiredArgsConstructor
public class PublicNavigationHealthIndicator implements HealthIndicator {
    private final PublicNavigationGroupRepository groups;
    private final PublicNavigationItemRepository items;
    private final ClientPublicNavigationItemRepository assignments;

    @Override
    public Health health() {
        try {
            return Health.up().withDetail("groups", groups.count())
                    .withDetail("items", items.count()).withDetail("assignments", assignments.count()).build();
        } catch (RuntimeException exception) {
            return Health.down(exception).build();
        }
    }
}
