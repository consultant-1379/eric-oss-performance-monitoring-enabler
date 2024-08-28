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

export const EPME_PREFIX =
  __ENV.EPME_PREFIX !== undefined
    ? __ENV.EPME_PREFIX
    : '/performance-monitoring-enabler';

export const EPME_APP_CLIENT_ID = 'staging-test-client';
export const EPME_APP_CLIENT_SESSION_REFERENCE = `staging-execution-${__ENV.BUILD_ID}`;
export const EPME_APP_CLIENT_SESSION_REFERENCE_TWO = `staging-execution-two-${__ENV.BUILD_ID}`;
export const EPME_APP_CLIENT_SESSION_REFERENCE_THREE = `staging-execution-three-${__ENV.BUILD_ID}`;
export const EPME_HEALTH_URI = `${EPME_PREFIX}/actuator/health`;
export const EPME_PROMETHEUS_URI = `${EPME_PREFIX}/actuator/prometheus`;
export const EPME_HEALTH_STARTUP_URI = `${EPME_HEALTH_URI}/startup`;
export const EPME_API_VERSION = `/v1alpha3`;
export const SESSIONS_BASE_URI = `${EPME_PREFIX}${EPME_API_VERSION}/sessions`;
export const CLIENT_ID_PARAM = `clientId=${EPME_APP_CLIENT_ID}`;
export const EPME_SESSIONS_URI = `${SESSIONS_BASE_URI}?${CLIENT_ID_PARAM}`;

export const EPME_SESSION_STOP_PAYLOAD = {
  status: 'STOPPED',
};
export const SESSION_OBJECTS = [];
export const MO_ENABLED = 'ENABLED';
export const DATE_FORMAT_24H = 'YYYY-MM-DD HH24:MI:SS';
export const MONITORING_OBJECT_OFFSET = 7200000;
export const EPME_CONFIGURATION_NAME = 'CRUD_CONFIGURATION';
export const DEFAULT_EPME_CONFIGURATION_NAME =
  'sample_configuration_with_all_kpis_included';
export const RETRY_ATTEMPTS_FOR_MONITORING_OBJECT_STATE_CHANGE = 140;

export const ROUTE_WITH_PROMETHEUS =
  '../../resources/epme-prometheus-route.json';
export const ROUTE_WITHOUT_PROMETHEUS = '../../resources/epme-route.json';
export const EPME_CONFIGURATIONS_URI = `${EPME_PREFIX}${EPME_API_VERSION}/configurations`;
const EPME_CONFIGURATION_PAYLOAD_FILE = open(
  '../../resources/configuration_payload.json',
);
const EPME_CONFIGURATION = JSON.parse(EPME_CONFIGURATION_PAYLOAD_FILE);

const KPI_DEFINITIONS_FILE = open('../../resources/KpiDefinitions.json');
const KPI_DEFINITIONS = JSON.parse(KPI_DEFINITIONS_FILE);

export const getKpiDefinitionsFile = () => KPI_DEFINITIONS_FILE;
export const getKpiDefinitions = () => KPI_DEFINITIONS;
export const getEpmeConfiguration = () => EPME_CONFIGURATION;

export const getEpmeConfigurationForCrud = () => {
  const crudEpmeConfiguration = getEpmeConfiguration();
  crudEpmeConfiguration.name = EPME_CONFIGURATION_NAME;
  return crudEpmeConfiguration;
};

export const getEpmeUpdatedConfiguration = () => {
  const updatedEpmeConfiguration = getEpmeConfigurationForCrud();
  updatedEpmeConfiguration.weekendDays = 'FRIDAY,SATURDAY';
  return updatedEpmeConfiguration;
};

// EPME Database constants
const EPME_DB_USER =
  __ENV.EPME_DB_USER !== undefined ? __ENV.EPME_DB_USER : 'pme_user';

const EPME_DB_PASSWORD =
  __ENV.EPME_DB_PASSWORD !== undefined ? __ENV.EPME_DB_PASSWORD : 'admin';

const EPME_DB_HOST =
  __ENV.EPME_DB_HOST !== undefined
    ? __ENV.EPME_DB_HOST
    : 'eric-oss-performance-monitoring-enabler-pg';

const EPME_DB_PORT =
  __ENV.EPME_DB_PORT !== undefined ? __ENV.EPME_DB_PORT : 5432;

const EPME_DB_NAME =
  __ENV.EPME_DB_NAME !== undefined
    ? __ENV.EPME_DB_NAME
    : 'performance_monitoring';

export const POSTGRES_URL = `postgresql://${EPME_DB_USER}:${EPME_DB_PASSWORD}@${EPME_DB_HOST}:${EPME_DB_PORT}/${EPME_DB_NAME}?sslmode=disable`;

// Kafka Constants
export const KAFKA_TLS =
  __ENV.KAFKA_TLS !== undefined ? /true/i.test(`${__ENV.KAFKA_TLS}`) : true;

const KAFKA_BOOTSTRAP_SERVER =
  __ENV.KAFKA_BOOTSTRAP_SERVER !== undefined
    ? __ENV.KAFKA_BOOTSTRAP_SERVER
    : 'eric-oss-dmm-kf-op-sz-kafka-bootstrap';

const KAFKA_BOOTSTRAP_PORT =
  __ENV.KAFKA_BOOTSTRAP_PORT !== undefined ? __ENV.KAFKA_BOOTSTRAP_PORT : 9092;

export const KAFKA_URL = `${KAFKA_BOOTSTRAP_SERVER}:${KAFKA_BOOTSTRAP_PORT}`;

export const KAFKA_MONITORING_OBJECT_TOPIC =
  __ENV.KAFKA_MONITORING_OBJECT_TOPIC !== undefined
    ? __ENV.KAFKA_MONITORING_OBJECT_TOPIC
    : 'epme-monitoring-objects';

export const KAFKA_VERDICT_TOPIC =
  __ENV.KAFKA_VERDICT_TOPIC !== undefined
    ? __ENV.KAFKA_VERDICT_TOPIC
    : 'epme-verdicts';

const buildSessionPayload = sessionReference => ({
  sessionReference,
  pmeConfigId: 1,
  duration: 1,
  verdictTopicName: KAFKA_VERDICT_TOPIC,
  monitoringObjectTopicName: KAFKA_MONITORING_OBJECT_TOPIC,
});

export const EPME_SESSION_PAYLOAD = buildSessionPayload(
  EPME_APP_CLIENT_SESSION_REFERENCE,
);
export const EPME_SESSION_TWO_PAYLOAD = buildSessionPayload(
  EPME_APP_CLIENT_SESSION_REFERENCE_TWO,
);
export const EPME_SESSION_THREE_PAYLOAD = buildSessionPayload(
  EPME_APP_CLIENT_SESSION_REFERENCE_THREE,
);

export const MONITORING_OBJECTS_SCHEMA = open(
  '../../resources/MonitoringObjects.avsc',
);

// Log search constants
export const SEARCH_ENGINE_ENTRIES_URI = '/_search?pretty&size=1';
export const FILTER_PAYLOAD = {
  query: {
    bool: {
      filter: [
        {
          match_phrase: {
            service_id: 'rapp-eric-oss-performance-monitoring-enabler',
          },
        },
        {
          range: {
            timestamp: {
              gt: 'now-5m',
            },
          },
        },
      ],
    },
  },
};

export const AUDIT_LOG_FILTER_PAYLOAD = {
  query: {
    bool: {
      filter: [
        {
          query_string: {
            query: '(facility:"log audit")',
          },
        },
        {
          match_phrase: {
            service_id: 'rapp-eric-oss-performance-monitoring-enabler',
          },
        },
        {
          range: {
            timestamp: {
              gt: 'now-5m',
            },
          },
        },
      ],
    },
  },
};

export const VERDICT_SCHEMA = open('../../resources/Verdict.avsc');
export const DEGRADED = 'DEGRADED';
export const NOT_DEGRADED = 'NOT_DEGRADED';
export const NOT_POSSIBLE = 'NOT_POSSIBLE';
