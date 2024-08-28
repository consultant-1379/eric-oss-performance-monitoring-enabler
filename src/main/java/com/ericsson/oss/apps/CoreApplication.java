/*******************************************************************************
 * COPYRIGHT Ericsson 2023 - 2024
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

import static com.ericsson.oss.apps.util.Constants.VERSION;

import java.util.Optional;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.ericsson.oss.apps.util.AuditLogInterceptor;

/**
 * Core Application, the starting point of the application.
 */
@SpringBootApplication
@EnableScheduling
@EnableRetry
public class CoreApplication {
    /**
     * Main entry point of the application.
     *
     * @param args
     *            Command line arguments
     */
    public static void main(final String[] args) {
        SpringApplication.run(CoreApplication.class, args);
    }

    /**
     * Configuration bean for Web MVC.
     * 
     * @param auditLogInterceptors
     *            auditLogInterceptor
     * @return WebMvcConfigurer
     */
    @Bean
    public WebMvcConfigurer webConfigurer(final ObjectProvider<AuditLogInterceptor> auditLogInterceptors) {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(final InterceptorRegistry registry) {
                final Optional<AuditLogInterceptor> auditLogInterceptor = auditLogInterceptors.orderedStream().findAny();
                if (auditLogInterceptor.isPresent()) {
                    final AuditLogInterceptor interceptor = auditLogInterceptor.get();
                    registry.addInterceptor(interceptor)
                            .addPathPatterns(VERSION + "/**");
                }
            }
        };
    }

    /**
     * Making a RestTemplate, using the RestTemplateBuilder, to use for consumption of RESTful interfaces.
     *
     * @param restTemplateBuilder
     *            RestTemplateBuilder instance
     * @return RestTemplate
     */
    @Bean
    public RestTemplate restTemplate(final RestTemplateBuilder restTemplateBuilder) {
        return restTemplateBuilder.build();
    }

}
