package com.nexacore.authmodule.service.interfaces;

import com.nexacore.authmodule.dto.AuthRequest;
import com.nexacore.authmodule.dto.AuthResponse;
import com.nexacore.authmodule.dto.AuthConfigResponse;
import com.nexacore.authmodule.dto.LoginStatusRequest;
import com.nexacore.authmodule.dto.LoginStatusResponse;
import com.nexacore.authmodule.dto.RefreshTokenRequest;
import com.nexacore.commonmodule.dto.ApiResponse;
import org.springframework.http.ResponseEntity;

public interface AuthService {
    public ResponseEntity<ApiResponse<AuthResponse>> authenticate(AuthRequest request);
    public ResponseEntity<ApiResponse<AuthConfigResponse>> getAuthConfig(String origin);
    public ResponseEntity<ApiResponse<Void>> logout(String username);
    public ResponseEntity<ApiResponse<LoginStatusResponse>> loginStatus(LoginStatusRequest request);
    public ResponseEntity<ApiResponse<Void>> sessionStatus();
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(RefreshTokenRequest requestDto);

}
