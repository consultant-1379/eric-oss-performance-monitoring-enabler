/*
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
 */

import { httpDelete, httpGet, httpPut } from './common.js';

import { getSessionId } from './api-gateway.js';

import {
  INGRESS_URL,
  APPLICATION_JSON,
  INGRESS_DC_SUBSCRIPTIONS,
  INGRESS_DCC_SUBSCRIPTIONS,
  RAPP_ID,
} from './constants.js';

const IDS_FILE = open('../../resources/input-data-specification.json');

function getIDSFileBody() {
  return IDS_FILE;
}

function getSessionParams() {
  return {
    headers: {
      'Content-Type': APPLICATION_JSON,
      Accept: APPLICATION_JSON,
      Cookie: getSessionId(),
    },
  };
}

function putIDSFile(body) {
  return httpPut(
    INGRESS_URL,
    `${INGRESS_DCC_SUBSCRIPTIONS}/${RAPP_ID}`,
    body,
    getSessionParams(),
  );
}

function deleteIDSFile() {
  return httpDelete(
    INGRESS_URL,
    `${INGRESS_DCC_SUBSCRIPTIONS}/${RAPP_ID}`,
    undefined,
    getSessionParams(),
  );
}

function getRAppSubscriptions() {
  return httpGet(
    INGRESS_URL,
    `${INGRESS_DC_SUBSCRIPTIONS}?rAppId=${RAPP_ID}`,
    getSessionParams(),
  );
}

module.exports = {
  getIDSFileBody,
  putIDSFile,
  deleteIDSFile,
  getRAppSubscriptions,
};
