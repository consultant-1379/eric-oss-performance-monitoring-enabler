/*******************************************************************************
 * COPYRIGHT Ericsson 2024
 *
 *
 *
 * The copyright to the computer program(s) herein is the property of
 *
 * Ericsson Inc. The programs may be used and/or copied only with written
 *
 * permission from Ericsson Inc. or in accordance with the terms and
 *
 * conditions stipulated in the agreement/contract under which the
 *
 * program(s) have been supplied.
 ******************************************************************************/

package com.ericsson.oss.apps.util;

import static com.ericsson.oss.apps.util.Constants.AUDIT_LOG;
import static com.ericsson.oss.apps.util.Constants.FACILITY_KEY;
import static com.ericsson.oss.apps.util.Constants.SUBJECT_KEY;

import java.util.Objects;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class AuditLogInterceptor implements HandlerInterceptor {
    private final Boolean enableAuditLogOfReads;

    public AuditLogInterceptor(@Value("${logging.enableAuditLogOfReads}") final Boolean enableAuditLogOfReads) {
        this.enableAuditLogOfReads = enableAuditLogOfReads;
    }

    @Override
    public boolean preHandle(final HttpServletRequest request, final HttpServletResponse response, final Object handler) {
        final String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (!Objects.isNull(authorizationHeader)) {
            final String accessToken = authorizationHeader.split(" ")[1];
            if (!HttpMethod.GET.toString().equals(request.getMethod()) || enableAuditLogOfReads) {
                final String logMsg = "Request : " + request.getMethod() + " End Point : " + request.getRequestURI();
                logAudit(logMsg, JwtDecoder.getUserName(accessToken));
            }
        }
        return true;
    }

    void logAudit(final String msg, final String userName) {
        MDC.put(FACILITY_KEY, AUDIT_LOG);
        MDC.put(SUBJECT_KEY, userName);
        log.info("{}", msg);
        MDC.remove(FACILITY_KEY);
        MDC.remove(SUBJECT_KEY);
    }
}
