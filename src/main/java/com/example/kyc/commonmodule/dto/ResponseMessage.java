package com.example.kyc.commonmodule.dto;

import com.example.kyc.commonmodule.constants.MessageType;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ResponseMessage {
    private MessageType type;
    private String message;
}
