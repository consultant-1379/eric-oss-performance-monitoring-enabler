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

package com.ericsson.oss.apps.client.iam.model;

import static com.ericsson.oss.apps.util.Constants.JSON_PROPERTY_ACCESS_TOKEN;
import static com.ericsson.oss.apps.util.Constants.JSON_PROPERTY_EXPIRES_IN;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccessTokenResponse {
    @JsonProperty(JSON_PROPERTY_ACCESS_TOKEN)
    @JsonInclude
    private String accessToken;

    @JsonProperty(JSON_PROPERTY_EXPIRES_IN)
    @JsonInclude
    private int expiresIn;
}
