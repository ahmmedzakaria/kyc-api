package com.example.kyc.appconfigmodule.filters;

import com.example.kyc.authmodule.enitty.ApiAuditLog;
import com.example.kyc.authmodule.repository.ApiAuditLogRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class AuditLoggingFilter extends OncePerRequestFilter {

    private final ApiAuditLogRepository auditLogRepository;

    public AuditLoggingFilter(ApiAuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/auth/authenticate")
                || path.startsWith("/auth/refresh-token")
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

        try {
            filterChain.doFilter(requestWrapper, responseWrapper);
        } finally {
            saveAuditLog(requestWrapper, responseWrapper,request,response);
            responseWrapper.copyBodyToResponse(); // important: copy content back to real response
        }
    }

    private void saveAuditLog(ContentCachingRequestWrapper requestWrapper,
                              ContentCachingResponseWrapper responseWrapper, HttpServletRequest request,HttpServletResponse response) {
        try {
            String requestBody = getRequestBody(requestWrapper);
            String responseBody = getResponseBody(responseWrapper);

            ApiAuditLog log = new ApiAuditLog();
            log.setMethod(request.getMethod());
            log.setUri(request.getRequestURI());
            log.setStatus(response.getStatus());
            log.setRequestBody(requestBody);
            log.setResponseBody(responseBody);

            // Example: fetch username from SecurityContext if available
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                log.setUsername(auth.getName());
            } else {
                log.setUsername("anonymous");
            }

            auditLogRepository.save(log);
        } catch (Exception e) {
            // Avoid breaking the app if audit logging fails
            logger.error("Failed to save audit log", e);
        }
    }

    private String getRequestBody(ContentCachingRequestWrapper request) {
        byte[] buf = request.getContentAsByteArray();
        return buf.length > 0 ? new String(buf, StandardCharsets.UTF_8) : "";
    }

    private String getResponseBody(ContentCachingResponseWrapper response) {
        byte[] buf = response.getContentAsByteArray();
        return buf.length > 0 ? new String(buf, StandardCharsets.UTF_8) : "";
    }

}

