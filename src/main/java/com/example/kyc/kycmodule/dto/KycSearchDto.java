package com.example.kyc.kycmodule.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record KycSearchDto(
           String searchText,
           @Schema(example = "1", description = "Page Number")
           @NotNull(message = "page can not be null")
           Integer page,
           @Schema(example = "1", description = "Page Number")
           @NotNull(message = "size not be null")
           Integer size,
           String sort,
           String source
) { }



