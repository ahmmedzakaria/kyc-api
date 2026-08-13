package com.nexacore.logmodule.dto;

import java.time.LocalDateTime;

/**
 * Shared filter/pagination shape for all three log list endpoints.
 * {@code status}/{@code errorType} only apply to Error/Access logs — the
 * Audit query simply ignores them.
 */
public record LogListRequestDto(
        Integer page,
        Integer pageSize,
        LocalDateTime from,
        LocalDateTime to,
        String username,
        String moduleCode,
        String traceId,
        Integer status,
        String errorType
) {
    public static LogListRequestDto empty() {
        return new LogListRequestDto(null, null, null, null, null, null, null, null, null);
    }
}
