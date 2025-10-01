package com.example.kyc.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Component
public class CustomJwtAuthEntryPoint implements AuthenticationEntryPoint {

	//private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationEntryException.class);
	
	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authException) throws IOException, ServletException {
		
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);

		Exception exception = (Exception) request.getAttribute("exception");
		String message;
		Map<String, Object> map = new HashMap<String, Object>();
		
		//String bearerToken = request.getHeader("Authorization");
		//String contentType = request.getContentType();
		
		if (exception != null) {
			map.put("data", null);
			map.put("status", HttpStatus.UNAUTHORIZED.value());
			map.put("message", Arrays.asList("Cause: "+ exception , "Token value expired, request to refresh token using given token"));
			map.put("timestamp", new Timestamp(System.currentTimeMillis()));

			byte[] body = new ObjectMapper().writeValueAsBytes(map);
			response.getOutputStream().write(body);
		} else {
			
			if (authException.getCause() != null) {
				message = authException.getCause().toString() + " " + authException.getMessage();
			}else {
				message = authException.getMessage();
			}
			map.put("data", null);
			map.put("status", HttpStatus.UNAUTHORIZED.value());
			map.put("message", Arrays.asList("Cause: "+message , "Sorry, Token not found to authenticate."));
			map.put("timestamp", new Timestamp(System.currentTimeMillis()));

			byte[] body = new ObjectMapper().writeValueAsBytes(map);
			response.getOutputStream().write(body);
		}
	}

}