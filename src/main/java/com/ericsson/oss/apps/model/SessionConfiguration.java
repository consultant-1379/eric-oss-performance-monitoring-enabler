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

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@NoArgsConstructor
@Table(name = "session_configurations")
public class SessionConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(unique = true, nullable = false)
    private String name;

    @Column(nullable = false)
    private int numberOfConnectedUsersForReliability;

    @Column(nullable = false)
    private String weekendDays;

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "sessionConfig", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<KpiConfiguration> kpiConfigs = new ArrayList<>();

    public SessionConfiguration(final String name, final String weekendDays) {
        this.name = name;
        this.weekendDays = weekendDays;
    }

    public void setKpiConfigs(final List<KpiConfiguration> kpiConfigs) {
        this.kpiConfigs.clear();
        this.kpiConfigs.addAll(kpiConfigs);
        this.kpiConfigs.forEach(r -> r.setSessionConfig(this));
    }

}