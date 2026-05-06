package com.nexacore.commonmodule.dto;

import com.nexacore.commonmodule.constants.MessageType;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ResponseMessage {
    private MessageType type;
    private String message;
}
