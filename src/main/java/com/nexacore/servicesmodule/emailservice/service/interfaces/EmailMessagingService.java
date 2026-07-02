package com.nexacore.servicesmodule.emailservice.service.interfaces;

import com.nexacore.servicesmodule.emailservice.dto.EmailMessageRequest;
import com.nexacore.servicesmodule.emailservice.dto.EmailMessageResponse;

import java.time.Duration;

public interface EmailMessagingService {

    EmailMessageResponse sendEmail(EmailMessageRequest request);

    EmailMessageResponse sendOtp(String emailAddress, String otp, Duration validity);

    EmailMessageResponse sendTransactionInfo(String emailAddress, String transactionReference, String message);
}

