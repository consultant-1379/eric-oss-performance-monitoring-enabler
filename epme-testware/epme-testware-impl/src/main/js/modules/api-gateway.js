/*
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
 */

import {
  createSession,
  logData,
  httpDelete,
  httpPutJson,
  httpPostJson,
} from './common.js';

import {
  INGRESS_URL,
  INGRESS_LOGIN_URI,
  INGRESS_PME_TESTWARE_PARAMS,
  INGRESS_HOST,
  EPME_ROUTE_ID,
  INGRESS_ROUTES_URI,
} from './constants.js';
import {
  ROUTE_WITHOUT_PROMETHEUS,
  ROUTE_WITH_PROMETHEUS,
} from './epme-constants.js';

import { getSessionParams } from './epme.js';

let sessionId;

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

function getRoutePayload(route) {
  const routePayload = JSON.parse(open(route));
  routePayload.predicates[1].args._genkey_0 = INGRESS_HOST;
  return routePayload;
}

const epmeRoutePayload = getRoutePayload(ROUTE_WITHOUT_PROMETHEUS);
function onboardEpmeApis() {
  return httpPostJson(
    INGRESS_URL,
    INGRESS_ROUTES_URI,
    epmeRoutePayload,
    getSessionParams(),
  );
}

const epmeUpdateRoutePayload = getRoutePayload(ROUTE_WITH_PROMETHEUS);
function onboardEpmePrometheusApis() {
  return httpPutJson(
    INGRESS_URL,
    INGRESS_ROUTES_URI,
    epmeUpdateRoutePayload,
    getSessionParams(),
  );
}

function offboardEpmeApis() {
  return httpDelete(
    INGRESS_URL,
    `${INGRESS_ROUTES_URI}/${EPME_ROUTE_ID}`,
    undefined,
    getSessionParams(),
  );
}

function clearSession() {
  sessionId = undefined;
}

module.exports = {
  getSessionId,
  onboardEpmeApis,
  onboardEpmePrometheusApis,
  offboardEpmeApis,
  clearSession,
};
