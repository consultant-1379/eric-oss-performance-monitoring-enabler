/*******************************************************************************
 * COPYRIGHT Ericsson 2023
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

package com.ericsson.oss.apps.exception;

import static com.ericsson.oss.apps.util.Constants.INTERNAL_SERVER_ERROR;
import static com.ericsson.oss.apps.util.Constants.UNEXPECTED_ERROR;
import static com.ericsson.oss.apps.util.Constants.VALIDATION_FAILED;
import static com.ericsson.oss.apps.util.Constants.WEEKEND_DAYS_INVALID_VALUE;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.ericsson.oss.apps.api.model.EpmeErrorDetails;
import com.ericsson.oss.apps.api.model.EpmeWeekendDays;
import com.fasterxml.jackson.databind.exc.ValueInstantiationException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    @ExceptionHandler(ControllerDetailException.class)
    public ResponseEntity<EpmeErrorDetails> customErrorHandling(@NonNull final ControllerDetailException ex) {
        final var error = new EpmeErrorDetails()
                .title(ex.getReason())
                .status(ex.getStatusCode().value())
                .detail(ex.getDetail());
        return new ResponseEntity<>(error, ex.getStatusCode());
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(@NonNull final MissingServletRequestParameterException ex,
            @NonNull final HttpHeaders headers, @NonNull final HttpStatusCode status, @NonNull final WebRequest request) {
        final var exBody = ex.getBody();
        final var error = new EpmeErrorDetails()
                .status(exBody.getStatus())
                .title(exBody.getTitle())
                .detail(exBody.getDetail());
        return new ResponseEntity<>(error, status);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(final HttpMessageNotReadableException ex, @NonNull final HttpHeaders headers,
            @NonNull final HttpStatusCode status, @NonNull final WebRequest request) {

        if (ex.getCause() instanceof ValueInstantiationException) {
            final ValueInstantiationException valueInstantiationException = (ValueInstantiationException) ex.getCause();
            if (valueInstantiationException.getType().hasRawClass(EpmeWeekendDays.class)) {
                final var error = new EpmeErrorDetails()
                        .status(status.value())
                        .title(VALIDATION_FAILED)
                        .detail(WEEKEND_DAYS_INVALID_VALUE);
                return new ResponseEntity<>(error, status);
            }
        }
        return super.handleHttpMessageNotReadable(ex, headers, status, request);
    }

    @Override
    @NonNull
    protected ResponseEntity<Object> createResponseEntity(@Nullable final Object body, @NonNull final HttpHeaders headers,
            @NonNull final HttpStatusCode statusCode, @NonNull final WebRequest request) {

        String errorDetails = UNEXPECTED_ERROR;
        if (body instanceof ProblemDetail) {
            errorDetails = ((ProblemDetail) body).getDetail();
        }
        String errorTitle = INTERNAL_SERVER_ERROR;
        if (statusCode.is4xxClientError()) {
            errorTitle = VALIDATION_FAILED;
        }

        final var error = new EpmeErrorDetails()
                .status(statusCode.value())
                .title(errorTitle)
                .detail(errorDetails);

        return new ResponseEntity<>(error, statusCode);
    }

}
