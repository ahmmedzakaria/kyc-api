package com.nexacore.systemmodule.backup.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BackupApiException extends RuntimeException {
    private final HttpStatus status;
    private final String code;

    public BackupApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }
}
