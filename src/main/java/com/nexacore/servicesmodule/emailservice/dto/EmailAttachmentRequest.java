package com.nexacore.servicesmodule.emailservice.dto;

import org.springframework.core.io.Resource;

public record EmailAttachmentRequest(String filename, String contentType, Resource resource) {}
