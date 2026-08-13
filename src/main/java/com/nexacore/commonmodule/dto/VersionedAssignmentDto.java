package com.nexacore.commonmodule.dto;

import java.util.List;

public record VersionedAssignmentDto<T>(String version, List<T> items) {
}
