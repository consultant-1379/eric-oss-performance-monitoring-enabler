/*
 * COPYRIGHT Ericsson 2024
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

import {
  createSession,
  httpDelete,
  httpGet,
  httpPostJson,
  logData,
  httpDeleteJson,
} from './common.js';
import {
  INGRESS_URL,
  INGRESS_LOGIN_URI,
  INGRESS_ROUTES_USER_URI,
  INGRESS_PME_TESTWARE_PARAMS,
  INGRESS_ROUTES_RBAC_URI,
  APPLICATION_JSON,
  INGRESS_LOGIN_PASSWORD,
  VALIDATE_EPME_RBAC,
  GAS_ACCESS_ROLE,
  INGRESS_SESSION_OPERATOR,
  INGRESS_CONFIGURATION_OPERATOR,
  INGRESS_CONFIGURATION_READER,
} from './constants.js';
import { EPME_PREFIX } from './epme-constants.js';

let sessionId;

function getEpmeUserPayload(user, role) {
  const epmeUserPayload = JSON.parse(open('../../resources/user.json'));
  epmeUserPayload.password = INGRESS_LOGIN_PASSWORD;
  epmeUserPayload.user.username = user;
  if (VALIDATE_EPME_RBAC) {
    epmeUserPayload.user.privileges = role;
  } else {
    epmeUserPayload.user.privileges = [];
  }
  return epmeUserPayload;
}

function getRbacPayload() {
  const epmeRbacPayload = JSON.parse(open('../../resources/epme_rbac.json'));
  epmeRbacPayload.authorization.resources[0].uris = [
    `${EPME_PREFIX}/*/configurations/**`,
  ];
  epmeRbacPayload.authorization.resources[1].uris = [
    `${EPME_PREFIX}/*/sessions/**`,
  ];
  return epmeRbacPayload;
}

const epmeRbacPayload = getRbacPayload();
const sessionOperatorPayload = getEpmeUserPayload(INGRESS_SESSION_OPERATOR, [
  'PME_Session_Operator',
  GAS_ACCESS_ROLE,
]);
const configurationOperatorPayload = getEpmeUserPayload(
  INGRESS_CONFIGURATION_OPERATOR,
  ['PME_Configuration_Operator', GAS_ACCESS_ROLE],
);
const configurationReaderPayload = getEpmeUserPayload(
  INGRESS_CONFIGURATION_READER,
  ['PME_Configuration_Reader', GAS_ACCESS_ROLE],
);

function getSessionId(options = {}) {
  if (!sessionId) {
    sessionId = createSession(
      INGRESS_URL,
      INGRESS_LOGIN_URI,
      '',
      INGRESS_PME_TESTWARE_PARAMS,
      options,
    );
    logData('CREATE SESSION', sessionId);
  }
  return sessionId;
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

function clearSession() {
  sessionId = undefined;
}

function getSearchQuery(user) {
  return `?search=(username==*${user}*;tenantname==master)`;
}

function getDeleteUri(user) {
  return `/${user}?tenantname=master`;
}

function deleteEpmeRbac(options = {}) {
  return httpDeleteJson(
    INGRESS_URL,
    INGRESS_ROUTES_RBAC_URI,
    epmeRbacPayload,
    getSessionParams(),
    options,
  );
}

function createEpmeRbac(options = {}) {
  return httpPostJson(
    INGRESS_URL,
    INGRESS_ROUTES_RBAC_URI,
    epmeRbacPayload,
    getSessionParams(),
    options,
  );
}

function getSessionOperator(options = {}) {
  return httpGet(
    INGRESS_URL,
    INGRESS_ROUTES_USER_URI.concat(getSearchQuery(INGRESS_SESSION_OPERATOR)),
    getSessionParams(),
    options,
  );
}

function createSessionOperator(options = {}) {
  return httpPostJson(
    INGRESS_URL,
    INGRESS_ROUTES_USER_URI,
    sessionOperatorPayload,
    getSessionParams(),
    options,
  );
}

function deleteSessionOperator(options = {}) {
  return httpDelete(
    INGRESS_URL,
    INGRESS_ROUTES_USER_URI.concat(getDeleteUri(INGRESS_SESSION_OPERATOR)),
    options,
  );
}

function getConfigurationOperator(options = {}) {
  return httpGet(
    INGRESS_URL,
    INGRESS_ROUTES_USER_URI.concat(
      getSearchQuery(INGRESS_CONFIGURATION_OPERATOR),
    ),
    getSessionParams(),
    options,
  );
}

function createConfigurationOperator(options = {}) {
  return httpPostJson(
    INGRESS_URL,
    INGRESS_ROUTES_USER_URI,
    configurationOperatorPayload,
    getSessionParams(),
    options,
  );
}

function deleteConfigurationOperator(options = {}) {
  return httpDelete(
    INGRESS_URL,
    INGRESS_ROUTES_USER_URI.concat(
      getDeleteUri(INGRESS_CONFIGURATION_OPERATOR),
    ),
    options,
  );
}

function getConfigurationReader(options = {}) {
  return httpGet(
    INGRESS_URL,
    INGRESS_ROUTES_USER_URI.concat(
      getSearchQuery(INGRESS_CONFIGURATION_READER),
    ),
    getSessionParams(),
    options,
  );
}

function createConfigurationReader(options = {}) {
  return httpPostJson(
    INGRESS_URL,
    INGRESS_ROUTES_USER_URI,
    configurationReaderPayload,
    getSessionParams(),
    options,
  );
}

function deleteConfigurationReader(options = {}) {
  return httpDelete(
    INGRESS_URL,
    INGRESS_ROUTES_USER_URI.concat(getDeleteUri(INGRESS_CONFIGURATION_READER)),
    options,
  );
}

module.exports = {
  getSessionId,
  clearSession,
  deleteEpmeRbac,
  createEpmeRbac,
  getSessionOperator,
  createSessionOperator,
  deleteSessionOperator,
  getConfigurationOperator,
  createConfigurationOperator,
  deleteConfigurationOperator,
  getConfigurationReader,
  createConfigurationReader,
  deleteConfigurationReader,
};
