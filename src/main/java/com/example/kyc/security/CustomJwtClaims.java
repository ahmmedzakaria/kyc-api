package com.example.kyc.security;

import lombok.Data;

@Data
public class CustomJwtClaims {
	private String sub;
	//private String roles;
	private int exp;
	private String source;
	private int iat;
}
