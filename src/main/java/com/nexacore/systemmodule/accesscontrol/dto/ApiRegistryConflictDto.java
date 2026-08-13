package com.nexacore.systemmodule.accesscontrol.dto;

import java.util.List;

public record ApiRegistryConflictDto(String apiCode, List<String> candidateHandlers, List<String> reasons) {}
