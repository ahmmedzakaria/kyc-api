package com.example.kyc.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Access Token")
public class AuthResponse {
    @Schema( description = "JWT access token.", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWUsImlhdCI6MTUxNjIzOTAyMn0.KMUFsIDTnFmyG3nMiGM6H9FNFUROf3wh7SmqJp-QV30")
    private String accessToken;
    
    @Schema( description = "JWT refresh token.", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWUsImlhdCI6MTUxNjIzOTAyMn0.KMUFsIDTnFmyG3nMiGM6H9FNFUROf3wh7SmqJp-QV30")
    private String refreshToken;
    
    @Schema( description = "Users permission scope", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWUsImlhdCI6MTUxNjIzOTAyMn0.KMUFsIDTnFmyG3nMiGM6H9FNFUROf3wh7SmqJp-QV30")
    private String scope;
    
    @Schema( description = "Users basic information", example = "Users name, username, email, mobile no etc.")
    private UserModelResp user;

	public AuthResponse(String accessToken, String refreshToken, String scope, UserModelResp user) {
		this.accessToken = accessToken;
		this.refreshToken = refreshToken;
		this.scope = scope;
		this.user = user;
	}
    
	public AuthResponse(String accessToken, String refreshToken, String scope) {
		this.accessToken = accessToken;
		this.refreshToken = refreshToken;
		this.scope = scope;
	}
    
}