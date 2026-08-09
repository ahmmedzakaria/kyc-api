package com.nexacore.logmodule.filter;

import com.nexacore.gatewaymodule.auth.service.interfaces.AuthModuleGateway;
import com.nexacore.logmodule.dto.LogContextDto;
import com.nexacore.logmodule.service.LogService;
import com.nexacore.systemmodule.accesscontrol.entity.SysAccApiRegistry;
import com.nexacore.systemmodule.accesscontrol.entity.SysAccClientApplication;
import com.nexacore.systemmodule.accesscontrol.security.ClientApplicationContext;
import com.nexacore.systemmodule.accesscontrol.security.ClientApplicationContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class ApiLoggingFilter extends OncePerRequestFilter {

    private final LogService logService;
    private final AuthModuleGateway authModuleGateway;

    public ApiLoggingFilter(LogService logService, AuthModuleGateway authModuleGateway) {
        this.logService = logService;
        this.authModuleGateway = authModuleGateway;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/api/v1/auth/authenticate")
                || path.startsWith("/api/v1/auth/refresh-token")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-resources")
                || path.startsWith("/webjars");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // Wrap request and response to read bodies
        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
        Exception failure = null;

        try {
            filterChain.doFilter(requestWrapper, responseWrapper);
        } catch (Exception e) {
            failure = e;
            throw e;
        } finally {
            saveLogs(requestWrapper, responseWrapper, request, failure);
            responseWrapper.copyBodyToResponse(); // important: copy content back to real response
        }
    }

    private void saveLogs(ContentCachingRequestWrapper requestWrapper,
                          ContentCachingResponseWrapper responseWrapper,
                          HttpServletRequest request,
                          Exception failure) {
        try {
            String requestBody = getRequestBody(requestWrapper);
            String responseBody = getResponseBody(responseWrapper);
            String username = getUsername();
            LogContextDto context = buildLogContext(username);
            int status = failure == null ? responseWrapper.getStatus() : HttpServletResponse.SC_INTERNAL_SERVER_ERROR;

            logService.writeApiAccessLog(
                    request.getMethod(),
                    request.getRequestURI(),
                    status,
                    username,
                    requestBody,
                    responseBody,
                    context
            );

            if (failure != null || status >= 500) {
                logService.writeErrorLog(
                        request.getMethod(),
                        request.getRequestURI(),
                        status,
                        username,
                        failure == null ? "HTTP_" + status : failure.getClass().getName(),
                        failure == null ? responseBody : failure.getMessage(),
                        requestBody,
                        responseBody,
                        context
                );
            }
        } catch (Exception e) {
            logger.error("Failed to save API log", e);
        }
    }

    private String getUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() ? auth.getName() : "anonymous";
    }

    private LogContextDto buildLogContext(String username) {
        ClientApplicationContext context = ClientApplicationContextHolder.get().orElse(null);
        SysAccClientApplication clientApplication = context == null ? null : context.clientApplication();
        SysAccApiRegistry apiRegistry = context == null ? null : context.apiRegistry();

        return LogContextDto.builder()
                .traceId(context == null ? null : context.traceId())
                .clientCode(clientApplication == null ? null : clientApplication.getClientCode())
                .clientType(clientApplication == null || clientApplication.getClientType() == null ? null : clientApplication.getClientType().name())
                .userId(resolveUserId(username))
                .apiCode(apiRegistry == null ? null : apiRegistry.getApiCode())
                .moduleCode(apiRegistry == null ? null : apiRegistry.getModuleCode())
                .moduleName(apiRegistry == null ? null : apiRegistry.getModuleName())
                .submoduleCode(apiRegistry == null ? null : apiRegistry.getSubmoduleCode())
                .submoduleName(apiRegistry == null ? null : apiRegistry.getSubmoduleName())
                .featureCode(apiRegistry == null ? null : apiRegistry.getFeatureCode())
                .featureName(apiRegistry == null ? null : apiRegistry.getFeatureName())
                .actionCode(apiRegistry == null ? null : apiRegistry.getActionCode())
                .actionName(apiRegistry == null ? null : apiRegistry.getActionName())
                .accessMode(username == null || "anonymous".equals(username) ? "CLIENT_CREDENTIAL" : "USER")
                .decision(context == null ? null : context.decision())
                .denyReason(context == null ? null : context.denyReason())
                .build();
    }

    private Long resolveUserId(String username) {
        if (username == null || "anonymous".equals(username)) {
            return null;
        }
        try {
            return authModuleGateway.getUserId(username);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String getRequestBody(ContentCachingRequestWrapper request) {
        byte[] buf = request.getContentAsByteArray();
        return buf.length > 0 ? stripNulBytes(new String(buf, StandardCharsets.UTF_8)) : "";
    }

    private String getResponseBody(ContentCachingResponseWrapper response) {
        byte[] buf = response.getContentAsByteArray();
        return buf.length > 0 ? stripNulBytes(new String(buf, StandardCharsets.UTF_8)) : "";
    }

    // Postgres TEXT columns reject 0x00, which can appear when the captured
    // body is binary content (e.g. photo/document bytes) rather than text.
    private String stripNulBytes(String body) {
        return body.indexOf('\u0000') < 0 ? body : body.replace("\u0000", "");
    }

}
