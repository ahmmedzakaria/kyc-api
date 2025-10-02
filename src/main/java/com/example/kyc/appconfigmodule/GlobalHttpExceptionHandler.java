package com.example.kyc.appconfigmodule;

import com.example.kyc.commonmodule.dto.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ControllerAdvice
public class GlobalHttpExceptionHandler {

    // Handle validation errors (400 Bad Request)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ApiResponse<?>> handleValidationExceptions(MethodArgumentNotValidException ex) {
    	
    	List<String> msg = new ArrayList<>();
        Map<String, String> errors = new HashMap<>();
        
        ex.getBindingResult().getFieldErrors().forEach(error -> {
		            String fieldName = error.getField();
		            String errorMessage = error.getDefaultMessage();
		            errors.put(fieldName, errorMessage);
		            msg.add("Bad request for the field: "+ fieldName +", Msg: "+ errorMessage);
        });
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.body(msg, errors, HttpStatus.BAD_REQUEST.value()));
    }

    // Handle method not allowed (405)
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ResponseEntity<ApiResponse<?>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
    	List<String> msg = new ArrayList<>();
    	msg.add("Method Not Allowed");
    	msg.add(ex.getMessage());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(ApiResponse.body(msg, HttpStatus.METHOD_NOT_ALLOWED.value()));
    }

    // Handle type mismatch (400 Bad Request)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ApiResponse<?>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
    	
    	List<String> msg = new ArrayList<>();
    	msg.add("Method Not Allowed");
    	msg.add(String.format("Parameter '%s' has invalid value '%s'", ex.getName(), ex.getValue()));
    	
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.body(msg, HttpStatus.BAD_REQUEST.value()));
    }

    // Handle constraint violations (400 Bad Request)
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ApiResponse<?>> handleConstraintViolation(ConstraintViolationException ex) {
        
    	List<String> msg = new ArrayList<>();
    	msg.add(ex.getMessage());
    	
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.body(msg, HttpStatus.BAD_REQUEST.value()));
    }

    // Handle 404 Not Found
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<ApiResponse<?>> handleNotFound(NoHandlerFoundException ex) {
    	
    	List<String> msg = new ArrayList<>();
    	msg.add("Not Found");
    	msg.add("The requested URL was not found on this server");
    	
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.body(msg, HttpStatus.NOT_FOUND.value()));
    }
}
