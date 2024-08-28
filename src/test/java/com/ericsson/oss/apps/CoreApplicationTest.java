/*******************************************************************************
 * COPYRIGHT Ericsson 2021 - 2024
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

package com.ericsson.oss.apps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.StringUtils;
import org.springframework.web.context.WebApplicationContext;

@ActiveProfiles("test")
@AutoConfigureObservability
@DirtiesContext
@SpringBootTest(classes = { CoreApplication.class, CoreApplicationTest.class })
public class CoreApplicationTest {

    private static final int ACTUATOR_URI_EXPOSED = 5;
    @Autowired
    private WebApplicationContext webApplicationContext;
    private MockMvc mvc;

    @Value("${info.app.description}")
    private String description;

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    @BeforeEach
    public void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    public void metrics_available() throws Exception {
        final MvcResult result = mvc.perform(get("/actuator/prometheus").contentType(MediaType.TEXT_PLAIN)).andExpect(status().isOk())
                .andReturn();
        Assertions.assertTrue(result.getResponse().getContentAsString().contains("jvm_threads_states_threads"));
    }

    @Test
    public void info_available() throws Exception {
        final MvcResult result = mvc.perform(get("/actuator/info").contentType(MediaType.TEXT_PLAIN)).andExpect(status().isOk())
                .andReturn();
        Assertions.assertTrue(result.getResponse().getContentAsString().contains(description));
    }

    @Test
    public void actuator_available() throws Exception {
        final MvcResult result = mvc.perform(get("/actuator").contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
                .andReturn();
        assertThat(StringUtils.countOccurrencesOf(result.getResponse().getContentAsString(), "href")).isEqualTo(ACTUATOR_URI_EXPOSED);
    }
}
