package com.nexacore.logmodule.filter;

import com.nexacore.logmodule.service.LogService;
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

    public ApiLoggingFilter(LogService logService) {
        this.logService = logService;
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
            int status = failure == null ? responseWrapper.getStatus() : HttpServletResponse.SC_INTERNAL_SERVER_ERROR;

            logService.writeApiAccessLog(
                    request.getMethod(),
                    request.getRequestURI(),
                    status,
                    username,
                    requestBody,
                    responseBody
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
                        responseBody
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

    private String getRequestBody(ContentCachingRequestWrapper request) {
        byte[] buf = request.getContentAsByteArray();
        return buf.length > 0 ? new String(buf, StandardCharsets.UTF_8) : "";
    }

    private String getResponseBody(ContentCachingResponseWrapper response) {
        byte[] buf = response.getContentAsByteArray();
        return buf.length > 0 ? new String(buf, StandardCharsets.UTF_8) : "";
    }

}
