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

import {
  createGasSession,
  logData,
  httpDelete,
  httpPostJson,
} from './common.js';
import {
  INGRESS_URL,
  INGRESS_ROUTES_USER_URI,
  INGRESS_LOGIN_PASSWORD,
  INGRESS_PME_TESTWARE_USER,
} from './constants.js';
import { createSessionParams } from './epme.js';

function getPmeTestwareUserPayload(user) {
  const pmeTestwareUserPayload = JSON.parse(open('../../resources/user.json'));
  pmeTestwareUserPayload.password = INGRESS_LOGIN_PASSWORD;
  pmeTestwareUserPayload.user.username = user;

  return pmeTestwareUserPayload;
}

function getDeleteUri(user = INGRESS_PME_TESTWARE_USER) {
  return `/${user}?tenantname=master`;
}

const pmeTestwareUserPayload = getPmeTestwareUserPayload(
  INGRESS_PME_TESTWARE_USER,
);
export function createPmeTestwareUser(options = {}) {
  logData('Creating Pme-Testware-User User');
  const result = httpPostJson(
    INGRESS_URL,
    INGRESS_ROUTES_USER_URI,
    pmeTestwareUserPayload,
    createSessionParams(createGasSession()),
    options,
  );
  logData('Pme-Testware-User User Created:', result);
  return result;
}
export function deletePmeTestwareUser(options = {}) {
  logData('Deleting Pme-Testware-User User');
  return httpDelete(
    INGRESS_URL,
    INGRESS_ROUTES_USER_URI.concat(getDeleteUri()),
    undefined,
    createSessionParams(createGasSession()),
    options,
  );
}
