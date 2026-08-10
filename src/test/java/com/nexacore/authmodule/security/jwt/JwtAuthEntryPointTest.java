package com.nexacore.authmodule.security.jwt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexacore.commonmodule.web.ApiResponseJsonWriter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.InsufficientAuthenticationException;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthEntryPointTest {

    @Test
    void returnsSpecificInactiveSessionMessage() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        JwtAuthEntryPoint entryPoint = new JwtAuthEntryPoint(new ApiResponseJsonWriter(objectMapper));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("jwt_error_code", "AUTHENTICATION_REQUIRED");
        request.setAttribute("jwt_error_message", "User session is not active. Please log in again.");
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new InsufficientAuthenticationException("Authentication required"));

        JsonNode body = objectMapper.readTree(response.getContentAsByteArray());
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(body.path("statusCode").asInt()).isEqualTo(401);
        assertThat(body.path("message").get(0).path("code").asText()).isEqualTo("AUTHENTICATION_REQUIRED");
        assertThat(body.path("message").get(0).path("message").asText())
                .isEqualTo("User session is not active. Please log in again.");
    }
}
