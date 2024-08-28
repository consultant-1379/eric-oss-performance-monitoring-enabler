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

package com.ericsson.oss.apps.config;

import static com.ericsson.oss.apps.util.Constants.VERSION;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.ericsson.oss.apps.controller.StartupReadyInterceptor;
import com.ericsson.oss.apps.service.StartupService;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {
    private final StartupService startupService;

    @Override
    public void addInterceptors(final InterceptorRegistry registry) {
        registry
                .addInterceptor(new StartupReadyInterceptor(startupService))
                .addPathPatterns(VERSION + "/sessions");
    }
}
