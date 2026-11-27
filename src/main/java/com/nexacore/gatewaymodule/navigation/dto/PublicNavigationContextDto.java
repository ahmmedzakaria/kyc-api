package com.nexacore.gatewaymodule.navigation.dto;

import java.util.List;

public record PublicNavigationContextDto(String clientCode, List<Region> regions) {
    public record Region(String surface, List<Group> groups) {}
    public record Group(String code, String label, List<Item> items) {}
    public record Item(String code, String type, String label, String description, String ariaLabel,
                       String routePath, String externalUrl, String target, String icon, List<Item> children) {}
}
