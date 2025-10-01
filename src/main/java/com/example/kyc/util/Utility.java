package com.example.kyc.util;

public class Utility {
	public static String generateOtp() {
        return String.valueOf((int) (Math.random() * 9000) + 1000); // 1000–9999
    }
}
