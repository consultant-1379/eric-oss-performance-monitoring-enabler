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

import { sleep } from 'k6';

import {
  httpGet,
  httpPostJson,
  httpPutJson,
  createSession,
  logData,
  httpDelete,
  httpRequest,
} from './common.js';
import {
  INGRESS_URL,
  EPME_INGRESS_URL,
  INGRESS_LOGIN_URI,
  APPLICATION_JSON,
  INGRESS_PME_TESTWARE_PARAMS,
  MO_SESSIONS_PARTITION,
  HTTP_STATUS_OK,
  INGRESS_SESSION_OPERATOR_PARAMS,
  INGRESS_CONFIGURATION_OPERATOR_PARAMS,
  INGRESS_CONFIGURATION_READER_PARAMS,
} from './constants.js';
import {
  EPME_SESSIONS_URI,
  EPME_PROMETHEUS_URI,
  MO_ENABLED,
  MONITORING_OBJECT_OFFSET,
  EPME_CONFIGURATIONS_URI,
  EPME_CONFIGURATION_NAME,
  getEpmeConfiguration,
  getEpmeUpdatedConfiguration,
  SESSIONS_BASE_URI,
  CLIENT_ID_PARAM,
  EPME_SESSION_STOP_PAYLOAD,
  SESSION_OBJECTS,
  DEFAULT_EPME_CONFIGURATION_NAME,
  SEARCH_ENGINE_ENTRIES_URI,
} from './epme-constants.js';
import { isStatusOk } from '../utils/validationUtils.js';

let sessionId;
let startTime;

const complexKpis = JSON.parse(
  open('../../resources/kpi_pme_cell_complex_60.json'),
);

const expectedVerdicts = JSON.parse(
  open('../../resources/expectedVerdicts.json'),
);

const monitoringObjects = [];

const getExpectedKPIResponseObject = () => complexKpis;
const getSessionObject = () => JSON.parse(SESSION_OBJECTS[0].body);
const getSessionObjectThree = () => JSON.parse(SESSION_OBJECTS[2].body);

const getExpectedVerdicts = () => {
  const pmeSessionId = getSessionObject().id;

  return expectedVerdicts.map(verdict => ({
    pmeSessionId,
    fdn: verdict.fdn,
    kpiVerdicts: verdict.kpiVerdicts,
  }));
};

function getIdForConfiguration(configName) {
  const response = httpGet(EPME_INGRESS_URL, EPME_CONFIGURATIONS_URI);
  if (response.status !== HTTP_STATUS_OK) {
    logData('Expecting 200 response code, but got: ', response.status);
    return -1;
  }

  const configurationsJson = JSON.parse(response.body);
  for (const configuration of configurationsJson) {
    logData('Configuration name: ', configuration.name);
    if (configuration.name === configName) {
      return configuration.id;
    }
  }
  logData('Could not find configuration name: ', EPME_CONFIGURATION_NAME);
  logData('Configuration payload json was: ', configurationsJson);
  return -1;
}

function getSessionPayloadWithConfigId(payload) {
  payload.pmeConfigId = getIdForConfiguration(DEFAULT_EPME_CONFIGURATION_NAME);
  return payload;
}

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

function getMonitoringObjectStartTime() {
  if (!startTime) {
    startTime = Date.now() - MONITORING_OBJECT_OFFSET;
    logData('Monitoring Objects Start Time', startTime);
  }
  return startTime;
}

function getSessionParams() {
  return {
    headers: {
      'Content-Type': APPLICATION_JSON,
      Accept: APPLICATION_JSON,
      Cookie: __ENV.STAGING_LEVEL === 'APP' ? undefined : getSessionId(),
    },
  };
}

function createSessionParams(createSessionId) {
  return {
    headers: {
      'Content-Type': APPLICATION_JSON,
      Accept: APPLICATION_JSON,
      Cookie: __ENV.STAGING_LEVEL === 'APP' ? undefined : createSessionId,
    },
  };
}

function getLogParams() {
  return {
    headers: {
      'Content-Type': APPLICATION_JSON,
    },
  };
}

function getSessionEndpointStatus(options = {}) {
  let response;
  let sleepAmount = 5;
  let retryCount = 0;
  while (retryCount < 25) {
    response = httpGet(
      EPME_INGRESS_URL,
      EPME_SESSIONS_URI,
      getSessionParams(),
      options,
    );

    if (isStatusOk(response.status)) {
      return response;
    }

    logData(`Sleeping for 30 seconds before retry: ${retryCount}`);
    sleep(sleepAmount);
    if (sleepAmount < 30) {
      sleepAmount += 5;
    }
    retryCount += 1;
  }
  return response;
}

function createEpmeConfiguration(configuration, options = {}) {
  return httpPostJson(
    EPME_INGRESS_URL,
    EPME_CONFIGURATIONS_URI,
    configuration,
    getSessionParams(),
    options,
  );
}

function createDefaultConfigIfNotPresent() {
  if (getIdForConfiguration(DEFAULT_EPME_CONFIGURATION_NAME) === -1) {
    logData(
      'Creating default configuration with the name',
      DEFAULT_EPME_CONFIGURATION_NAME,
    );
    createEpmeConfiguration(getEpmeConfiguration());
  }
}

function getSessionById(options = {}) {
  const pmeSessionId = getSessionObject().id;
  return httpGet(
    EPME_INGRESS_URL,
    `${SESSIONS_BASE_URI}/${pmeSessionId}?${CLIENT_ID_PARAM}`,
    getSessionParams(),
    options,
  );
}

function getAllEpmeSessions(options = {}) {
  return httpGet(
    EPME_INGRESS_URL,
    EPME_SESSIONS_URI,
    getSessionParams(),
    options,
  );
}

function createMonitoringObjects() {
  const session = getSessionObject();
  const complexKpisForFdn = getExpectedKPIResponseObject().value;

  logData('Creating Monitoring objects');
  complexKpisForFdn.forEach(complexKpiForFdn => {
    monitoringObjects.push({
      pmeSessionId: session.id,
      fdn: complexKpiForFdn.fullFdn,
      time: getMonitoringObjectStartTime(),
      state: MO_ENABLED,
    });
  });
  logData(`${monitoringObjects.length} Monitoring Objects have been created`);
  return monitoringObjects;
}

const createMonitoringObjectsFromFdns = allFdns => {
  logData('Creating Monitoring objects');

  allFdns.forEach((fdn, index) => {
    const session = JSON.parse(
      SESSION_OBJECTS[index % MO_SESSIONS_PARTITION].body,
    );
    monitoringObjects.push({
      pmeSessionId: session.id,
      fdn,
      time: getMonitoringObjectStartTime(),
      state: MO_ENABLED,
    });
  });

  logData(`${monitoringObjects.length} Monitoring Objects have been created`);
  return monitoringObjects;
};

function updateEpmeConfiguration(options = {}) {
  const configurationId = getIdForConfiguration(EPME_CONFIGURATION_NAME);
  const updateConfigurationsUrl = `${EPME_CONFIGURATIONS_URI}/${configurationId}`;
  logData('Update url: ', updateConfigurationsUrl);

  return httpPutJson(
    EPME_INGRESS_URL,
    updateConfigurationsUrl,
    getEpmeUpdatedConfiguration(),
    getSessionParams(),
    options,
  );
}

function deleteSessionConfiguration(options = {}) {
  const configurationId = getIdForConfiguration(EPME_CONFIGURATION_NAME);
  const deleteConfigurationsUrl = `${EPME_CONFIGURATIONS_URI}/${configurationId}`;
  logData('Delete url: ', deleteConfigurationsUrl);

  return httpDelete(
    EPME_INGRESS_URL,
    deleteConfigurationsUrl,
    getSessionParams(),
    options,
  );
}

function getAllSessionConfigurations(options = {}) {
  return httpGet(
    EPME_INGRESS_URL,
    EPME_CONFIGURATIONS_URI,
    getSessionParams(),
    options,
  );
}

function getOneSessionConfiguration(options = {}) {
  return httpGet(
    EPME_INGRESS_URL,
    `${EPME_CONFIGURATIONS_URI}/${getIdForConfiguration(
      DEFAULT_EPME_CONFIGURATION_NAME,
    )}`,
    getSessionParams(),
    options,
  );
}

function stopEpmeSession(options = {}) {
  const pmeSessionId = getSessionObjectThree().id;
  const stopSessionsUri = `${SESSIONS_BASE_URI}/${pmeSessionId}/status?${CLIENT_ID_PARAM}`;

  return httpPutJson(
    EPME_INGRESS_URL,
    stopSessionsUri,
    EPME_SESSION_STOP_PAYLOAD,
    getSessionParams(),
    options,
  );
}

function getCreatedSessionIds() {
  return SESSION_OBJECTS.map(response => response.body)
    .map(JSON.parse)
    .map(({ id }) => id);
}

function getSessionOperatorSessionId(options = {}) {
  if (!sessionId) {
    sessionId = createSession(
      INGRESS_URL,
      INGRESS_LOGIN_URI,
      '',
      INGRESS_SESSION_OPERATOR_PARAMS,
      options,
    );
    logData('CREATE SESSION OPERATOR SESSION', INGRESS_SESSION_OPERATOR_PARAMS);
    logData('CREATE SESSION OPERATOR SESSION', sessionId);
  }
  return sessionId;
}

function getConfigurationOperatorSessionId(options = {}) {
  if (!sessionId) {
    sessionId = createSession(
      INGRESS_URL,
      INGRESS_LOGIN_URI,
      '',
      INGRESS_CONFIGURATION_OPERATOR_PARAMS,
      options,
    );
    logData(
      'CREATE CONFIGURATION OPERATOR SESSION',
      INGRESS_CONFIGURATION_OPERATOR_PARAMS,
    );
    logData('CREATE CONFIGURATION OPERATOR SESSION', sessionId);
  }
  return sessionId;
}

function getConfigurationReaderSessionId(options = {}) {
  if (!sessionId) {
    sessionId = createSession(
      INGRESS_URL,
      INGRESS_LOGIN_URI,
      '',
      INGRESS_CONFIGURATION_READER_PARAMS,
      options,
    );
    logData(
      'CREATE CONFIGURATION READER SESSION',
      INGRESS_CONFIGURATION_READER_PARAMS,
    );
    logData('CREATE CONFIGURATION READER SESSION', sessionId);
  }
  return sessionId;
}

function cleanSession() {
  sessionId = undefined;
}

function createEpmeSession(epmePayload, options = {}) {
  cleanSession();

  getConfigurationOperatorSessionId();
  createDefaultConfigIfNotPresent();
  const sessionPayload = getSessionPayloadWithConfigId(epmePayload);
  cleanSession();

  getSessionOperatorSessionId();

  const sessionObject = httpPostJson(
    EPME_INGRESS_URL,
    EPME_SESSIONS_URI,
    sessionPayload,
    getSessionParams(),
    options,
  );
  SESSION_OBJECTS.push(sessionObject);
  return sessionObject;
}

function createEpmeSessionWithPayload(epmePayload, options = {}) {
  return httpPostJson(
    EPME_INGRESS_URL,
    EPME_SESSIONS_URI,
    epmePayload,
    getSessionParams(),
    options,
  );
}

function getMetrics() {
  return httpGet(EPME_INGRESS_URL, EPME_PROMETHEUS_URI);
}

function getLogsWithFilter(filterPayload) {
  const date = new Date(Date.now());
  const dateDay = date.getDate().toString().padStart(2, '0');
  const dateMonthNum = date.getMonth() + 1;
  const dateMonth = dateMonthNum.toString().padStart(2, '0');
  const dateYear = date.getFullYear().toString();
  const formattedDate = `${dateYear}.${dateMonth}.${dateDay}`;

  const rAppIndex = `/rapps-logs-${formattedDate}`;
  const rAppURI = rAppIndex.concat(SEARCH_ENGINE_ENTRIES_URI);
  return httpRequest(
    'GET',
    EPME_INGRESS_URL,
    rAppURI,
    JSON.stringify(filterPayload),
    getLogParams(),
  );
}

module.exports = {
  getSessionId,
  getExpectedKPIResponseObject,
  getSessionParams,
  getSessionEndpointStatus,
  createEpmeSession,
  getSessionById,
  getAllEpmeSessions,
  createMonitoringObjects,
  createMonitoringObjectsFromFdns,
  createEpmeConfiguration,
  updateEpmeConfiguration,
  createSessionParams,
  deleteSessionConfiguration,
  getAllSessionConfigurations,
  getOneSessionConfiguration,
  getExpectedVerdicts,
  stopEpmeSession,
  getIdForConfiguration,
  getSessionPayloadWithConfigId,
  getCreatedSessionIds,
  getSessionOperatorSessionId,
  getConfigurationReaderSessionId,
  getConfigurationOperatorSessionId,
  cleanSession,
  createEpmeSessionWithPayload,
  getMetrics,
  getLogsWithFilter,
};
