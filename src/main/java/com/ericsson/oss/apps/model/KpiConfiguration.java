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
package com.ericsson.oss.apps.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "kpi_configurations")
public class KpiConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String kpiName;
    @Column(nullable = false)
    private boolean monitor;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ThresholdType thresholdType;
    private Double fixedThresholdValue;
    private Double relevanceThreshold;
    private Double confidenceInterval;

    @ToString.Exclude
    @ManyToOne(optional = false)
    @JoinColumn(name = "session_configuration_id")
    private SessionConfiguration sessionConfig;

}
