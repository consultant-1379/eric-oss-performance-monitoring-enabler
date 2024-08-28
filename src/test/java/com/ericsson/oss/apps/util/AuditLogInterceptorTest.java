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

import static com.ericsson.oss.apps.util.TestConstants.AUTHORIZATION;
import static com.ericsson.oss.apps.util.TestConstants.CONFIGURATIONS_URL;
import static com.ericsson.oss.apps.util.TestConstants.TOKEN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.ericsson.oss.apps.model.SessionConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

@SpringBootTest
@ActiveProfiles("test")
public class AuditLogInterceptorTest {
    @Autowired
    private WebApplicationContext webApplicationContext;
    @Autowired
    private AuditLogInterceptor objectUnderTest;
    private MockMvc mvc;
    private InMemoryLogAppender logAppender;
    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setUpTest() {
        final Logger logUnderTest = (Logger) LoggerFactory.getLogger(objectUnderTest.getClass());
        logUnderTest.setLevel(Level.INFO);
        logAppender = new InMemoryLogAppender();
        logAppender.start();
        logUnderTest.addAppender(logAppender);
        mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    public void verify_auditLogInterceptorIsCalled_AndLogged() throws Exception {
        mvc.perform(post(CONFIGURATIONS_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .header(AUTHORIZATION, TOKEN)
                .content(toJson(new SessionConfiguration())))
                .andExpect(status().isBadRequest());
        logAppender.stop();
        assertThat(logAppender.getLoggedEvents()).asString()
                .contains("Request : POST End Point : " + CONFIGURATIONS_URL);
    }

    @Test
    public void verify_auditLogInterceptorIsCalled_andMessageIsNotLoggedWhenAuditLogsNotEnabledForReads() throws Exception {
        ReflectionTestUtils.setField(objectUnderTest, "enableAuditLogOfReads", false);
        mvc.perform(get(CONFIGURATIONS_URL).contentType(MediaType.APPLICATION_JSON).header(AUTHORIZATION, TOKEN))
                .andExpect(status().isOk());
        logAppender.stop();
        assertThat(logAppender.getLoggedEvents()).asString()
                .doesNotContain("Request : GET End Point : " + CONFIGURATIONS_URL);
    }

    @Test
    public void verify_auditLogInterceptorIsCalled_andMessageIsLoggedWhenAuditLogsIsEnabledForReads() throws Exception {
        ReflectionTestUtils.setField(objectUnderTest, "enableAuditLogOfReads", true);
        mvc.perform(get(CONFIGURATIONS_URL).contentType(MediaType.APPLICATION_JSON).header(AUTHORIZATION, TOKEN))
                .andExpect(status().isOk());
        logAppender.stop();
        assertThat(logAppender.getLoggedEvents()).asString()
                .contains("Request : GET End Point : " + CONFIGURATIONS_URL);
    }

    @Test
    public void verify_auditLogInterceptorIsCalled_andHandlesAuthorizationNotSet() throws Exception {
        ReflectionTestUtils.setField(objectUnderTest, "enableAuditLogOfReads", true);
        mvc.perform(get(CONFIGURATIONS_URL).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
        logAppender.stop();
        assertThat(logAppender.getLoggedEvents()).asString()
                .doesNotContain("Request : GET End Point : " + CONFIGURATIONS_URL);
    }

    private byte[] toJson(final Object object) throws IOException {
        return objectMapper.writeValueAsBytes(object);
    }
}
