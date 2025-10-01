package com.example.kyc.dto;

import org.springframework.http.HttpStatus;

import java.util.Arrays;
import java.util.List;

public class ApiError {

    private HttpStatus status;
    private List<String> message;

    public ApiError(HttpStatus status, List<String> message) {
        this.status = status;
        this.message = message;
    }

    public ApiError(HttpStatus status, String message) {
        this.status = status;
        this.message = Arrays.asList(message);
    }

	public HttpStatus getStatus() {
		return status;
	}

	public void setStatus(HttpStatus status) {
		this.status = status;
	}

	public List<String> getMessage() {
		return message;
	}

	public void setMessage(List<String> message) {
		this.message = message;
	}
}