package com.example.kyc.kycmodule.dto.documentation;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class KycCreateRequest {

    @Schema(example = "John")
    private String firstName;

    @Schema(example = "john@example.com")
    private String email;

    @Schema(example = "1234567890")
    private String phone;

    @Schema(type = "string", format = "binary")
    private MultipartFile photo;
}
