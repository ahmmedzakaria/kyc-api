package com.nexacore.systemmodule.publicnavigation.dto;

import java.util.List;

public record PublicNavigationIntegrityDto(boolean valid, List<String> issues) {}
