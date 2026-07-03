package com.nexacore.commonmodule.dto;

import com.nexacore.commonmodule.constants.MessageType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    @Test
    void successCodeAddsCodeAndLocalizedMessage() {
        ApiResponse<String> response = ApiResponse.successCode("data", "common.success", "Success");

        assertThat(response.getStatus()).isEqualTo("SUCCESS");
        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.getData()).isEqualTo("data");
        assertThat(response.getMessage()).hasSize(1);
        assertThat(response.getMessage().getFirst().getType()).isEqualTo(MessageType.SUCCESS);
        assertThat(response.getMessage().getFirst().getCode()).isEqualTo("common.success");
        assertThat(response.getMessage().getFirst().getMessage()).isEqualTo("Success");
    }

    @Test
    void errorCodeAddsCodeAndLocalizedMessage() {
        ApiResponse<Object> response = ApiResponse.errorCode(400, "common.error.validation", "Validation failed");

        assertThat(response.getStatus()).isEqualTo("ERROR");
        assertThat(response.getStatusCode()).isEqualTo(400);
        assertThat(response.getMessage()).hasSize(1);
        assertThat(response.getMessage().getFirst().getType()).isEqualTo(MessageType.ERROR);
        assertThat(response.getMessage().getFirst().getCode()).isEqualTo("common.error.validation");
        assertThat(response.getMessage().getFirst().getMessage()).isEqualTo("Validation failed");
    }

    @Test
    void legacyErrorUsesErrorMessageType() {
        ApiResponse<Object> response = ApiResponse.error(400, "Bad request");

        assertThat(response.getMessage().getFirst().getType()).isEqualTo(MessageType.ERROR);
    }
}
