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

package com.ericsson.oss.apps.api.contract;

import static com.ericsson.oss.apps.util.TestConstants.CLIENT_APP_ID;
import static com.ericsson.oss.apps.util.TestConstants.PME_CONFIGURATION_ID;
import static com.ericsson.oss.apps.util.TestConstants.PME_SESSION_ID;
import static com.ericsson.oss.apps.util.TestConstants.SESSION_REFERENCE;
import static com.ericsson.oss.apps.util.TestConstants.WEEKEND_DAYS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.context.WebApplicationContext;

import com.ericsson.oss.apps.api.model.EpmeConfigurationRequest;
import com.ericsson.oss.apps.model.Session;
import com.ericsson.oss.apps.model.SessionConfiguration;
import com.ericsson.oss.apps.model.SessionStatus;
import com.ericsson.oss.apps.repository.SessionConfigurationRepository;
import com.ericsson.oss.apps.repository.SessionRepository;
import com.ericsson.oss.apps.service.ConfigurationService;
import com.ericsson.oss.apps.service.SessionService;
import com.ericsson.oss.apps.service.StartupService;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

@ActiveProfiles({ "test", "contract" })
@DirtiesContext
@SpringBootTest
public class PositiveCasesBase {

    final private LocalDateTime timestamp = LocalDateTime.now();
    @Autowired
    private WebApplicationContext context;
    @MockBean
    private StartupService startupService;
    @MockBean
    private SessionRepository sessionRepository;
    @MockBean
    private SessionConfigurationRepository sessionConfigurationRepository;
    @SpyBean
    private SessionService sessionService;
    @Mock
    private SessionConfiguration sessionConfig;

    @Mock
    private ConfigurationService configurationService;

    @BeforeEach
    public void setup() {
        RestAssuredMockMvc.webAppContextSetup(context);
        mockConfigurationService();
        mockSessionService();
    }

    private void mockConfigurationService() {
        when(sessionConfig.getId()).thenReturn(PME_CONFIGURATION_ID);
        when(sessionConfig.getWeekendDays()).thenReturn(WEEKEND_DAYS);
        when(configurationService.getConfig(String.valueOf(PME_CONFIGURATION_ID)))
                .thenReturn(sessionConfig);
        when(configurationService.createSessionConfiguration(any(EpmeConfigurationRequest.class)))
                .thenCallRealMethod();
        when(sessionConfigurationRepository.findById(PME_CONFIGURATION_ID)).thenReturn(Optional.of(sessionConfig));

        // mocks for create configuration
        when(sessionConfigurationRepository.existsByName(any(String.class))).thenReturn(false);
        when(sessionConfigurationRepository.save(any(SessionConfiguration.class)))
                .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0, SessionConfiguration.class));

        // mocks for update configuration
        when(sessionConfig.getName()).thenReturn("sample_configuration_with_all_predefined_kpis_included");

    }

    private void mockSessionService() {
        when(sessionService.exists(anyString(), anyString())).thenReturn(false);
        when(sessionRepository.save(any(Session.class)))
                .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0, Session.class));
        when(sessionRepository.findAllByClientId(CLIENT_APP_ID)).thenReturn(getSessionAsList());
        when(sessionRepository.findAllByClientIdAndSessionReference(CLIENT_APP_ID, SESSION_REFERENCE)).thenReturn(getSessionAsList());
        when(sessionRepository.findByClientIdAndId(CLIENT_APP_ID, PME_SESSION_ID)).thenReturn(Optional.of(createSession()));
        when(sessionRepository.updateSessionToStoppedByIdAndClientId(PME_SESSION_ID, CLIENT_APP_ID)).thenReturn(1);
    }

    private List<Session> getSessionAsList() {
        return Collections.singletonList(createSession());
    }

    private Session createSession() {
        return new Session(PME_SESSION_ID,
                CLIENT_APP_ID,
                SESSION_REFERENCE,
                18,
                new SessionConfiguration(),
                SessionStatus.CREATED,
                timestamp,
                timestamp,
                timestamp);
    }
}
