package com.nexacore.appconfigmodule;

import com.nexacore.commonmodule.dto.ApiResponse;
import com.nexacore.commonmodule.i18n.service.interfaces.MessageLocalizationService;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import com.nexacore.systemmodule.accesscontrol.security.DataScopeAccessDeniedException;
import com.nexacore.systemmodule.accesscontrol.security.AccessControlError;
import com.nexacore.systemmodule.backup.exception.BackupApiException;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
@RequiredArgsConstructor
public class HttpExceptionHandler {

    private final MessageLocalizationService localizationService;

    @ExceptionHandler(BackupApiException.class)
    public ResponseEntity<ApiResponse<?>> handleBackupApiException(BackupApiException ex) {
        return ResponseEntity.status(ex.getStatus()).body(ApiResponse.errorCode(
                ex.getStatus().value(), ex.getCode(), ex.getMessage()
        ));
    }

    @ExceptionHandler(DataScopeAccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ResponseEntity<ApiResponse<?>> handleDataScopeDenied(DataScopeAccessDeniedException ex) {
        AccessControlError error = AccessControlError.DATA_SCOPE_NOT_ALLOWED;
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.errorCode(
                error.getStatus(), error.name(), error.getMessage()
        ));
    }

    // Handle validation errors (400 Bad Request)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ApiResponse<?>> handleValidationExceptions(MethodArgumentNotValidException ex) {
    	
        Map<String, String> errors = new HashMap<>();
        
        ex.getBindingResult().getFieldErrors().forEach(error -> {
		            String fieldName = error.getField();
		            String errorMessage = error.getDefaultMessage();
		            errors.put(fieldName, errorMessage);
        });
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.errorCode(
                errors,
                HttpStatus.BAD_REQUEST.value(),
                "common.error.validation",
                localizationService.getMessage("common.error.validation")
        ));
    }

    // Handle method not allowed (405)
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ResponseEntity<ApiResponse<?>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(ApiResponse.errorCode(
                HttpStatus.METHOD_NOT_ALLOWED.value(),
                "common.error.method_not_allowed",
                localizationService.getMessage("common.error.method_not_allowed")
        ));
    }

    // Handle type mismatch (400 Bad Request)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ApiResponse<?>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.errorCode(
                HttpStatus.BAD_REQUEST.value(),
                "common.error.type_mismatch",
                localizationService.getMessage("common.error.type_mismatch", ex.getName(), ex.getValue())
        ));
    }

    // Handle constraint violations (400 Bad Request)
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ApiResponse<?>> handleConstraintViolation(ConstraintViolationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.errorCode(
                HttpStatus.BAD_REQUEST.value(),
                "common.error.validation",
                localizationService.getMessage("common.error.validation")
        ));
    }

    // Handle 404 Not Found
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<ApiResponse<?>> handleNotFound(NoHandlerFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.errorCode(
                HttpStatus.NOT_FOUND.value(),
                "common.error.not_found",
                localizationService.getMessage("common.error.not_found")
        ));
    }
}
