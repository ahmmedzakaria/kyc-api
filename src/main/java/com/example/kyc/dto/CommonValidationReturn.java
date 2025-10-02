package com.example.kyc.dto;

import java.util.ArrayList;
import java.util.List;

public class CommonValidationReturn {

	private boolean isValid;
	private List<String> messages = new ArrayList<String>();

	public boolean isValid() {
		return isValid;
	}
	public void setValid(boolean isValid) {
		this.isValid = isValid;
	}
	public List<String> getMessages() {
		return messages;
	}
	public void setMessage(String message) {
		this.messages.add(message);
	}
}
