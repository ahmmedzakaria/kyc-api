package com.example.kyc.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Standard API response wrapper")
@Data
@Builder
public class ApiResponse<T> {
	
    @Schema(example = "Http Status like 200,401,403,404,405,412 etc.", description = "Http Status Code.")
    private int status;
    @Schema( description = "Success or error messages.", example = "This properties will contains all the message like success or error.")
    private List<String> message;
    @Schema(description = "The data properties is dynamically build, based on the request it can be Object it can Be array.")
    private T data;
    @Schema( description = "Server response time.", example = "2025-05-31T11:55:28.0076812")
    private LocalDateTime timestamp;

    public static <T> ApiResponse<T> body(List<String> message, T data) {
        return ApiResponse.<T>builder()
                .status(200)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    public static <T> ApiResponse<T> body(List<String> message, T data, int status) {
        return ApiResponse.<T>builder()
                .status(status)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    public static <T> ApiResponse<T> body(List<String> message, int status) {
        return ApiResponse.<T>builder()
                .status(status)
                .message(message)
                .data(null)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
