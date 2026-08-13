package com.nexacore.logmodule.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record LogPageDto<T>(List<T> items, long total, int page, int pageSize) {}
