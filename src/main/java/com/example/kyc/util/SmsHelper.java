package com.example.kyc.util;

import com.example.kyc.dto.MessageRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

//@Service
@RequiredArgsConstructor
@Slf4j
@Async
public class SmsHelper {

    @Value("${sms.api.url}")
    private String smsApiUrl;

    @Value("${sms.api.key}")
    private String apiKey;
    
    private final WebClient webClient;
    
    public void sendSms(MessageRequestDto emailDto) {
        String[] recipients = emailDto.getTo().split(",");

        for (String recpnt : recipients) {
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("api_key", emailDto.getSubject());
            requestBody.put("msg", emailDto.getMessage());
            requestBody.put("to", recpnt);

            webClient.post()
                    .uri(smsApiUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .doOnNext(response -> log.debug("SMS Response: {}", response))
                    .doOnError(e -> log.debug("Error sending SMS: {}", e.getMessage()))
                    .subscribe();
        }
    }

    public void sendSms(String mobileNumber, String otp) {
    	log.debug("Sending scheduled SMS...");
        
        String smsBody = "Use "+ otp +" as your MediPilot OTP";
        String smsRecipient = mobileNumber;
 
        MessageRequestDto emailDto = new MessageRequestDto();
        emailDto.setMessage(smsBody);
        emailDto.setTo(smsRecipient);
        emailDto.setSubject(apiKey);
        
        sendSms(emailDto);
    }
    
}
