package com.nexacore.servicesmodule.messagingservice.service.interfaces;

import com.nexacore.servicesmodule.messagingservice.dto.MobileMessageRequest;
import com.nexacore.servicesmodule.messagingservice.dto.MobileMessageResponse;

import java.time.Duration;

public interface MobileMessagingService {

    MobileMessageResponse sendMessage(MobileMessageRequest request);

    MobileMessageResponse sendOtp(String mobileNumber, String otp, Duration validity);

    MobileMessageResponse sendTransactionInfo(String mobileNumber, String transactionReference, String message);
}

