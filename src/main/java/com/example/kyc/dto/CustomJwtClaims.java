package com.example.kyc.dto;

import lombok.Data;

@Data
public class CustomJwtClaims {
	private String sub;
	//private String roles;
	private int exp;
	private String source;
	private int iat;
}
