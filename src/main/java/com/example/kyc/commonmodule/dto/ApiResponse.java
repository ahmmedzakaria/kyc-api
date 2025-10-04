package com.example.kyc.commonmodule.dto;

import com.example.kyc.commonmodule.constants.MessageType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import org.geolatte.geom.M;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Schema(description = "Standard API response wrapper")
@Data
public class ApiResponse<T> {

    @Schema(example = "Http Status like 200,401,403,404,405,412 etc.", description = "Http Status Code.")
    private String status;
    private int statusCode;
    @Schema( description = "Success or error messages.", example = "This properties will contains all the message like success or error.")
    private List<ResponseMessage> message = new ArrayList<>();
    @Schema(description = "The data properties is dynamically build, based on the request it can be Object it can Be array.")
    private T data;
//    @Schema( description = "Server response time.", example = "2025-05-31T11:55:28.0076812")
//    private LocalDateTime timestamp = LocalDateTime.now();

    public static <T> ApiResponse<T> success(T data, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setStatusCode(200);
        response.setStatus("SUCCESS");
        response.message.add(new ResponseMessage(MessageType.SUCCESS, message));
        response.setData(data);

        return response;
    }

    public static <T> ApiResponse<T> success( String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setStatusCode(200);
        response.setStatus("SUCCESS");
        response.message.add(new ResponseMessage(MessageType.SUCCESS, message));
        response.setData(null);

        return response;
    }

    public static <T> ApiResponse<T> success(T data, List<String> message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setStatusCode(200);
        response.setStatus("SUCCESS");
        response.message.addAll(buildResponseMessage(message, MessageType.SUCCESS));
        response.setData(data);

        return response;
    }


    public static <T> ApiResponse<T> error(T data, int status, List<String> message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setStatusCode(500);
        response.setStatus("ERROR");
        response.message.addAll(buildResponseMessage(message, MessageType.ERROR));
        response.setData(data);

        return response;
    }
    
    public static <T> ApiResponse<T> error(int status,List<String> message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setStatusCode(status);
        response.setStatus("ERROR");
        response.message.addAll(buildResponseMessage(message,MessageType.ERROR));
        response.setData(null);

        return response;
    }

    public static <T> ApiResponse<T> error(int status, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setStatusCode(status);
        response.setStatus("ERROR");
        response.message.add(new ResponseMessage(MessageType.SUCCESS, message));
        response.setData(null);

        return response;
    }

    public static List<ResponseMessage> buildResponseMessage( List<String> message,MessageType messageType) {
        return message.stream().map(m->new ResponseMessage(messageType, m)).toList();
    }
}
