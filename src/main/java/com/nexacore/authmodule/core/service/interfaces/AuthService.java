package com.nexacore.authmodule.core.service.interfaces;

import com.nexacore.authmodule.core.dto.ApplicationContextDto;
import com.nexacore.authmodule.core.dto.AuthRequest;
import com.nexacore.authmodule.core.dto.AuthResponse;
import com.nexacore.authmodule.core.dto.AuthConfigResponse;
import com.nexacore.authmodule.core.dto.LoginStatusRequest;
import com.nexacore.authmodule.core.dto.LoginStatusResponse;
import com.nexacore.authmodule.core.dto.RefreshTokenRequest;
import com.nexacore.commonmodule.dto.ApiResponse;
import org.springframework.http.ResponseEntity;

public interface AuthService {
    public ResponseEntity<ApiResponse<AuthResponse>> authenticate(AuthRequest request, String clientCode);
    public ResponseEntity<ApiResponse<AuthConfigResponse>> getAuthConfig(String origin, String clientCode);
    public ResponseEntity<ApiResponse<ApplicationContextDto>> getPublicApplicationContext(String origin, String clientCode);
    public ResponseEntity<ApiResponse<ApplicationContextDto>> getApplicationContext(String origin, String clientCode);
    public ResponseEntity<ApiResponse<Void>> logout(String username);
    public ResponseEntity<ApiResponse<LoginStatusResponse>> loginStatus(LoginStatusRequest request, String clientCode);
    public ResponseEntity<ApiResponse<Void>> sessionStatus();
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(RefreshTokenRequest requestDto);

}
