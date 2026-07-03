package com.nexacore.commonmodule.i18n.util;

import org.springframework.util.StringUtils;

public final class MessageCodeUtil {

    private MessageCodeUtil() {
    }

    public static boolean looksLikeMessageCode(String value) {
        return StringUtils.hasText(value)
                && value.matches("^[a-z][a-z0-9]*(\\.[a-z0-9_]+)+$");
    }
}
