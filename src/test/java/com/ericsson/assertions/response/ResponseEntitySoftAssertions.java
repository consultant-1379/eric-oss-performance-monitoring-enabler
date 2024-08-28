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
package com.ericsson.assertions.response;

import org.assertj.core.api.SoftAssertions;
import org.springframework.http.HttpEntity;

/**
 * Implementation of SoftAssertions for generic {@link HttpEntity}.
 */
public class ResponseEntitySoftAssertions<T> extends SoftAssertions implements IResponseEntitySoftAssertions<T> {
    //empty implementation to combine the interface and parent class functionality
}
