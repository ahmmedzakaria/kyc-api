package com.example.kyc.authmodule.service.interfaces;

import com.example.kyc.authmodule.dto.AuthRequest;
import com.example.kyc.authmodule.dto.AuthResponse;
import com.example.kyc.authmodule.dto.RefreshTokenRequest;
import com.example.kyc.commonmodule.dto.ApiResponse;
import org.springframework.http.ResponseEntity;

public interface AuthService {
    public ResponseEntity<ApiResponse<AuthResponse>> authenticate(AuthRequest request);
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(RefreshTokenRequest requestDto);

}
