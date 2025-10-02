package com.example.kyc.authmodule.service.implementations;


import com.example.kyc.authmodule.dto.AuthRequest;
import com.example.kyc.authmodule.dto.AuthResponse;
import com.example.kyc.authmodule.dto.RefreshTokenRequest;
import com.example.kyc.authmodule.service.interfaces.AuthService;
import com.example.kyc.commonmodule.dto.ApiResponse;
import com.example.kyc.configmodule.jwt.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

	private final AuthenticationManager authenticationManager;
	private final UserDetailsService userDetailsService;
	private final JwtUtil jwtUtil;

	@Override
	public ResponseEntity<ApiResponse<AuthResponse>> authenticate(AuthRequest request) {
		try {
			Authentication authentication = authenticationManager.authenticate(
					new UsernamePasswordAuthenticationToken(request.username(), request.password())
			);

			if(authentication.isAuthenticated()){
				var userDetails = userDetailsService.loadUserByUsername(request.username());

				String accessToken = jwtUtil.generateToken(userDetails);
				String refreshToken = jwtUtil.generateRefreshToken(userDetails);

				AuthResponse response =  AuthResponse.builder()
						.accessToken(accessToken)
						.refreshToken(refreshToken)
						.build();

				return ResponseEntity.ok(ApiResponse.body("Authentication successful. Token generated", response));

			}else {
				return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).body(ApiResponse.body("User authentication Failed", HttpStatus.PRECONDITION_FAILED.value()));
			}

		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.body(List.of("Exception occurred: " + e.getLocalizedMessage()),HttpStatus.INTERNAL_SERVER_ERROR.value()));
		}
	}

	@Override
	public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@Valid RefreshTokenRequest requestDto) {
		try {
			String username = jwtUtil.extractUsername(requestDto.refreshToken());

			var userDetails = userDetailsService.loadUserByUsername(username);

			if (jwtUtil.validateToken(requestDto.refreshToken(), userDetails)) {
				String newAccessToken = jwtUtil.generateToken(userDetails);
				String newRefreshToken = jwtUtil.generateRefreshToken(userDetails);

				AuthResponse response =  AuthResponse.builder()
						.accessToken(newAccessToken)
						.refreshToken(newRefreshToken)
						.build();

				return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.body("access token is generated", response));

			} else {
				return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).body(ApiResponse.body("No such user is exist!", HttpStatus.PRECONDITION_FAILED.value()));
			}

		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(ApiResponse.body(Arrays.asList("Exception occurs: " + e.getLocalizedMessage()),HttpStatus.INTERNAL_SERVER_ERROR.value()));
		}
	}

}
