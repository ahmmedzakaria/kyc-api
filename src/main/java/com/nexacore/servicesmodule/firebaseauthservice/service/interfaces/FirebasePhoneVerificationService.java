package com.nexacore.servicesmodule.firebaseauthservice.service.interfaces;

import com.nexacore.servicesmodule.firebaseauthservice.dto.FirebasePhoneVerificationRequest;
import com.nexacore.servicesmodule.firebaseauthservice.dto.FirebasePhoneVerificationResponse;

public interface FirebasePhoneVerificationService {

    FirebasePhoneVerificationResponse verifyPhoneToken(String idToken);

    FirebasePhoneVerificationResponse verifyPhoneToken(FirebasePhoneVerificationRequest request);
}

