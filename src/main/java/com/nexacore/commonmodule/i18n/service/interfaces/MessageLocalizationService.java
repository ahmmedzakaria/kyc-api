package com.nexacore.commonmodule.i18n.service.interfaces;

import com.nexacore.commonmodule.i18n.dto.LocalizedMessage;

import java.util.Locale;

public interface MessageLocalizationService {

    String getMessage(String code, Object... args);

    String getMessage(String code, Locale locale, Object... args);

    LocalizedMessage getLocalizedMessage(String code, Object... args);

    LocalizedMessage getLocalizedMessage(String code, Locale locale, Object... args);
}
