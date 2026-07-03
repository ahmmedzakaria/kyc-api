package com.nexacore.commonmodule.dto;

import com.nexacore.commonmodule.constants.MessageType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResponseMessage {
    private MessageType type;
    private String code;
    private String message;

    public ResponseMessage(MessageType type, String message) {
        this.type = type;
        this.message = message;
    }
}
