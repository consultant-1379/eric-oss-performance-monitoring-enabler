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

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@Entity
@Table(name = "sessions")
@NoArgsConstructor
@AllArgsConstructor
public class Session {
    @Id
    private String id;

    @Pattern(regexp = "^[\\w-_]+$")
    @NotBlank
    @Column(updatable = false, nullable = false)
    private String clientId;

    @Pattern(regexp = "^[\\w-_]+$")
    @NotBlank
    @Column(updatable = false, nullable = false)
    private String sessionReference;

    @NotNull
    @Min(1)
    @Max(24)
    @Column(updatable = false, nullable = false)
    private Integer duration;

    @NotNull
    @ManyToOne
    @JoinColumn(updatable = false, nullable = false)
    private SessionConfiguration sessionConfiguration;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionStatus status;

    @NotNull
    @Column(updatable = false, nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;
}
