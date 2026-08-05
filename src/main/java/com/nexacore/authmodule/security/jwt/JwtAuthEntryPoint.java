package com.nexacore.authmodule.security.jwt;

import com.nexacore.commonmodule.web.ApiResponseJsonWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthEntryPoint implements AuthenticationEntryPoint {

	private static final String AUTHENTICATION_REQUIRED = "AUTHENTICATION_REQUIRED";
	private static final String INVALID_TOKEN_TYPE = "INVALID_TOKEN_TYPE";

	private final ApiResponseJsonWriter responseWriter;

	@Override
	public void commence(HttpServletRequest request,
						 HttpServletResponse response,
						 AuthenticationException authException) throws IOException {
		String errorCode = (String) request.getAttribute("jwt_error_code");
		String code = INVALID_TOKEN_TYPE.equals(errorCode) ? INVALID_TOKEN_TYPE : AUTHENTICATION_REQUIRED;
		String message = INVALID_TOKEN_TYPE.equals(code)
				? "The supplied token type cannot access this resource"
				: "A valid user access token is required";
		responseWriter.writeError(response, HttpServletResponse.SC_UNAUTHORIZED, code, message);
	}


/*
In Spring Security, an AuthenticationEntryPoint is what handles unauthenticated requests to secured endpoints.

When a request comes in without a valid JWT/token (or invalid credentials), Spring Security invokes the configured AuthenticationEntryPoint.

By default, Spring might redirect to a login page (not useful for APIs).

For a REST API, we usually override it to return a 401 Unauthorized JSON response.
* */


//	@Override
//	public void commence(HttpServletRequest request, HttpServletResponse response,
//			AuthenticationException authException) throws IOException, ServletException {
//
//		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
//		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
//
//		Exception exception = (Exception) request.getAttribute("exception");
//		String message;
//		Map<String, Object> map = new HashMap<String, Object>();
//
//
//		if (exception != null) {
//			map.put("data", null);
//			map.put("status", HttpStatus.UNAUTHORIZED.value());
//			map.put("message", Arrays.asList("Cause: "+ exception , "Token value expired, request to refresh token using given token"));
//			map.put("timestamp", new Timestamp(System.currentTimeMillis()));
//
//			byte[] body = new ObjectMapper().writeValueAsBytes(map);
//			response.getOutputStream().write(body);
//		} else {
//
//			if (authException.getCause() != null) {
//				message = authException.getCause().toString() + " " + authException.getMessage();
//			}else {
//				message = authException.getMessage();
//			}
//			map.put("data", null);
//			map.put("status", HttpStatus.UNAUTHORIZED.value());
//			map.put("message", Arrays.asList("Cause: "+message , "Sorry, Token not found to authenticate."));
//			map.put("timestamp", new Timestamp(System.currentTimeMillis()));
//
//			byte[] body = new ObjectMapper().writeValueAsBytes(map);
//			response.getOutputStream().write(body);
//		}
//	}

}
