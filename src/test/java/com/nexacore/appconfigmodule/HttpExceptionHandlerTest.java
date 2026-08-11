package com.nexacore.appconfigmodule;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class HttpExceptionHandlerTest {

    @Test
    void mapsInvalidRequestArgumentsToBadRequestInsteadOfAuthenticationFailure() {
        var handler = new HttpExceptionHandler(null);

        var response = handler.handleIllegalArgument(
                new IllegalArgumentException("Global person first name is required"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatusCode()).isEqualTo(400);
        assertThat(response.getBody().getMessage()).singleElement().satisfies(message -> {
            assertThat(message.getCode()).isEqualTo("INVALID_REQUEST");
            assertThat(message.getMessage()).isEqualTo("Global person first name is required");
        });
    }
}
