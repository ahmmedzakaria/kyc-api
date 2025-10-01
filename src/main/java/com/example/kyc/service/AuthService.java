package com.example.kyc.service;


import com.example.kyc.dto.*;
import com.example.kyc.entity.Role;
import com.example.kyc.entity.User;
import com.example.kyc.repository.RoleRepository;
import com.example.kyc.repository.UserRepository;
import com.example.kyc.security.JwtUtil;
import com.example.kyc.util.SmsHelper;
import com.example.kyc.util.Utility;
import com.example.kyc.validation.AuthenticationValidatiorService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
public class AuthService {

	@Autowired
	UserRepository userRepository;

	@Autowired
	RoleRepository roleRepository;

//	@Autowired
//	EmailService emailService;

	@Autowired
	JwtUtil jwtUtil;
	
//	@Autowired
//	SmsHelper smsHelper;

	@Autowired
	AuthenticationValidatiorService authValidator;

	public ResponseEntity<ApiResponse<?>> requestForOTP(@Valid RequestOTPDto requestDto) {
		try {
			Optional<User> optUser = userRepository.findByMobile(requestDto.mobile());
			User userToSave;

			String otp = Utility.generateOtp();
			LocalDateTime expires = LocalDateTime.now().plusMinutes(5);

			if (optUser.isPresent()) {
				userToSave = optUser.get();
				userToSave.setOtpCode(otp);
				userToSave.setOtpExpiresAt(expires);

				userRepository.save(userToSave);
				//smsHelper.sendSms(userToSave.getMobile(), otp);
				
				return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.body(List.of("OTP is being sent"),"OTP is sent. Please check your inbox! : " + otp));
			} else {
				userToSave = new User();
				userToSave.setMobile(requestDto.mobile());
				userToSave.setName("NA");
				userToSave.setEmail("NA");
				userToSave.setMobileVerified(true);
				userToSave.setOtpCode(otp);
				userToSave.setOtpExpiresAt(expires);
				userToSave.setActive(true);
				Role role = roleRepository.findByName("ROLE_STUDENT")
				        .orElseGet(() -> {
				            Role newRole = new Role();
				            newRole.setName("ROLE_STUDENT");
				            return roleRepository.save(newRole);
				        });
				
				userToSave.setRole(role);
                
				userRepository.save(userToSave);
				//smsHelper.sendSms(userToSave.getMobile(), otp);
				return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.body(List.of("OTP is being sent"),"OTP is sent. Please check your inbox! : " + otp));

				//return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).body(ApiResponse.body(List.of("No such user exists as: " + requestDto.mobile()), HttpStatus.PRECONDITION_FAILED.value()));
			}

		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.body(List.of("Exception occurs: " + e.getLocalizedMessage()),HttpStatus.INTERNAL_SERVER_ERROR.value()));
		}
	}

	public ResponseEntity<ApiResponse<AuthResponse>> authenticate(LoginRequestWithOTP requestDto) {
		try {
			User user = null;

			Optional<User> optUser = userRepository.findByMobile(requestDto.mobile());
			if (!optUser.isPresent()) {
				return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).body(ApiResponse.body(List.of("No such user exists as: " + requestDto.mobile()), HttpStatus.PRECONDITION_FAILED.value()));
			}
			user = optUser.get();

			AuthValidationReturn validationReturn = authValidator.validateAuthentication(user, requestDto);

			if (validationReturn.isValid()) {
				//user.setOtpCode(null);
				user.setOtpExpiresAt(null);
				userRepository.save(user);

				return ResponseEntity.ok(ApiResponse.body(validationReturn.getMessages(), validationReturn.getData()));
			} else {
				return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).body(ApiResponse.body(validationReturn.getMessages(), HttpStatus.PRECONDITION_FAILED.value()));
			}

		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.body(List.of("Exception occurred: " + e.getLocalizedMessage()),HttpStatus.INTERNAL_SERVER_ERROR.value()));
		}
	}

	public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@Valid RefreshTokenRequest requestDto) {
		try {
			String email = jwtUtil.extractUsername(requestDto.refreshToken());

			Optional<User> optUser = userRepository.findByEmail(email);
			if (optUser.isPresent()) {
				User user = optUser.get();

				AuthValidationReturn validationReturn = authValidator.validaterefreshToken(user, requestDto);

				if (validationReturn.isValid()) {
					return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.body(validationReturn.getMessages(), validationReturn.getData()));
				} else {
					return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).body(ApiResponse.body(validationReturn.getMessages(), HttpStatus.PRECONDITION_FAILED.value()));
				}
			} else {
				return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).body(ApiResponse.body(Arrays.asList("No such user is exist!"), HttpStatus.PRECONDITION_FAILED.value()));
			}
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(ApiResponse.body(Arrays.asList("Exception occurs: " + e.getLocalizedMessage()),HttpStatus.INTERNAL_SERVER_ERROR.value()));
		}
	}

}
