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

package com.ericsson.oss.apps.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ericsson.oss.apps.exception.ControllerDetailException;
import com.ericsson.oss.apps.service.StartupService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@ExtendWith(MockitoExtension.class)
class StartupReadyInterceptorTest {
    @Mock
    private StartupService startupService;

    @InjectMocks
    private StartupReadyInterceptor objectUnderTest;

    @Test
    void whenPreHandle_andServiceIsReady_thenTrueIsReturned() {
        assertThat(objectUnderTest.preHandle(mock(HttpServletRequest.class), mock(HttpServletResponse.class), mock(Object.class)))
                .isTrue();
    }

    @Test
    void whenPreHandle_andServiceIsNotReady_thenExceptionIsThrown() {
        doThrow(ControllerDetailException.class).when(startupService).verifyServiceReady();

        assertThatThrownBy(() -> objectUnderTest
                .preHandle(mock(HttpServletRequest.class), mock(HttpServletResponse.class), mock(Object.class)))
                .isInstanceOf(ControllerDetailException.class);
    }
}
