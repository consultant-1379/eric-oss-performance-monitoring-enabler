/*******************************************************************************
 * COPYRIGHT Ericsson 2021
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

package com.ericsson.oss.apps.controller.health;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.ericsson.oss.apps.CoreApplication;
import com.ericsson.oss.apps.service.StartupService;

@ActiveProfiles("test")
@DirtiesContext
@SpringBootTest(classes = { CoreApplication.class, HealthCheck.class })
class HealthCheckTest {

    @Autowired
    private WebApplicationContext webApplicationContext;
    private MockMvc mvc;
    @MockBean
    private StartupService startupService;
    @Autowired
    private HealthCheck health;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void whenGetHealthStatus_andStatusIsUp_then200IsReturned() throws Exception {
        when(startupService.getStatus()).thenReturn(StartupService.StartupStatus.READY);
        mvc.perform(get("/actuator/health").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json("{'status' : 'UP'}"));
    }

    @Test
    void whenGetHealthStatus_andStatusIsFailed_then5XXIsReturned() throws Exception {
        health.failHealthCheck("HC down");
        mvc.perform(get("/actuator/health").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is5xxServerError())
                .andExpect(content().json("{'status' : 'DOWN'}"));
    }

    @Test
    void whenGetHealthStatus_andStartupServiceIsRunning_then200IsReturned() throws Exception {
        when(startupService.getStatus()).thenReturn(StartupService.StartupStatus.VALIDATING_DATA_SUBSCRIPTIONS);
        mvc.perform(get("/actuator/health").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json("{'status' : 'UP'}"));
    }

    @Test
    void whenGetHealthStatus_andStartupServiceHasFailed_then503IsReturned() throws Exception {
        when(startupService.getStatus()).thenReturn(StartupService.StartupStatus.FAILED);
        mvc.perform(get("/actuator/health").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().json("{'status' : 'OUT_OF_SERVICE'}"));
    }
}
