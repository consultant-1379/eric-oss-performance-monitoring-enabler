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

import { check, group } from 'k6';
import * as pmsc from '../../modules/pmsch.js';
import {
  isStatusOk,
  containsExpectedKPIsAndValues,
  containsExpectedKpis,
  containsOnlyPersistedMonitoringObjects,
  hasLastProcessedTimeUpdated,
  validateVerdictContents,
} from '../../utils/validationUtils.js';
import * as epme from '../../modules/epme.js';
import * as kafka from '../../modules/kafka.js';

function verifyKpiDefinitionsPosted() {
  group('WHEN EPME sends down KPI, correctly sent and exist', () => {
    check(pmsc.getKpiDefinitions(), {
      'THEN status code is 200': r => isStatusOk(r.status),
      'AND response body contains correct KPI definitions': r =>
        containsExpectedKpis(r.body),
    });
  });
}

function verifyKpisCalculatedCorrectly() {
  group('WHEN KPIs are calculated, correct values', () => {
    check(pmsc.getCalculatedKPIValues(4), {
      'THEN status code is 200': r => isStatusOk(r.status),
      'AND response body contains expected KPIs and values': r =>
        containsExpectedKPIsAndValues(
          r.body,
          epme.getExpectedKPIResponseObject(),
        ),
    });
  });
}

function verifyMonitoringObjectPersistence() {
  group('WHEN monitoring objects are sent over kafka', () => {
    check(kafka.sendMonitoringObjects(epme.createMonitoringObjects()), {
      "THEN PME's database contains the persisted monitoring objects": r =>
        containsOnlyPersistedMonitoringObjects(r, epme.getCreatedSessionIds()),
    });
  });
}

function verifyPmeExecution() {
  group('WHEN a PME execution takes place', () => {
    check(hasLastProcessedTimeUpdated(epme.getCreatedSessionIds()), {
      'THEN monitoring objects have an updated last_processed_time': r =>
        r === true,
    });
  });
}

function verifyVerdicts() {
  group('WHEN reading verdicts from kafka', () => {
    check(kafka.consumeVerdicts(), {
      'THEN verdict results are valid': r =>
        validateVerdictContents(r, epme.getExpectedVerdicts()),
    });
  });
}

module.exports = {
  verifyKpiDefinitionsPosted,
  verifyKpisCalculatedCorrectly,
  verifyMonitoringObjectPersistence,
  verifyPmeExecution,
  verifyVerdicts,
};
