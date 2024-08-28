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
import { httpGet, logData } from './common.js';
import {
  INGRESS_URL,
  EXECUTION_GROUP_COMPLEX,
  EXECUTION_GROUPS_SIMPLE,
  EXECUTION_GROUPS_METADATA,
  STATUS_FINISHED,
  HTTP_STATUS_OK,
} from './constants.js';
import {
  PMSC_CALCULATION_URI,
  PMSC_DEFINITIONS_URI,
  PMSC_COMPLEX_QUERY_URI,
  PMSC_QUERY_URI,
  RETRY_ATTEMPTS_FOR_SIMPLE_KPIS,
} from './pmsch-constants.js';
import { getSessionParams } from './epme.js';
import { containsExpectedKpis } from '../utils/validationUtils.js';

let allFdns = [];

function getAllFdns() {
  return allFdns;
}

function getKpiDefinitions() {
  let response = {};
  let retryCount = 0;
  while (retryCount < 5) {
    response = httpGet(INGRESS_URL, PMSC_DEFINITIONS_URI);
    if (
      response.status === HTTP_STATUS_OK &&
      containsExpectedKpis(response.body)
    ) {
      break;
    } else {
      retryCount += 1;
      logData(`Sleeping for 10 seconds before retry: ${retryCount}`);
      sleep(10);
    }
  }
  return response;
}

function getCalculatedKPIValues(expectedFDNCount) {
  let response = {};
  let retryCount = 0;
  let responseBody;

  const timestamp = new Date();
  timestamp.setHours(timestamp.getHours() - 1, 0, 0, 0);

  const queryUri = `${PMSC_COMPLEX_QUERY_URI}?$filter=aggregation_begin_time%20eq%20${timestamp.toISOString()}`;

  while (retryCount < 30) {
    response = httpGet(INGRESS_URL, queryUri);
    if (response.status !== HTTP_STATUS_OK) {
      logData('Expecting 200 response code,but got: ', response.status);
      return response;
    }
    responseBody = JSON.parse(response.body);
    if (responseBody.value.length === expectedFDNCount) {
      return response;
    }
    retryCount += 1;
    logData(`Sleeping for 30 seconds before retry: ${retryCount}`);
    sleep(30);
  }
  logData(
    `Did not get expected number of FDNs with calculated KPIs. Expected: ${expectedFDNCount} FDNs, Got: ${responseBody.value.length} FDNs.`,
  );
  return response;
}

function getPersistedKpiDefinitions() {
  return httpGet(INGRESS_URL, PMSC_DEFINITIONS_URI, getSessionParams());
}

function retrievePersistedComplexKpis() {
  let retryCount = 0;
  let responseBody;
  let response;

  while (retryCount < 120) {
    response = httpGet(INGRESS_URL, PMSC_CALCULATION_URI, getSessionParams());

    if (response.status !== HTTP_STATUS_OK) {
      logData(`Expecting 200 response code,but got: ${response.status}`);
      return response;
    }

    responseBody = JSON.parse(response.body);

    for (const group of responseBody) {
      if (
        group.status === STATUS_FINISHED &&
        group.executionGroup === EXECUTION_GROUP_COMPLEX
      ) {
        response = httpGet(
          INGRESS_URL,
          `${PMSC_CALCULATION_URI}/${group.calculationId}`,
          getSessionParams(),
        );

        if (response.status !== HTTP_STATUS_OK) {
          logData(
            `Expecting ${HTTP_STATUS_OK} response code,but got: ${response.status}`,
          );
          return response;
        }

        const executionGroup = JSON.parse(response.body);
        const readinessLogsLen = executionGroup.readinessLogs.length;

        const lastTimestamp =
          executionGroup.readinessLogs[readinessLogsLen - 1]
            .earliestCollectedData;

        let dateTime = new Date(lastTimestamp);
        dateTime.setMinutes(0);
        dateTime = dateTime.toISOString();

        const timestamp = dateTime.slice(0, dateTime.length - 5).concat('Z');

        return httpGet(
          INGRESS_URL,
          `${PMSC_COMPLEX_QUERY_URI}?$filter=aggregation_begin_time%20eq%20${timestamp}`,
          getSessionParams(),
        );
      }
    }

    retryCount += 1;
    logData(
      `Calculated scheduled complex KPIs not found. Sleeping for 30 seconds before retry: ${retryCount}`,
    );
    sleep(30);
  }

  logData(
    'Failed to retrieve scheduled complex KPIs. No calculated KPIs found.',
  );
  return response;
}

function parseFdns(response, fdnKpiName) {
  const fdnsInKpis = [];
  const responseBody = JSON.parse(response.body);
  for (const kpi of responseBody.value) {
    const fdn = kpi[fdnKpiName];
    if (fdn) {
      fdnsInKpis.push(fdn);
    }
  }
  return fdnsInKpis;
}

function retrieveKpisForSchema(group, dateTime, completedGroups) {
  const timestamp = dateTime.slice(0, dateTime.length - 5).concat('Z');
  const groupData = EXECUTION_GROUPS_METADATA.get(group.executionGroup);
  const response = httpGet(
    INGRESS_URL,
    `${PMSC_QUERY_URI}/${groupData.table}?$filter=aggregation_begin_time%20eq%20${timestamp}`,
    getSessionParams(),
  );
  allFdns = allFdns.concat(parseFdns(response, groupData.fullFdnKpiName));
  completedGroups.push(group.executionGroup);
}

function isGroupValid(group, completedGroups) {
  return (
    group.status === STATUS_FINISHED &&
    EXECUTION_GROUPS_SIMPLE.includes(group.executionGroup) &&
    !completedGroups.includes(group.executionGroup)
  );
}

function truncateTimeToHoursAsString(dateTime) {
  dateTime.setMinutes(0, 0, 0);
  dateTime = dateTime.toISOString();
  return dateTime;
}

function retrievePersistedSimpleKpis() {
  let retryCount = 0;
  let responseBody;
  let response;
  const completedGroups = [];

  while (retryCount < RETRY_ATTEMPTS_FOR_SIMPLE_KPIS) {
    response = httpGet(
      INGRESS_URL,
      `${PMSC_CALCULATION_URI}?elapsedMinutes=120`,
      getSessionParams(),
    );
    if (response.status !== HTTP_STATUS_OK) {
      logData(`Expecting 200 response code, but got: ${response.status}`);
      return false;
    }

    responseBody = JSON.parse(response.body);
    for (const group of responseBody) {
      if (isGroupValid(group, completedGroups)) {
        response = httpGet(
          INGRESS_URL,
          `${PMSC_CALCULATION_URI}/${group.calculationId}`,
          getSessionParams(),
        );

        if (response.status !== HTTP_STATUS_OK) {
          logData(
            `Expecting ${HTTP_STATUS_OK} response code, but got: ${response.status}`,
          );
          return false;
        }

        const executionGroup = JSON.parse(response.body);
        const lastTimestamp =
          executionGroup.readinessLogs[executionGroup.readinessLogs.length - 1]
            .earliestCollectedData;
        const dateTime = truncateTimeToHoursAsString(new Date(lastTimestamp));

        let currentTime = new Date();
        currentTime.setHours(currentTime.getHours() - 1);
        currentTime = truncateTimeToHoursAsString(currentTime);

        if (dateTime === currentTime) {
          retrieveKpisForSchema(group, dateTime, completedGroups);
        }
        if (completedGroups.length === EXECUTION_GROUPS_SIMPLE.length) {
          return true;
        }
      }
    }

    retryCount += 1;
    logData(
      `Calculated scheduled simple KPIs not found for previous hour. Sleeping for 30 seconds before retry: ${retryCount}`,
    );
    sleep(30);
  }

  logData('Failed to retrieve scheduled simple KPIs.');
  return false;
}

module.exports = {
  getKpiDefinitions,
  getCalculatedKPIValues,
  getPersistedKpiDefinitions,
  retrievePersistedComplexKpis,
  retrievePersistedSimpleKpis,
  getAllFdns,
};
