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

import static com.ericsson.assertions.EpmeAssertions.assertThat;
import static com.ericsson.oss.apps.util.Constants.UNEXPECTED_ERROR;
import static com.ericsson.oss.apps.util.Constants.VALIDATION_FAILED;
import static com.ericsson.oss.apps.util.Constants.WEEKEND_DAYS_INVALID_VALUE;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;

import com.ericsson.oss.apps.api.model.EpmeErrorDetails;
import com.ericsson.oss.apps.api.model.EpmeWeekendDays;
import com.ericsson.oss.apps.util.Constants;
import com.fasterxml.jackson.databind.exc.ValueInstantiationException;
import com.fasterxml.jackson.databind.type.SimpleType;

@ExtendWith(MockitoExtension.class)
class GlobalExecutionHandlerTest {

    private static final String BAD_REQUEST_REASON = "br reason";
    private static final String INTERNAL_SERVER_REASON = "is reason";
    private static final String INTERNAL_SERVER_DETAIL = "is detail";
    private static final String FAILED_TO_READ_REQUST = "Failed to read request";
    private static final String REQUIRED_PARAMETER_CLIENT_ID_IS_NOT_PRESENT = "Required parameter 'clientId' is not present";
    private static final String BAD_REQUEST = "BAD REQUEST";
    private final GlobalExceptionHandler objectUnderTest = new GlobalExceptionHandler();

    @Mock
    private HttpMessageNotReadableException httpMessageNotReadableException;
    @Mock
    private ValueInstantiationException valueInstantiationException;
    @Mock
    private MissingServletRequestParameterException missingServletRequestParameterException;
    @Mock
    private ProblemDetail problemDetail;

    @Test
    void whenCustomErrorHandling_forExceptionWithoutDetails_verifyResponseReturned() {
        final var exception = ControllerDetailException.builder()
                .withStatus(HttpStatus.BAD_REQUEST)
                .withReason(BAD_REQUEST_REASON)
                .build();

        final var errorBody = new EpmeErrorDetails()
                .title(BAD_REQUEST_REASON)
                .status(HttpStatus.BAD_REQUEST.value());

        assertThat(objectUnderTest.customErrorHandling(exception))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .hasBody(errorBody);
    }

    @Test
    void whenCustomErrorHandling_forExceptionWithDetail_verifyResponseContainsDetail() {
        final var exception = ControllerDetailException.builder()
                .withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                .withReason(INTERNAL_SERVER_REASON)
                .withDetail(INTERNAL_SERVER_DETAIL)
                .build();

        final var errorBody = new EpmeErrorDetails()
                .title(INTERNAL_SERVER_REASON)
                .detail(INTERNAL_SERVER_DETAIL)
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value());

        assertThat(objectUnderTest.customErrorHandling(exception))
                .hasStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                .hasBody(errorBody);
    }

    @Test
    void whenMissingServletRequestParameterHandling_thenResponseContainsBadRequestWithMissingParameterMsg() {

        when(missingServletRequestParameterException.getBody()).thenReturn(problemDetail);
        when(problemDetail.getStatus()).thenReturn(HttpStatus.BAD_REQUEST.value());
        when(problemDetail.getTitle()).thenReturn(BAD_REQUEST);
        when(problemDetail.getDetail()).thenReturn(REQUIRED_PARAMETER_CLIENT_ID_IS_NOT_PRESENT);

        final var errorBody = new EpmeErrorDetails()
                .title(BAD_REQUEST)
                .detail(REQUIRED_PARAMETER_CLIENT_ID_IS_NOT_PRESENT)
                .status(HttpStatus.BAD_REQUEST.value());

        assertThat(objectUnderTest.handleMissingServletRequestParameter(missingServletRequestParameterException, null, HttpStatus.BAD_REQUEST, null))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .hasBody(errorBody);
    }

    @Test
    void whenHandleHttpMessageNotReadableWithInvalidWeekendDays_thenResponseContainsBadRequestWithInvalidWeekendDaysMsg() {
        final SimpleType epmeWeekendDaysJavaType = SimpleType.constructUnsafe(EpmeWeekendDays.class);
        when(httpMessageNotReadableException.getCause()).thenReturn(valueInstantiationException);
        when(valueInstantiationException.getType()).thenReturn(epmeWeekendDaysJavaType);

        final var errorBody = new EpmeErrorDetails()
                .title(VALIDATION_FAILED)
                .detail(WEEKEND_DAYS_INVALID_VALUE)
                .status(HttpStatus.BAD_REQUEST.value());

        assertThat(objectUnderTest.handleHttpMessageNotReadable(httpMessageNotReadableException, null, HttpStatus.BAD_REQUEST, null))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .hasBody(errorBody);
    }

    @Test
    void whenHandleHttpMessageNotReadableWithFailedToReadRequest_thenResponseContainsBadRequestWithFailedToReadRequestMsg() {
        final SimpleType stringJavaType = SimpleType.constructUnsafe(String.class);
        when(httpMessageNotReadableException.getCause()).thenReturn(valueInstantiationException);
        when(valueInstantiationException.getType()).thenReturn(stringJavaType);

        final var errorBody = new EpmeErrorDetails()
                .title(VALIDATION_FAILED)
                .detail(FAILED_TO_READ_REQUST)
                .status(HttpStatus.BAD_REQUEST.value());

        assertThat(objectUnderTest.handleHttpMessageNotReadable(httpMessageNotReadableException, null, HttpStatus.BAD_REQUEST, null))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .hasBody(errorBody);
    }

    @Test
    void whenCreateResponseEntityForUnexpectedError_thenResponseContainsInternalServerErrortWithUnexpectedErrorMsg() {
        final var errorBody = new EpmeErrorDetails()
                .title(Constants.INTERNAL_SERVER_ERROR)
                .detail(UNEXPECTED_ERROR)
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value());

        assertThat(objectUnderTest.createResponseEntity(null, null, HttpStatus.INTERNAL_SERVER_ERROR, null))
                .hasStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                .hasBody(errorBody);
    }
}
