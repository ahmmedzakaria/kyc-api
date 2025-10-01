package com.example.kyc.validation;

import java.time.LocalDateTime;

import com.example.kyc.dto.*;
import com.example.kyc.entity.User;
import com.example.kyc.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;



@Service
public class AuthenticationValidatiorService {

	@Autowired
	JwtUtil jwtUtil;

	public AuthValidationReturn validateAuthentication(User user, LoginRequestWithOTP requestDto) {
		AuthValidationReturn validationReturn = new AuthValidationReturn();

		// If OTP is present → validate it
		if (user.getOtpCode() == null) {
			validationReturn.setValid(false);
			validationReturn.setMessage("No OTP found for given user!");
			return validationReturn;
		}

//		if (user.getOtpExpiresAt().isBefore(LocalDateTime.now())) {
//			validationReturn.setValid(false);
//			validationReturn.setMessage("OTP time is expired!");
//			return validationReturn;
//		}

		if (!requestDto.otp().equals(user.getOtpCode())) {
			validationReturn.setValid(false);
			validationReturn.setMessage("Given OTP does not match!");
			return validationReturn;
		}
		
		// Common logic for all auth types
		UserModelResp userVm = new UserModelResp(user.getName(), user.getEmail(), user.isActive());

		AuthResponse tokenBody = new AuthResponse(
									//jwtUtil.generateAccessToken(user, requestDto.source()),
									jwtUtil.generateTokenWithSubject(user.getUsername(), 3600000),
									jwtUtil.generateRefreshToken(user, requestDto.source()),
									user.getRole().getName(),
									userVm
									);

		validationReturn.setValid(true);
		validationReturn.setMessage("Authentication successful. Token generated.");
		validationReturn.setData(tokenBody);

		return validationReturn;
	}

	public AuthValidationReturn validaterefreshToken(User user, RefreshTokenRequest requestDto) {
		AuthValidationReturn validationReturn = new AuthValidationReturn();

		String source = jwtUtil.extractSource(requestDto.refreshToken());
		// Generate Jwt token
		AuthResponse tokenBody = new AuthResponse(jwtUtil.generateAccessToken(user, source),
													jwtUtil.generateRefreshToken(user, source), user.getRole().getName());
		validationReturn.setValid(true);
		validationReturn.setMessage("OTP is valid and access token is generated.");
		validationReturn.setData(tokenBody);

		return validationReturn;
	}

}
