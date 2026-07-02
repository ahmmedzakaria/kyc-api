package com.nexacore.servicesmodule.emailservice.service.implementations;

import com.nexacore.servicesmodule.emailservice.config.EmailMessagingProperties;
import com.nexacore.servicesmodule.emailservice.dto.EmailMessageRequest;
import com.nexacore.servicesmodule.emailservice.dto.EmailMessageResponse;
import com.nexacore.servicesmodule.emailservice.enums.EmailMessageType;
import com.nexacore.servicesmodule.emailservice.service.interfaces.EmailMessagingService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.UnsupportedEncodingException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailMessagingServiceImpl implements EmailMessagingService {

    private final EmailMessagingProperties properties;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Override
    public EmailMessageResponse sendEmail(EmailMessageRequest request) {
        validateRequest(request);

        if (!properties.isEnabled()) {
            log.info("Email messaging is disabled. Skipping {} email to {}", request.getMessageType(), request.getTo());
            return buildSkippedResponse(request);
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            throw new IllegalStateException("JavaMailSender is not configured");
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(buildFromAddress());
            helper.setTo(request.getTo().toArray(String[]::new));
            if (!CollectionUtils.isEmpty(request.getCc())) {
                helper.setCc(request.getCc().toArray(String[]::new));
            }
            if (!CollectionUtils.isEmpty(request.getBcc())) {
                helper.setBcc(request.getBcc().toArray(String[]::new));
            }
            helper.setSubject(request.getSubject().trim());
            helper.setText(request.getBody(), request.isHtml());

            mailSender.send(message);
            return EmailMessageResponse.builder()
                    .success(true)
                    .skipped(false)
                    .providerName(properties.getProviderName())
                    .to(request.getTo())
                    .messageType(request.getMessageType())
                    .referenceId(request.getReferenceId())
                    .providerResponse("Email sent")
                    .build();
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new IllegalStateException("Failed to build email message", e);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to send email message", e);
        }
    }

    @Override
    public EmailMessageResponse sendOtp(String emailAddress, String otp, Duration validity) {
        if (!StringUtils.hasText(otp)) {
            throw new IllegalArgumentException("OTP must not be blank");
        }

        long validityMinutes = Optional.ofNullable(validity)
                .filter(value -> !value.isZero() && !value.isNegative())
                .orElse(Duration.ofMinutes(5))
                .toMinutes();

        String body = String.format(properties.getOtpTemplate(), otp.trim(), validityMinutes);

        return sendEmail(EmailMessageRequest.builder()
                .to(List.of(emailAddress))
                .subject(properties.getOtpSubject())
                .body(body)
                .messageType(EmailMessageType.OTP)
                .referenceId("OTP")
                .metadata(Map.of("validityMinutes", validityMinutes))
                .build());
    }

    @Override
    public EmailMessageResponse sendTransactionInfo(String emailAddress, String transactionReference, String message) {
        return sendEmail(EmailMessageRequest.builder()
                .to(List.of(emailAddress))
                .subject(properties.getTransactionSubject())
                .body(message)
                .messageType(EmailMessageType.TRANSACTION_INFO)
                .referenceId(transactionReference)
                .build());
    }

    private InternetAddress buildFromAddress() throws MessagingException, UnsupportedEncodingException {
        if (StringUtils.hasText(properties.getFromName())) {
            return new InternetAddress(properties.getFromAddress(), properties.getFromName());
        }
        return new InternetAddress(properties.getFromAddress());
    }

    private void validateRequest(EmailMessageRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Email message request must not be null");
        }
        if (CollectionUtils.isEmpty(request.getTo())) {
            throw new IllegalArgumentException("Email recipient list must not be empty");
        }
        if (request.getTo().stream().anyMatch(value -> !StringUtils.hasText(value))) {
            throw new IllegalArgumentException("Email recipient must not be blank");
        }
        if (!StringUtils.hasText(request.getSubject())) {
            throw new IllegalArgumentException("Email subject must not be blank");
        }
        if (!StringUtils.hasText(request.getBody())) {
            throw new IllegalArgumentException("Email body must not be blank");
        }
        if (request.getMessageType() == null) {
            request.setMessageType(EmailMessageType.GENERAL);
        }
    }

    private EmailMessageResponse buildSkippedResponse(EmailMessageRequest request) {
        return EmailMessageResponse.builder()
                .success(false)
                .skipped(true)
                .providerName(properties.getProviderName())
                .to(request.getTo())
                .messageType(request.getMessageType())
                .referenceId(request.getReferenceId())
                .providerResponse("Email messaging is disabled")
                .build();
    }
}
