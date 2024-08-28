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

package com.ericsson.oss.apps.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.Executor;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@DirtiesContext
@SpringBootTest(properties = {
        "execution.threadPool.size=10"
})
@ActiveProfiles("test")
public class ExecutionPoolConfigurationTest {

    @Autowired
    @Qualifier("SessionTaskExecutor")
    private Executor sessionExecutor;

    @Test
    void whenAThreadPoolExecutorIsCreated_itHasTheCorrectConfiguration() {
        final ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) sessionExecutor;
        assertThat(executor.getCorePoolSize()).isEqualTo(10);
        assertThat(executor.getThreadNamePrefix()).isEqualTo("sessionTaskExecutor-");
    }
}
