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
package com.ericsson.oss.apps.model;

import static com.ericsson.oss.apps.util.Constants.UTC;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Table(name = "monitoring_object")
@AllArgsConstructor
@NoArgsConstructor
@IdClass(MonitoringObjectId.class)
@Builder
public class MonitoringObject {

    @Id
    private String fdn;
    @Id
    private String pmeSessionId;
    @Enumerated(EnumType.STRING)
    private StateEnum state;
    private ZonedDateTime startTime;
    private ZonedDateTime lastProcessedTime;
    private ZonedDateTime endTime;

    public MonitoringObject(final MonitoringObjectMessage message) {
        fdn = message.getFdn().toString();
        pmeSessionId = message.getPmeSessionId().toString();
        state = message.getState();
        startTime = ZonedDateTime.ofInstant(message.getTime(), ZoneId.of(UTC)).plusHours(1L).truncatedTo(ChronoUnit.HOURS);
    }

    public MonitoringObjectId getId() {
        return new MonitoringObjectId(fdn, pmeSessionId);
    }
}
