package com.example.kyc.dto;

public class AuthValidationReturn extends CommonValidationReturn {
	private AuthResponse data;

	public AuthResponse getData() {
		return data;
	}
	public void setData(AuthResponse data) {
		this.data = data;
	}

}
