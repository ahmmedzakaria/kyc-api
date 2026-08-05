package com.nexacore.commonmodule.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexacore.commonmodule.dto.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class ApiResponseJsonWriter {

    private final ObjectMapper objectMapper;

    public void writeError(HttpServletResponse response, int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(), ApiResponse.errorCode(status, code, message));
    }
}
