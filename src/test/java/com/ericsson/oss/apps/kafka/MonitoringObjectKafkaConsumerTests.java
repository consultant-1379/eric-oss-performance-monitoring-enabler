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

package com.ericsson.oss.apps.kafka;

import static com.ericsson.oss.apps.util.Constants.UTC;
import static com.ericsson.oss.apps.util.TestConstants.CLIENT_APP_ID;
import static com.ericsson.oss.apps.util.TestConstants.PME_CONFIGURATION_ID;
import static com.ericsson.oss.apps.util.TestConstants.PME_SESSION_ID;
import static com.ericsson.oss.apps.util.TestConstants.SESSION_CONFIGURATION;
import static com.ericsson.oss.apps.util.TestConstants.SESSION_REFERENCE;
import static com.ericsson.oss.apps.util.TestConstants.WEEKEND_DAYS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import com.ericsson.oss.apps.model.MonitoringObject;
import com.ericsson.oss.apps.model.MonitoringObjectId;
import com.ericsson.oss.apps.model.MonitoringObjectMessage;
import com.ericsson.oss.apps.model.Session;
import com.ericsson.oss.apps.model.SessionConfiguration;
import com.ericsson.oss.apps.model.SessionStatus;
import com.ericsson.oss.apps.model.StateEnum;
import com.ericsson.oss.apps.repository.MonitoringObjectRepository;
import com.ericsson.oss.apps.repository.SessionConfigurationRepository;
import com.ericsson.oss.apps.repository.SessionRepository;
import com.ericsson.oss.apps.service.ConfigurationService;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@DirtiesContext
@SpringBootTest
@EmbeddedKafka(partitions = 1, ports = 9092, topics = { "${spring.kafka.topics.monitoringObjectTopic}" })
@ActiveProfiles("test")
class MonitoringObjectKafkaConsumerTests {

    private static final long ONE_SECOND = 1000;
    private static final String FDN = "SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00001";
    private static final int SESSION_DURATION = 18;

    @Autowired
    private KafkaTestProducer testProducer;
    @Autowired
    private InvalidKafkaProducer invalidProducer;
    @Autowired
    private MonitoringObjectRepository moRepo;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private MonitoringObjectKafkaConsumer objectUnderTest;

    @Autowired
    private SessionConfigurationRepository sessionConfigurationRepository;

    @Value("${spring.kafka.topics.monitoringObjectTopic}")
    private String topic;

    @PostConstruct
    public void createSessionConfiguration() {
        sessionConfigurationRepository.save(buildSessionConfiguration());
    }

    @BeforeEach
    void tearDown() {
        moRepo.deleteAll();
        sessionRepository.deleteAll();
    }

    @Test
    void whenMonitoringObjectIsSentToTopic_andConsumerIsListening_thenMonitoringObjectIsPersistedInDb() throws InterruptedException {
        assertThat(objectUnderTest).isNotNull();
        setupSession(SessionStatus.CREATED);

        final Instant time = Instant.ofEpochMilli(System.currentTimeMillis());
        final MonitoringObjectMessage moMessage = MonitoringObjectMessage.newBuilder()
                .setPmeSessionId(PME_SESSION_ID)
                .setFdn(FDN)
                .setTime(time)
                .setState(StateEnum.ENABLED)
                .build();

        testProducer.sendMessage(topic, moMessage);
        // The small delay here and in further tests is to give the db time to update.
        awaitUpdates();

        final ZonedDateTime expectedZonedDateTime = ZonedDateTime.ofInstant(time, ZoneId.of(UTC)).plusHours(1L).truncatedTo(ChronoUnit.HOURS);
        final ZonedDateTime expectedEndTime = expectedZonedDateTime.plusHours(SESSION_DURATION).minusSeconds(1L);

        final MonitoringObject mo = moRepo.getReferenceById(new MonitoringObjectId(FDN, PME_SESSION_ID));
        assertThat(mo.getFdn()).isEqualTo(FDN);
        assertThat(mo.getPmeSessionId()).isEqualTo(PME_SESSION_ID);
        assertThat(mo.getState()).isEqualTo(StateEnum.ENABLED);
        assertThat(mo.getStartTime()).isEqualTo(expectedZonedDateTime);
        assertThat(mo.getLastProcessedTime()).isNull();
        assertThat(mo.getEndTime()).isEqualTo(expectedEndTime);
    }

    @Test
    void whenMonitoringObjectIsSentAndStringEquivalentIsSent_thenOnlyMonitoringObjectIsPersistedInDb() throws InterruptedException {
        assertThat(objectUnderTest).isNotNull();
        setupSession(SessionStatus.CREATED);

        final Instant time = Instant.ofEpochMilli(System.currentTimeMillis());
        final MonitoringObjectMessage moMessage = MonitoringObjectMessage.newBuilder()
                .setPmeSessionId(PME_SESSION_ID)
                .setFdn(FDN)
                .setTime(time)
                .setState(StateEnum.ENABLED)
                .build();

        invalidProducer.sendInvalidMessage(topic);
        awaitUpdates();

        testProducer.sendMessage(topic, moMessage);
        awaitUpdates();

        assertThat(moRepo.count()).isOne();
        final MonitoringObject mo = moRepo.getReferenceById(new MonitoringObjectId(FDN, PME_SESSION_ID));
        assertThat(mo.getFdn()).isEqualTo(FDN);
    }

    @Test
    void whenTwoMonitoringObjectsAreSentWithTheSamePaSessionIdAndFdn_thenOnlyOneIsPersistedInDb() throws InterruptedException {
        assertThat(objectUnderTest).isNotNull();
        setupSession(SessionStatus.CREATED);

        final Instant time = Instant.ofEpochMilli(System.currentTimeMillis());
        final MonitoringObjectMessage moMessage = MonitoringObjectMessage.newBuilder()
                .setPmeSessionId(PME_SESSION_ID)
                .setFdn(FDN)
                .setTime(time)
                .setState(StateEnum.ENABLED)
                .build();

        log.info("Test send messages:: whenTwoMonitoringObjectsAreSentWithTheSamePaSessionIdAndFdn_thenOnlyOneIsPersistedInDb");

        await()
                .pollDelay(Duration.ofSeconds(1))
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    testProducer.sendMessage(topic, moMessage);
                    assertThat(moRepo.count()).isOne();
                });

        final MonitoringObject mo = moRepo.getReferenceById(new MonitoringObjectId(FDN, PME_SESSION_ID));
        assertThat(mo.getFdn()).isEqualTo(FDN);
        assertThat(mo.getPmeSessionId()).isEqualTo(PME_SESSION_ID);
        assertThat(mo.getState()).isEqualByComparingTo(StateEnum.ENABLED);

        final MonitoringObjectMessage moMessage2 = MonitoringObjectMessage.newBuilder()
                .setPmeSessionId(PME_SESSION_ID)
                .setFdn(FDN)
                .setTime(time)
                .setState(StateEnum.STOPPED)
                .build();

        testProducer.sendMessage(topic, moMessage2);
        awaitUpdates();

        assertThat(moRepo.count()).isOne();
        final MonitoringObject mo2 = moRepo.getReferenceById(new MonitoringObjectId(FDN, PME_SESSION_ID));
        assertThat(mo2.getFdn()).isEqualTo(FDN);
        assertThat(mo2.getPmeSessionId()).isEqualTo(PME_SESSION_ID);
        assertThat(mo2.getState()).isEqualByComparingTo(StateEnum.STOPPED);
    }

    @Test
    void whenMonitoringObjectsIsSentWithTheSamePaSessionIdNotExist_thenMoNotPersistedInDb() throws InterruptedException {
        assertThat(objectUnderTest).isNotNull();

        final Instant time = Instant.ofEpochMilli(System.currentTimeMillis());
        final MonitoringObjectMessage moMessage = MonitoringObjectMessage.newBuilder()
                .setPmeSessionId(PME_SESSION_ID)
                .setFdn(FDN)
                .setTime(time)
                .setState(StateEnum.ENABLED)
                .build();

        testProducer.sendMessage(topic, moMessage);
        awaitUpdates();

        assertThat(moRepo.count()).isZero();
    }

    @Test
    void whenMonitoringObjectIsSent_andTheSessionIsStopped_nothingIsPersisted() throws InterruptedException {
        assertThat(objectUnderTest).isNotNull();
        setupSession(SessionStatus.STOPPED);

        final Instant time = Instant.ofEpochMilli(System.currentTimeMillis());
        final MonitoringObjectMessage moMessage = MonitoringObjectMessage.newBuilder()
                .setPmeSessionId(PME_SESSION_ID)
                .setFdn(FDN)
                .setTime(time)
                .setState(StateEnum.ENABLED)
                .build();

        testProducer.sendMessage(topic, moMessage);
        // The small delay here and in further tests is to give the db time to update.
        awaitUpdates();
        assertThat(moRepo.count()).isZero();
    }

    @Test
    void whenMonitoringObjectIsSent_andTheSessionIsFinished_nothingIsPersisted() throws InterruptedException {
        assertThat(objectUnderTest).isNotNull();
        setupSession(SessionStatus.FINISHED);

        final Instant time = Instant.ofEpochMilli(System.currentTimeMillis());
        final MonitoringObjectMessage moMessage = MonitoringObjectMessage.newBuilder()
                .setPmeSessionId(PME_SESSION_ID)
                .setFdn(FDN)
                .setTime(time)
                .setState(StateEnum.ENABLED)
                .build();

        testProducer.sendMessage(topic, moMessage);
        // The small delay here and in further tests is to give the db time to update.
        awaitUpdates();
        assertThat(moRepo.count()).isZero();
    }

    private SessionConfiguration buildSessionConfiguration() {
        final SessionConfiguration sessionConfiguration = new SessionConfiguration();
        sessionConfiguration.setId(PME_CONFIGURATION_ID);
        sessionConfiguration.setName(SESSION_CONFIGURATION);
        sessionConfiguration.setWeekendDays(WEEKEND_DAYS);
        return sessionConfiguration;
    }

    private void setupSession(final SessionStatus status) {
        final var session = Session.builder()
                .id(PME_SESSION_ID)
                .sessionReference(SESSION_REFERENCE)
                .clientId(CLIENT_APP_ID)
                .createdAt(LocalDateTime.now())
                .status(status)
                .duration(SESSION_DURATION)
                .sessionConfiguration(configurationService.getConfig(String.valueOf(PME_CONFIGURATION_ID)))
                .build();
        sessionRepository.save(session);
    }

    private void awaitUpdates() throws InterruptedException {
        Thread.sleep(ONE_SECOND); // NOSONAR Only used in test
    }
}
