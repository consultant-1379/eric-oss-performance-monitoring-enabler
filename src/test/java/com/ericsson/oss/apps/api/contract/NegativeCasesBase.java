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

import static com.ericsson.oss.apps.api.contract.ContractConstants.CONFLICTING_CLIENT_EXECUTION_ID;
import static com.ericsson.oss.apps.model.SessionStatus.STOPPED;
import static com.ericsson.oss.apps.util.TestConstants.ALT_CLIENT_APP_ID;
import static com.ericsson.oss.apps.util.TestConstants.CLIENT_APP_ID;
import static com.ericsson.oss.apps.util.TestConstants.PME_CONFIGURATION_ID;
import static com.ericsson.oss.apps.util.TestConstants.PME_SESSION_ID;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.context.WebApplicationContext;

import com.ericsson.oss.apps.model.Session;
import com.ericsson.oss.apps.model.SessionConfiguration;
import com.ericsson.oss.apps.repository.SessionConfigurationRepository;
import com.ericsson.oss.apps.service.SessionService;
import com.ericsson.oss.apps.service.StartupService;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

@ActiveProfiles({ "test", "contract" })
@DirtiesContext
@SpringBootTest
public class NegativeCasesBase {
    @Autowired
    private WebApplicationContext context;
    @MockBean
    private StartupService startupService;

    @SpyBean
    private SessionService sessionService;

    @MockBean
    private SessionConfigurationRepository sessionConfigurationRepository;

    @BeforeEach
    public void setup() {
        RestAssuredMockMvc.webAppContextSetup(context);
        mockConfigurationService();
        mockSessionService();
    }

    private void mockConfigurationService() {
        createMocksForSessionContracts();
        createMocksForConfigurationContracts();
    }

    private void createMocksForSessionContracts() {
        when(sessionConfigurationRepository.findById(not(eq(PME_CONFIGURATION_ID)))).thenReturn(Optional.empty());
    }

    private void createMocksForConfigurationContracts() {
        createMocksForConfigurationCreate();
        createMocksForConfigurationUpdate();
        createMocksForConfigurationDelete();
    }

    private void createMocksForConfigurationCreate() {
        // Create Internal server error
        when(sessionConfigurationRepository.save(nullable(SessionConfiguration.class))).thenThrow(new DataAccessResourceFailureException(""));

        // Internal server error
        when(sessionConfigurationRepository.existsByName("sample_configuration_with_internal_error")).thenReturn(false);
        doThrow(new DataAccessResourceFailureException("")).when(sessionConfigurationRepository).save(nullable(SessionConfiguration.class));

        // Bad request - name already used
        when(sessionConfigurationRepository.existsByName("sample_configuration_with_name_already_used")).thenReturn(true);
    }

    private void createMocksForConfigurationDelete() {
        final SessionConfiguration sessionConfiguration1 = new SessionConfiguration();
        sessionConfiguration1.setId(2);

        //Delete Conflict
        when(sessionConfigurationRepository.findById(2L)).thenReturn(Optional.of(sessionConfiguration1));
        when(sessionService.isSessionConfigurationInUse(2L)).thenReturn(true);

        //Delete internal server error
        final SessionConfiguration sessionConfiguration2 = new SessionConfiguration();
        sessionConfiguration2.setId(3L);
        when(sessionConfigurationRepository.findById(3L)).thenReturn(Optional.of(sessionConfiguration2));
        doThrow(new DataAccessResourceFailureException("")).when(sessionConfigurationRepository).deleteById(3L);
    }

    private void createMocksForConfigurationUpdate() {
        final SessionConfiguration sessionConfiguration101 = new SessionConfiguration();
        sessionConfiguration101.setId(101);
        sessionConfiguration101.setName("sample_configuration_101");

        // Not found 
        when(sessionConfigurationRepository.findById(99999L)).thenReturn(Optional.empty());

        //Update Conflict
        when(sessionConfigurationRepository.findById(101L)).thenReturn(Optional.of(sessionConfiguration101));
        when(sessionService.isSessionConfigurationInUse(101L)).thenReturn(true);

        // internal server error when trying to read the existing configuration
        final SessionConfiguration sessionConfiguration100 = new SessionConfiguration();
        sessionConfiguration100.setName("sample_configuration_100");
        sessionConfiguration100.setId(100L);
        when(sessionConfigurationRepository.findById(100L)).thenReturn(Optional.of(sessionConfiguration100));
        doThrow(new DataAccessResourceFailureException("")).when(sessionConfigurationRepository).save(nullable(SessionConfiguration.class));
    }

    private void mockSessionService() {
        final var session = Session.builder().status(STOPPED).build();

        when(sessionService.exists(CLIENT_APP_ID, CONFLICTING_CLIENT_EXECUTION_ID))
                .thenReturn(true);
        when(sessionService.updateSessionToStopped(PME_SESSION_ID, CLIENT_APP_ID)).thenReturn(0);
        when(sessionService.findByClientIdAndSessionId(ALT_CLIENT_APP_ID, PME_SESSION_ID)).thenReturn(Optional.of(session));
    }
}