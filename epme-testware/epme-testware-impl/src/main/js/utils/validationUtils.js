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
import { logData } from '../modules/common.js';
import {
  EPME_SESSION_PAYLOAD,
  getKpiDefinitions,
  MO_ENABLED,
  RETRY_ATTEMPTS_FOR_MONITORING_OBJECT_STATE_CHANGE,
  DEGRADED,
  NOT_DEGRADED,
  NOT_POSSIBLE,
} from '../modules/epme-constants.js';
import {
  DMM_HTTP_REQUESTS,
  ENDPOINT_DMM,
  ENDPOINT_KPI_CALC_ID,
  EUTRANCELLFDD,
  EUTRANCELLTDD,
  HTTP_CODE_200,
  HTTP_CODE_202,
  KPI_HTTP_REQUESTS,
  METRIC_STATUS_SUCCEEDED,
  MO_TOTAL,
  ONE_SECOND_MS,
  PA_EXECUTION_TIME,
  STATUS_CREATED,
  STATUS_STOPPED,
  UL_PUSCH_SINR_HOURLY,
} from '../modules/constants.js';
import * as epmeDatabase from '../modules/epme-database.js';
import { sortVerdicts } from './sortUtils.js';

const JSESSION_ID_PATTERN = /JSESSIONID=[\d\w-]+/;

export function isStatusAccepted(status) {
  return status === 202;
}

export function isStatusOk(status) {
  return status === 200;
}

export function isStatusCreated(status) {
  return status === 201;
}

export function isStatusNoContent(status) {
  return status === 204;
}

export function isStatusBadRequest(status) {
  return status === 400;
}

export function isStatusNotFound(status) {
  return status === 404;
}

export function isStatusOkOrBadRequest(status) {
  return status === 200 || status === 400;
}

export function isStatusForbidden(status) {
  return status === 403;
}

export function isSessionValid(session) {
  return JSESSION_ID_PATTERN.test(session);
}

export function containsExpectedStatus(response) {
  const json = JSON.parse(response.body);
  return json.status === 'UP';
}

export function containsExpectedKPIsAndValues(
  responseBody,
  expectedResponseObject,
) {
  const responseBodyObject = JSON.parse(responseBody);
  if (expectedResponseObject.value.length !== responseBodyObject.value.length) {
    logData(
      `Number of FDNs with calculated KPIs are not equal between expected and actual response. Expected: ${expectedResponseObject.value.length}, Actual: ${responseBodyObject.value.length}`,
    );
    return false;
  }
  for (let i = 0; i < responseBodyObject.value.length; i += 1) {
    delete responseBodyObject.value[i].aggregation_begin_time;
    delete responseBodyObject.value[i].aggregation_end_time;
  }
  const expectedResponseValuesArray = [];
  const responseValuesArray = [];
  for (let i = 0; i < expectedResponseObject.value.length; i += 1) {
    const expectedResponseValueMapEntry = JSON.parse(
      JSON.stringify(expectedResponseObject.value[i]),
    );
    const responseValueMapEntry = JSON.parse(
      JSON.stringify(responseBodyObject.value[i]),
    );

    expectedResponseValuesArray.push(
      Object.entries(expectedResponseValueMapEntry),
    );
    responseValuesArray.push(Object.entries(responseValueMapEntry));
  }
  expectedResponseValuesArray.sort();
  responseValuesArray.sort();
  const mismatches = [];
  for (let i = 0; i < expectedResponseValuesArray.length; i += 1) {
    const expectedResponseValueMap = new Map(expectedResponseValuesArray[i]);
    const responseValueMap = new Map(responseValuesArray[i]);
    for (const [key, value] of expectedResponseValueMap) {
      if (responseValueMap.get(key) !== value) {
        const fdn = responseValueMap.get('fullFdn');
        const actual = responseValueMap.get(key);
        const resultData = `mis-match found: FDN: '${fdn}', KPI: '${key}', expected value: '${value}', actual: '${actual}`;
        mismatches.push(resultData);
        logData(resultData);
      }
    }
  }
  if (mismatches.length !== 0) {
    logData(`mis-matches: ${mismatches}`);
    return false;
  }

  return true;
}

function extractMoFromSchema(name) {
  const moType = name.split('_')[4];

  if (moType.includes('fdd')) {
    return EUTRANCELLFDD;
  }
  if (moType.includes('tdd')) {
    return EUTRANCELLTDD;
  }
  return moType;
}

function createKpiMap() {
  const allKpiDefinitions = getKpiDefinitions();

  const {
    aggregation_elements: defaultAggregationElements,
    kpi_definitions: kpiDefinitions,
  } = allKpiDefinitions.scheduled_complex.kpi_output_tables[0];

  const defaultMoType = extractMoFromSchema(defaultAggregationElements[0]);
  const kpiMap = {};

  for (const {
    aggregation_elements: aggregationElements,
    name: kpiName,
  } of kpiDefinitions) {
    let moType = defaultMoType;

    if (aggregationElements) {
      if (!kpiName.startsWith(UL_PUSCH_SINR_HOURLY)) {
        moType = extractMoFromSchema(aggregationElements[0]);
      } else if (kpiName.includes('fdd')) {
        moType = EUTRANCELLFDD;
      } else if (kpiName.includes('tdd')) {
        moType = EUTRANCELLTDD;
      }
    }

    if (kpiMap[moType]) {
      kpiMap[moType].push(kpiName);
    } else {
      kpiMap[moType] = [kpiName];
    }
  }

  return kpiMap;
}

function extractMoTypeFromMoFdn(moFdn) {
  const splitFdn = moFdn.split(',');
  return splitFdn[splitFdn.length - 1].split('=')[0].toLowerCase();
}

export function containsExpectedFdnsAndNonNullKpis(response) {
  const responseBody = JSON.parse(response);
  const cellKpis = responseBody.value ? responseBody.value : [];
  const fdnCount = cellKpis.length;

  if (fdnCount <= 0) {
    logData('There is no KPIs calculated for any FDNs');
    return false;
  }

  const kpiMap = createKpiMap();
  const processedMos = new Set();

  for (const cellKpi of cellKpis) {
    const moType = extractMoTypeFromMoFdn(cellKpi.fullFdn);

    if (!processedMos.has(moType)) {
      processedMos.add(moType);

      for (const kpi of kpiMap[moType]) {
        if (!(kpi in cellKpi)) {
          logData(
            `KPI ${kpi} for MO Type ${moType} not found in response body`,
          );
          return false;
        }

        if (!cellKpi[kpi]) {
          logData(
            `KPI ${kpi} for MO Type ${moType} has a value of null in response body`,
          );
          return false;
        }
      }
    }

    if (processedMos.size === Object.keys(kpiMap).length) {
      return true;
    }
  }

  logData(
    `Processed MOs ( ${processedMos} ) does not contains all required MOs from definitions ( ${Object.keys(
      kpiMap,
    )} )`,
  );

  return false;
}

export function containsExpectedSubscriptionCreatedResponse(response) {
  return response.body.includes('Subscription created successfully');
}

export function containsExpectedSubscriptionDeletedResponse(response) {
  return response.body.includes(
    'Subscription deactivated successfully and access has been revoked on the associated message bus topics',
  );
}

export function containsExpectedKpis(response) {
  const json = JSON.parse(response);
  const complexOutputTables = json.scheduled_complex.kpi_output_tables;
  const simpleOutputTables = json.scheduled_simple.kpi_output_tables;

  const kpiDefinitions = getKpiDefinitions();

  if (!complexOutputTables.length || !simpleOutputTables.length) {
    return false;
  }

  const actualSimpleKpis = new Set();
  const actualComplexKpis = new Set();

  simpleOutputTables.forEach(simpleKpi => {
    simpleKpi.kpi_definitions.forEach(kpi => {
      actualSimpleKpis.add(kpi.name);
    });
  });

  complexOutputTables.forEach(complexKpi => {
    complexKpi.kpi_definitions.forEach(kpi => {
      actualComplexKpis.add(kpi.name);
    });
  });

  const expectedSimpleKpis = kpiDefinitions.scheduled_simple.kpi_output_tables;
  const expectedComplexKpis =
    kpiDefinitions.scheduled_complex.kpi_output_tables;

  for (const simpleKpi of expectedSimpleKpis) {
    for (const kpi of simpleKpi.kpi_definitions) {
      if (!actualSimpleKpis.has(kpi.name)) {
        return false;
      }
    }
  }

  for (const complexKpi of expectedComplexKpis) {
    for (const kpi of complexKpi.kpi_definitions) {
      if (!actualComplexKpis.has(kpi.name)) {
        return false;
      }
    }
  }

  return true;
}

export function validateSessionBody(responseBody, expected) {
  const json = JSON.parse(responseBody);

  return (
    json.id.match(`PME-[a-fA-F\\d]+-${expected.sessionReference}`) &&
    json.sessionReference === expected.sessionReference &&
    json.epmeConfigId === expected.epmeConfigId &&
    json.duration === expected.duration &&
    json.status === 'CREATED' &&
    json.monitoringObjectTopicName === expected.monitoringObjectTopicName &&
    json.verdictTopicName === expected.verdictTopicName &&
    !!json.createdAt
  );
}

function getStartTime(timestamp) {
  const dateTime = new Date(timestamp);
  dateTime.setHours(dateTime.getHours() + 1);
  dateTime.setMinutes(0, 0, 0);
  return dateTime;
}

function getEndTime(timestamp) {
  const dateTime = new Date(timestamp);
  dateTime.setHours(dateTime.getHours() + EPME_SESSION_PAYLOAD.duration);
  return new Date(dateTime - ONE_SECOND_MS);
}

function isMonitoringObjectsPresent(monitoringObjects, expectedMos) {
  const processedMos = new Set();
  logData(`Processing ${monitoringObjects.length} Monitoring Objects`);

  monitoringObjects.forEach(object => {
    expectedMos.forEach(expectedObject => {
      const startTime = getStartTime(expectedObject.time).toISOString();
      const endTime = getEndTime(startTime).toISOString();
      const actualStartTime = new Date(
        object.start_time_formatted,
      ).toISOString();
      const actualEndTime = new Date(object.end_time_formatted).toISOString();

      if (
        object.fdn === expectedObject.fdn &&
        object.pme_session_id === expectedObject.pmeSessionId &&
        object.state === expectedObject.state &&
        object.state === MO_ENABLED &&
        actualStartTime === startTime &&
        actualEndTime === endTime &&
        !processedMos.has(object.fdn)
      ) {
        processedMos.add(object.fdn);
      }
    });
  });

  if (processedMos.size !== expectedMos.length) {
    logData(
      `Mismatch between number of processed Monitoring Objects ${processedMos.size} and expected Monitoring Objects ${expectedMos.length}`,
    );
    return false;
  }
  return true;
}

export function containsOnlyPersistedMonitoringObjects(
  expectedMos,
  sessionIdList,
) {
  const monitoringObjects = epmeDatabase.getMonitoringObjects(sessionIdList);

  if (monitoringObjects.length !== expectedMos.length) {
    logData(
      `Mismatch between expected Monitoring Objects ${expectedMos.length} and actual ${monitoringObjects.length}`,
    );
    return false;
  }
  return isMonitoringObjectsPresent(monitoringObjects, expectedMos);
}

export function containsPersistedMonitoringObjects(
  expectedMos,
  sessionIdList,
  maxRetries = 6,
) {
  let retryCount = 0;
  let validated = false;

  while (!validated && retryCount < maxRetries) {
    retryCount += 1;
    sleep(30);
    logData(
      `Attempt ${retryCount} / ${maxRetries} to verify monitoring objects present`,
    );

    const monitoringObjects = epmeDatabase.getMonitoringObjects(sessionIdList);
    validated = isMonitoringObjectsPresent(monitoringObjects, expectedMos);
  }

  return validated;
}

export function hasLastProcessedTimeUpdated(sessionIdList) {
  logData('Validating last processed time');
  let monitoringObjects = {};
  let retryCount = 0;
  while (retryCount < 30) {
    const invalidMos = new Set();
    monitoringObjects = epmeDatabase.getMonitoringObjects(sessionIdList);
    monitoringObjects.forEach(object => {
      const lastProcessedTime = new Date(
        object.last_processed_time_formatted,
      ).getTime();
      const startTime = new Date(object.start_time_formatted).getTime();
      if (lastProcessedTime < startTime) {
        invalidMos.add(object.fdn);
      }
    });
    if (invalidMos.size === 0) {
      return true;
    }
    retryCount += 1;
    logData(`Sleeping for 30 seconds before retry: ${retryCount}`);
    sleep(30);
  }

  logData(`Monitoring Objects have not got the correct last_processed_time`);
  return false;
}

function subtractHours(date, hours) {
  date.setHours(date.getHours() - hours);
  return date;
}

export function validateRetention(responseBody) {
  const json = JSON.parse(responseBody);
  const retentionDateTime = subtractHours(new Date(), 72);
  for (const session of json) {
    if (session.status === STATUS_CREATED) {
      const createdAtTime = new Date(session.createdAt);
      if (createdAtTime < retentionDateTime) {
        logData('createdAt time evaluated to earlier than retention date');
        logData('Retention time ', retentionDateTime);
        logData('createdAt time of session ', createdAtTime);
        return false;
      }
    } else {
      const stoppedAtTime = new Date(session.stoppedAt);
      if (stoppedAtTime < retentionDateTime) {
        logData('stoppedAt time evaluated to earlier than retention date');
        logData('Retention time ', retentionDateTime);
        logData('stoppedAt of session ', stoppedAtTime);
        return false;
      }
    }
  }
  return true;
}

export function hasMonitoringObjectStateUpdated(sessionIdList) {
  logData('Validating monitoring object state');
  let monitoringObjects = {};
  let retryCount = 0;

  while (retryCount < RETRY_ATTEMPTS_FOR_MONITORING_OBJECT_STATE_CHANGE) {
    const invalidMos = new Set();
    monitoringObjects = epmeDatabase.getMonitoringObjects(sessionIdList);

    monitoringObjects.forEach(object => {
      const currentState = object.state;
      if (currentState !== 'STOPPED') {
        invalidMos.add(object.fdn);
      }
    });

    if (invalidMos.size === 0) {
      return true;
    }

    retryCount += 1;
    logData(`Sleeping for 30 seconds before retry: ${retryCount}`);
    sleep(30);
  }

  logData('Monitoring Object states have not been updated to STOPPED');
  return false;
}

export function validateCreatedConfiguration(responseBody, expected) {
  const json = JSON.parse(responseBody);

  return (
    !!json.id &&
    json.name === expected.name &&
    json.weekendDays === expected.weekendDays &&
    json.fixedThresholdKpis.size === expected.fixedThresholdKpis.size
  );
}

export function validateGetAllConfigurations(responseBody) {
  const json = JSON.parse(responseBody);

  if (json.length > 0) {
    return true;
  }
  return false;
}

export function validateGetOneConfiguration(responseBody) {
  const json = JSON.parse(responseBody);

  return !!json.id;
}

const areDeeplyEqual = (obj1, obj2) => {
  if (obj1 === obj2) return true;

  if (Array.isArray(obj1) && Array.isArray(obj2)) {
    if (obj1.length !== obj2.length) return false;

    return obj1.every((elem, index) => areDeeplyEqual(elem, obj2[index]));
  }

  if (
    typeof obj1 === 'object' &&
    typeof obj2 === 'object' &&
    !!obj1 &&
    !!obj2
  ) {
    if (Array.isArray(obj1) || Array.isArray(obj2)) return false;

    const keys1 = Object.keys(obj1);
    const keys2 = Object.keys(obj2);

    if (
      keys1.length !== keys2.length ||
      !keys1.every(key => keys2.includes(key))
    ) {
      return false;
    }

    for (const key in obj1) {
      if (!areDeeplyEqual(obj1[key], obj2[key])) {
        return false;
      }
    }

    return true;
  }

  return false;
};

export const validateVerdictContents = (actual, expected) => {
  const sortedActual = sortVerdicts(actual);
  const sortedExpected = sortVerdicts(expected);

  logData('Actual verdicts:: ', sortedActual);
  logData('Expected verdicts:: ', sortedExpected);

  if (sortedActual.length !== sortedExpected.length) {
    logData(
      `Actual verdicts (${sortedActual.length}) is not the expected size (${sortedExpected.length})`,
    );
    return false;
  }

  return areDeeplyEqual(sortedActual, sortedExpected);
};

function checkKpiAndThresholdValues(kpiVerdict, status) {
  if (kpiVerdict.kpiValue === null && kpiVerdict.thresholdValue === null) {
    logData(
      'Validation of PME verdicts failed, null value found in verdict marked : ',
      status,
    );
    logData('Invalid Verdict: ', kpiVerdict);
    return false;
  }
  return true;
}

export function validateVerdicts(verdicts, numberOfMessages) {
  if (verdicts.length === 0) {
    logData(
      'Validation of PME verdicts failed, no verdicts have been consumed',
    );
    return false;
  }
  if (verdicts.length !== numberOfMessages) {
    logData(
      'Validation of PME verdicts failed, number of verdicts consumed does not equal the number of of messages sent',
    );
    return false;
  }

  const degraded = [];
  const notDegraded = [];
  for (const verdict of verdicts) {
    if (verdict.kpiVerdicts.length === 0) {
      logData(
        'No KPI verdicts are present in verdict message. Check the configuration used contained kpis and thresholds.',
      );
      return false;
    }
    for (const kpiVerdict of verdict.kpiVerdicts) {
      if (kpiVerdict.verdict === DEGRADED) {
        if (checkKpiAndThresholdValues(kpiVerdict, DEGRADED)) {
          degraded.push(kpiVerdict);
        } else {
          return false;
        }
      } else if (kpiVerdict.verdict === NOT_DEGRADED) {
        if (checkKpiAndThresholdValues(kpiVerdict, NOT_DEGRADED)) {
          notDegraded.push(kpiVerdict);
        } else {
          return false;
        }
      } else if (kpiVerdict.verdict !== NOT_POSSIBLE) {
        logData(
          'Validation of PME verdicts failed, verdict found to contain unsupported Verdict type',
        );
        return false;
      }
    }
  }

  if (degraded.length === 0 && notDegraded.length === 0) {
    logData(
      'Validation of PME verdicts failed, all verdicts were evaluated as not possible',
    );
    return false;
  }
  return true;
}

export function hasSessionStatusAndFinishTimeUpdated(
  responseBody,
  expectedPayloads,
) {
  const json = JSON.parse(responseBody);
  logData(`I am the body: ${json}`);
  for (const session of json) {
    for (const expected of expectedPayloads) {
      if (session.id.match(`PME-[a-fA-F\\d]+-${expected.sessionReference}`)) {
        if (
          session.status === 'FINISHED' &&
          session.stoppedAt !== null &&
          session.stoppedAt !== ''
        ) {
          return true;
        }
        logData(
          'Session not in FINISHED status or stoppedAt timestamp not as expected',
        );
        logData(
          `Actual session :: sessionId: ${session.id}, status: ${session.status}, stoppedAt: ${session.stoppedAt}`,
        );
        logData(
          `Expected session :: sessionId: ${expected.id}, status: FINISHED`,
        );
        return false;
      }
    }
  }
  logData(
    `session not found, expected session :: sessionReference: ${expectedPayloads.sessionReference}`,
  );
  return false;
}

export const validateSessionStatusStopped = response => {
  const json = JSON.parse(response);
  return json.status === STATUS_STOPPED && json.finished_at !== null;
};

function extractOperation(str, label) {
  const match = str.match(`${label}="([^"]*)"`);

  return match ? match[1] : null;
}

function validateHttpMetricsAreIncremented(
  response,
  metricName,
  uri,
  httpCode,
) {
  const bodyString = response.body.toString();
  const metricArray = bodyString.split('\n');

  const filteredData = metricArray.filter(str => {
    const name = str.split('{')[0];
    const endpoint = extractOperation(str, 'endpoint');
    const code = extractOperation(str, 'code');

    return (
      metricName.includes(name) &&
      uri.includes(endpoint) &&
      httpCode.includes(code)
    );
  });

  if (filteredData === undefined || filteredData.length === 0) {
    logData('Invalid input, one or more parameters do not match metrics');
    return false;
  }

  for (const str of filteredData) {
    const value = str.split(' ')[1];
    if (value <= 0) {
      logData('Metric with value not greater than zero:', str);
      return false;
    }
  }

  return true;
}

function validateCustomMetricsAreIncremented(response, metrics) {
  const bodyString = response.body.toString();
  const metricArray = bodyString.split('\n');

  const filteredData = metricArray.filter(str => {
    const name = str.split(' ')[0];

    return metrics.includes(name);
  });

  if (filteredData === undefined || filteredData.length === 0) {
    logData('Invalid input, one or more parameters do not match metrics');
    return false;
  }

  for (const str of filteredData) {
    const value = str.split(' ')[1];
    if (value <= 0) {
      logData('Metric with value not greater than zero:', str);
      return false;
    }
  }

  return true;
}

function validateCustomMetricsWithStatusAreIncremented(
  response,
  metrics,
  status,
) {
  const bodyString = response.body.toString();
  const metricArray = bodyString.split('\n');

  const filteredData = metricArray.filter(str => {
    const name = str.split('{')[0];
    const statusValue = extractOperation(str, 'status');

    return metrics.includes(name) && status.includes(statusValue);
  });

  if (filteredData === undefined || filteredData.length === 0) {
    logData('Invalid input, one or more parameters do not match metrics');
    return false;
  }

  for (const str of filteredData) {
    const value = str.split(' ')[1];
    if (value <= 0) {
      logData('Metric with value not greater than zero:', str);
      return false;
    }
  }

  return true;
}

export function validateMetricsAreIncremented(response) {
  if (!isStatusOk(response.status)) {
    logData('Status response failure: ', response.status);
    return false;
  }

  return (
    validateHttpMetricsAreIncremented(
      response,
      KPI_HTTP_REQUESTS,
      ENDPOINT_KPI_CALC_ID,
      HTTP_CODE_202,
    ) &&
    validateHttpMetricsAreIncremented(
      response,
      DMM_HTTP_REQUESTS,
      ENDPOINT_DMM,
      HTTP_CODE_200,
    ) &&
    validateCustomMetricsWithStatusAreIncremented(
      response,
      PA_EXECUTION_TIME,
      METRIC_STATUS_SUCCEEDED,
    ) &&
    validateCustomMetricsAreIncremented(response, MO_TOTAL)
  );
}

export function responseContainsLog(response) {
  const jsonString = JSON.parse(response.body.toString());
  logData('responseContainsLog', jsonString);
  const latestLogSource = jsonString.hits.hits;
  return !(latestLogSource.length === 0);
}
