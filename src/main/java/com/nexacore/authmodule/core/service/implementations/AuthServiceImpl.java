package com.nexacore.authmodule.core.service.implementations;


import com.nexacore.authmodule.core.dto.ApplicationContextDto;
import com.nexacore.authmodule.core.enums.LoginMethod;
import com.nexacore.authmodule.security.config.AuthenticationProperties;
import com.nexacore.authmodule.security.config.KeycloakProperties;
import com.nexacore.authmodule.core.dto.AuthConfigResponse;
import com.nexacore.authmodule.core.dto.AuthRequest;
import com.nexacore.authmodule.core.dto.AuthResponse;
import com.nexacore.authmodule.core.dto.LoginStatusRequest;
import com.nexacore.authmodule.core.dto.LoginStatusResponse;
import com.nexacore.authmodule.core.dto.RefreshTokenRequest;
import com.nexacore.authmodule.core.service.interfaces.AuthService;
import com.nexacore.commonmodule.dto.ApiResponse;
import com.nexacore.authmodule.security.jwt.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

	private final AuthenticationManager authenticationManager;
	private final UserDetailsService userDetailsService;
	private final JwtUtil jwtUtil;
	private final AuthenticationProperties authenticationProperties;
	private final KeycloakProperties keycloakProperties;
	private final LogoutSessionService logoutSessionService;
	private final AuthApplicationContextService authApplicationContextService;
	private final AuthClientPolicyService authClientPolicyService;

	@Override
	public ResponseEntity<ApiResponse<AuthResponse>> authenticate(AuthRequest request, String clientCode) {
		if (!authClientPolicyService.isLoginMethodEnabled(LoginMethod.PASSWORD, clientCode)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN)
					.body(ApiResponse.error(HttpStatus.FORBIDDEN.value(), "LOCAL_LOGIN_DISABLED"));
		}

		try {
			Authentication authentication = authenticationManager.authenticate(
					new UsernamePasswordAuthenticationToken(request.username(), request.password())
			);

			if(authentication.isAuthenticated()){
				var userDetails = userDetailsService.loadUserByUsername(request.username());

				logoutSessionService.login(userDetails.getUsername());
				String accessToken = jwtUtil.generateToken(userDetails);
				String refreshToken = jwtUtil.generateRefreshToken(userDetails);

				AuthResponse response =  AuthResponse.builder()
						.accessToken(accessToken)
						.refreshToken(refreshToken)
						.build();

				return ResponseEntity.ok(ApiResponse.success(response, "Authentication successful. Token generated"));

			}else {
				return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).body(ApiResponse.error(HttpStatus.PRECONDITION_FAILED.value(),"User authentication Failed"));
			}

		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(),List.of("Exception occurred: " + e.getLocalizedMessage())));
		}
	}

	@Override
	public ResponseEntity<ApiResponse<AuthConfigResponse>> getAuthConfig(String origin, String clientCode) {
		String redirectUri = authApplicationContextService.buildRedirectUri(origin);
		ApplicationContextDto applicationContext = authApplicationContextService.buildPublicContext(origin, clientCode);
		AuthConfigResponse response = AuthConfigResponse.builder()
				.registrationMode(applicationContext.getRegistrationMode())
				.enabledRegistrationCredentialModels(applicationContext.getEnabledRegistrationCredentialModels())
				.enabledLoginMethods(applicationContext.getEnabledLoginMethods())
				.loginIdentifierTypes(applicationContext.getLoginIdentifierTypes())
				.userActivationMode(applicationContext.getUserActivationMode())
				.issuerUri(keycloakProperties.getIssuerUri())
				.clientId(authApplicationContextService.resolveClientId(origin))
				.redirectUri(redirectUri)
				.sso(applicationContext.getSso())
				.registration(applicationContext.getRegistration())
				.securityPolicy(applicationContext.getSecurityPolicy())
				.applicationContext(applicationContext)
				.build();

		return ResponseEntity.ok(ApiResponse.success(response, "Authentication config loaded"));
	}

	@Override
	public ResponseEntity<ApiResponse<ApplicationContextDto>> getPublicApplicationContext(String origin, String clientCode) {
		return ResponseEntity.ok(ApiResponse.success(
				authApplicationContextService.buildPublicContext(origin, clientCode),
				"Public application context loaded"
		));
	}

	@Override
	public ResponseEntity<ApiResponse<ApplicationContextDto>> getApplicationContext(String origin, String clientCode) {
		return ResponseEntity.ok(ApiResponse.success(
				authApplicationContextService.buildPublicContext(origin, clientCode),
				"Application context loaded"
		));
	}

	@Override
	public ResponseEntity<ApiResponse<Void>> logout(String username) {
		if (StringUtils.hasText(username)) {
			logoutSessionService.logout(username);
			log.info("User logged out from shared session: {}", username);
		}
		return ResponseEntity.ok(ApiResponse.<Void>success("Logout successful"));
	}

	@Override
	public ResponseEntity<ApiResponse<LoginStatusResponse>> loginStatus(LoginStatusRequest request) {
		boolean loggedIn = request != null
				&& StringUtils.hasText(request.username())
				&& logoutSessionService.isLoggedIn(request.username());

		return ResponseEntity.ok(ApiResponse.success(
				new LoginStatusResponse(loggedIn),
				"Login status loaded"
		));
	}

	@Override
	public ResponseEntity<ApiResponse<Void>> sessionStatus() {
		return ResponseEntity.ok(ApiResponse.<Void>success("Session active"));
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

				return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(response,"access token is generated"));

			} else {
				return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).body(ApiResponse.error(HttpStatus.PRECONDITION_FAILED.value(),"No such user is exist!"));
			}

		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), List.of("Exception occurs: " + e.getLocalizedMessage())));
		}
	}

}
