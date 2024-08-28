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

import * as epme from '../../modules/epme.js';
import {
  EPME_SESSION_PAYLOAD,
  EPME_SESSION_THREE_PAYLOAD,
  EPME_SESSION_TWO_PAYLOAD,
  getEpmeConfigurationForCrud,
  getEpmeUpdatedConfiguration,
  FILTER_PAYLOAD,
  AUDIT_LOG_FILTER_PAYLOAD,
} from '../../modules/epme-constants.js';
import * as dmm from '../../modules/dmm.js';
import * as pmsch from '../../modules/pmsch.js';
import * as kafka from '../../modules/kafka.js';

import {
  isStatusOk,
  isStatusCreated,
  isStatusNoContent,
  containsExpectedSubscriptionCreatedResponse,
  containsExpectedSubscriptionDeletedResponse,
  containsExpectedKpis,
  validateRetention,
  validateCreatedConfiguration,
  validateGetAllConfigurations,
  validateGetOneConfiguration,
  containsExpectedFdnsAndNonNullKpis,
  hasMonitoringObjectStateUpdated,
  hasSessionStatusAndFinishTimeUpdated,
  isStatusAccepted,
  validateSessionStatusStopped,
  validateSessionBody,
  containsPersistedMonitoringObjects,
  hasLastProcessedTimeUpdated,
  validateVerdicts,
  isStatusNotFound,
  validateMetricsAreIncremented,
  responseContainsLog,
} from '../../utils/validationUtils.js';
import * as gateway from '../../modules/api-gateway.js';

function verifyEpmeApiOnboarding() {
  group('Delete EPME Route if exists', () => {
    check(gateway.offboardEpmeApis(), {
      'THEN Expect 404 OR 204': r =>
        isStatusNotFound(r.status) || isStatusNoContent(r.status),
    });
  });

  group('Create EPME Route', () => {
    check(gateway.onboardEpmeApis(), {
      'THEN Expect 201': r => isStatusCreated(r.status),
    });
  });
  gateway.clearSession();
}

function putIDSFile() {
  let body = [];
  group('WHEN Check Subscriptions for rApp', () => {
    check(dmm.getRAppSubscriptions(), {
      'THEN status code is 200': r => {
        body = JSON.parse(r.body);
        return isStatusOk(r.status);
      },
    });
  });

  if (body.length > 0) {
    group('WHEN existing Subscriptions for rApp are deleted', () => {
      check(dmm.deleteIDSFile(), {
        'THEN status code is 200': r => isStatusOk(r.status),
        'AND response message is Deleted': r =>
          containsExpectedSubscriptionDeletedResponse(r),
      });
    });
  }

  group('WHEN IDS file is sent down to DCC', () => {
    check(dmm.putIDSFile(dmm.getIDSFileBody()), {
      'THEN status code is 200': r => isStatusOk(r.status),
      'AND response message is Created': r =>
        containsExpectedSubscriptionCreatedResponse(r),
    });
  });
}

function verifySessionEndpointReady() {
  epme.cleanSession();
  epme.getSessionOperatorSessionId();
  group('WHEN GET EPME Session', () => {
    check(epme.getSessionEndpointStatus(), {
      'THEN status code is 200': r => isStatusOk(r.status),
    });
  });
  epme.cleanSession();
}

function verifyPersistedKpiDefinitions() {
  group('WHEN KPI definitions are persisted', () => {
    check(pmsch.getPersistedKpiDefinitions(), {
      'THEN status code is 200': r => isStatusOk(r.status),
      'AND response body contains the posted KPI definitions': r =>
        containsExpectedKpis(r.body),
    });
  });
}

function verifyKpisCalculated() {
  group('WHEN KPIs are calculated', () => {
    check(pmsch.retrievePersistedComplexKpis(), {
      'THEN status code is 200': r => isStatusOk(r.status),
      'AND response body contains the expected FDNs and non null KPIs': r =>
        containsExpectedFdnsAndNonNullKpis(r.body),
    });
  });
}

function verifySimpleKpisAndRetrieveMoFdn() {
  group('WHEN simpleKpis are calculated', () => {
    check(pmsch.retrievePersistedSimpleKpis(), {
      'THEN kpis for all simple groups are found': r => r === true,
    });
  });
}

function verifyEpmeSession() {
  epme.getSessionOperatorSessionId();
  group('WHEN Create EPME sessions', () => {
    check(epme.createEpmeSession(EPME_SESSION_PAYLOAD), {
      'THEN Session is created': r => isStatusCreated(r.status),
      'AND response body contains expected content': r =>
        validateSessionBody(
          r.body,
          epme.getSessionPayloadWithConfigId(EPME_SESSION_PAYLOAD),
        ),
    });
  });
  epme.cleanSession();
}

function verifySecondEpmeSession() {
  epme.getSessionOperatorSessionId();
  group('WHEN Create Second EPME sessions', () => {
    check(epme.createEpmeSession(EPME_SESSION_TWO_PAYLOAD), {
      'THEN Session is created': r => isStatusCreated(r.status),
      'AND response body contains expected content': r =>
        validateSessionBody(
          r.body,
          epme.getSessionPayloadWithConfigId(EPME_SESSION_TWO_PAYLOAD),
        ),
    });
  });
  epme.cleanSession();
}

function verifyThirdEpmeSession() {
  epme.getSessionOperatorSessionId();
  group('WHEN Create Third EPME sessions', () => {
    check(epme.createEpmeSession(EPME_SESSION_THREE_PAYLOAD), {
      'THEN Session is created': r => isStatusCreated(r.status),
      'AND response body contains expected content': r =>
        validateSessionBody(
          r.body,
          epme.getSessionPayloadWithConfigId(EPME_SESSION_THREE_PAYLOAD),
        ),
    });
  });
  epme.cleanSession();
}

function verifyTopicsReady() {
  group('WHEN epme-tesware connects to kafka', () => {
    check(kafka.getTopics(), {
      'THEN epme-monitoring-objects is available': t =>
        t.includes('epme-monitoring-objects'),
      'AND epme-verdicts is available': t => t.includes('epme-verdicts'),
    });
  });
}

function verifyProduceMonitoringObjects() {
  group('WHEN monitoring objects are sent over kafka', () => {
    check(
      kafka.sendMonitoringObjects(
        epme.createMonitoringObjectsFromFdns(pmsch.getAllFdns()),
      ),
      {
        "THEN PME's database contains the persisted monitoring objects": r =>
          containsPersistedMonitoringObjects(r, epme.getCreatedSessionIds()),
      },
    );
  });
}

function verifyGetEpmeSessionById() {
  epme.getSessionOperatorSessionId();
  group('WHEN Get EPME session by Id', () => {
    check(epme.getSessionById(), {
      'THEN the status code is 200': r => isStatusOk(r.status),
      'AND response body contains expected content': r =>
        validateSessionBody(r.body, EPME_SESSION_PAYLOAD),
    });
  });
  epme.cleanSession();
}

function verifyEpmeSessionRetention() {
  epme.getSessionOperatorSessionId();
  group('WHEN Get EPME sessions', () => {
    check(epme.getAllEpmeSessions(), {
      'THEN the status code is 200': r => isStatusOk(r.status),
      'AND response body contains no sessions older than retention period': r =>
        validateRetention(r.body),
    });
  });
  epme.cleanSession();
}

function verifyCreateSessionConfiguration() {
  epme.getConfigurationOperatorSessionId();
  group('WHEN Create EPME configuration', () => {
    check(epme.createEpmeConfiguration(getEpmeConfigurationForCrud()), {
      'THEN Configuration is created': r => isStatusCreated(r.status),
      'AND response body contains expected content': r =>
        validateCreatedConfiguration(r.body, getEpmeConfigurationForCrud()),
    });
  });
  epme.cleanSession();
}

function verifyUpdateSessionConfiguration() {
  epme.getConfigurationOperatorSessionId();
  group('WHEN Update EPME configuration', () => {
    check(epme.updateEpmeConfiguration(), {
      'THEN Configuration is update': r => isStatusOk(r.status),
      'AND response body contains expected content': r =>
        validateCreatedConfiguration(r.body, getEpmeUpdatedConfiguration()),
    });
  });
  epme.cleanSession();
}

function verifyDeleteSessionConfiguration() {
  epme.getConfigurationOperatorSessionId();
  group('WHEN Delete EPME configuration', () => {
    check(epme.deleteSessionConfiguration(), {
      'THEN status code is 204': r => isStatusNoContent(r.status),
    });
  });
  epme.cleanSession();
}

function verifyGetAllSessionConfigurations() {
  epme.getConfigurationOperatorSessionId();
  group('WHEN Get all EPME configurations', () => {
    check(epme.getAllSessionConfigurations(), {
      'THEN status code is 200': r => isStatusOk(r.status),
      'AND response body contains expected content': r =>
        validateGetAllConfigurations(r.body),
    });
  });
  epme.cleanSession();
}

function verifyGetOneSessionConfiguration() {
  epme.getConfigurationOperatorSessionId();
  group('WHEN Get one EPME configuration', () => {
    check(epme.getOneSessionConfiguration(), {
      'THEN status code is 200': r => isStatusOk(r.status),
      'AND response body contains expected content': r =>
        validateGetOneConfiguration(r.body),
    });
  });
  epme.cleanSession();
}
function verifySessionFinished() {
  epme.cleanSession();
  epme.getSessionOperatorSessionId();
  group('WHEN PME sessions are FINISHED', () => {
    check(hasMonitoringObjectStateUpdated(epme.getCreatedSessionIds()), {
      'THEN Monitoring Objects states updated': r => r === true,
    });
    epme.cleanSession();
    epme.getSessionOperatorSessionId();
    check(hasLastProcessedTimeUpdated(epme.getCreatedSessionIds()), {
      'THEN monitoring objects have an updated last_processed_time': r =>
        r === true,
    });
    epme.cleanSession();
    epme.getSessionOperatorSessionId();
    check(epme.getAllEpmeSessions(), {
      'THEN Status and Finish Time of Sessions updated': r =>
        hasSessionStatusAndFinishTimeUpdated(r.body, [
          EPME_SESSION_PAYLOAD,
          EPME_SESSION_TWO_PAYLOAD,
        ]),
    });
  });
  epme.cleanSession();
}

function verifyVerdicts() {
  group('WHEN reading verdicts from kafka', () => {
    check(kafka.consumeVerdicts(), {
      'THEN verdict results are valid': r =>
        validateVerdicts(r, kafka.getNumberOfMessages()),
    });
  });
}

function verifyStopSession() {
  epme.getSessionOperatorSessionId();
  group('WHEN Stop session by session ID and client ID', () => {
    check(epme.stopEpmeSession(), {
      'THEN status code is 202': r => isStatusAccepted(r.status),
      'AND response body contains session with updated status': r =>
        validateSessionStatusStopped(r.body),
    });
  });
  epme.cleanSession();
}

function verifyRoutesUpdated() {
  group('WHEN Update EPME Route', () => {
    check(gateway.onboardEpmePrometheusApis(), {
      'THEN Expect 200': r => isStatusOk(r.status),
    });
  });
  gateway.clearSession();
}

function verifyMetricsAreIncremented() {
  group('WHEN reading metrics from prometheus', () => {
    check(epme.getMetrics(), {
      'THEN status code is 200': r => isStatusOk(r.status),
      'THEN metrics are incremented': r => validateMetricsAreIncremented(r),
    });
  });
}

function verifyLogStreamingIsSuccessful() {
  epme.getSessionOperatorSessionId();
  group('WHEN Logs are streamed', () => {
    check(epme.getLogsWithFilter(FILTER_PAYLOAD), {
      'THEN status code is 200': r => isStatusOk(r.status),
      'AND response body contains logs for the right service id in the past 5 minutes':
        r => responseContainsLog(r),
    });
  });
  epme.cleanSession();
}

function verifyAuditLogIsSuccessful() {
  epme.getSessionOperatorSessionId();
  group('WHEN audit Logs are streamed', () => {
    check(epme.getLogsWithFilter(AUDIT_LOG_FILTER_PAYLOAD), {
      'THEN status code is 200': r => isStatusOk(r.status),
      'AND response body contains an audit log for the correct service id in the past 5 minutes':
        r => responseContainsLog(r),
    });
  });
  epme.cleanSession();
}

module.exports = {
  verifySessionEndpointReady,
  putIDSFile,
  verifyPersistedKpiDefinitions,
  verifySimpleKpisAndRetrieveMoFdn,
  verifyKpisCalculated,
  verifyGetEpmeSessionById,
  verifyEpmeSession,
  verifySecondEpmeSession,
  verifyThirdEpmeSession,
  verifyEpmeSessionRetention,
  verifyCreateSessionConfiguration,
  verifyUpdateSessionConfiguration,
  verifyDeleteSessionConfiguration,
  verifyGetAllSessionConfigurations,
  verifyGetOneSessionConfiguration,
  verifySessionFinished,
  verifyVerdicts,
  verifyStopSession,
  verifyTopicsReady,
  verifyProduceMonitoringObjects,
  verifyEpmeApiOnboarding,
  verifyRoutesUpdated,
  verifyMetricsAreIncremented,
  verifyLogStreamingIsSuccessful,
  verifyAuditLogIsSuccessful,
};
