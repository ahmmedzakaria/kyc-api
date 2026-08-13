package com.nexacore.systemmodule.layout.dto;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class LayoutNavigationIntegrityDto {
    private boolean valid;
    @Builder.Default private List<String> invalidParents = new ArrayList<>();
    @Builder.Default private List<String> duplicateCodes = new ArrayList<>();
    @Builder.Default private List<String> duplicateRoutes = new ArrayList<>();
    @Builder.Default private List<String> unknownRoutes = new ArrayList<>();
    @Builder.Default private List<String> invalidPrivilegeReferences = new ArrayList<>();
}
