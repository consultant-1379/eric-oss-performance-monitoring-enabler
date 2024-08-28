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

package com.ericsson.oss.apps.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.ericsson.oss.apps.model.Session;
import com.ericsson.oss.apps.model.SessionConfiguration;
import com.ericsson.oss.apps.model.SessionStatus;

@Repository
public interface SessionRepository extends JpaRepository<Session, String> {
    boolean existsByClientIdAndSessionReference(String clientId, String sessionReference);

    List<Session> findByStatusAndFinishedAtBefore(SessionStatus status, LocalDateTime finishedTime);

    List<Session> findByStatusAndCreatedAtBefore(SessionStatus status, LocalDateTime createdTime);

    List<Session> findAllByClientId(String clientId);

    Optional<Session> findByClientIdAndId(String clientId, String id);

    List<Session> findAllByClientIdAndSessionReference(String clientId, String sessionReference);

    List<Session> findAllByStatus(SessionStatus status);

    boolean existsBySessionConfigurationId(long id);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Session s "
            + "SET s.status = SessionStatus.STARTED, s.startedAt = current_timestamp "
            + "WHERE s.id = :id and s.status = SessionStatus.CREATED")
    int updateSessionToStartedById(@Param("id") String id);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Session s "
            + "SET s.status = SessionStatus.FINISHED, s.finishedAt = current_timestamp "
            + "WHERE s.id = :id and s.status = SessionStatus.STARTED")
    int updateSessionToFinishedById(@Param("id") String id);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Session s " +
            "SET s.status = SessionStatus.STOPPED, s.finishedAt = current_timestamp " +
            "WHERE s.id = :id " +
            "AND s.clientId = :clientId " +
            "AND s.status != SessionStatus.STOPPED " +
            "AND s.status != SessionStatus.FINISHED")
    int updateSessionToStoppedByIdAndClientId(@Param("id") String id, @Param("clientId") String clientId);

    @Query("SELECT s.sessionConfiguration FROM Session s where s.id = :id")
    Optional<SessionConfiguration> findConfigurationBySessionId(@Param("id") String id);
}
