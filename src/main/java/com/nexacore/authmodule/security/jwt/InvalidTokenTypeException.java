package com.nexacore.authmodule.security.jwt;

import io.jsonwebtoken.JwtException;

public class InvalidTokenTypeException extends JwtException {
    public InvalidTokenTypeException(String message) {
        super(message);
    }
}
