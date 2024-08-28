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

export const INGRESS_SCHEMA = __ENV.INGRESS_SCHEMA
  ? __ENV.INGRESS_SCHEMA
  : 'https';
export const EPME_INGRESS_SCHEMA = __ENV.EPME_INGRESS_SCHEMA
  ? __ENV.EPME_INGRESS_SCHEMA
  : INGRESS_SCHEMA;
export const INGRESS_HOST = __ENV.INGRESS_HOST
  ? __ENV.INGRESS_HOST
  : 'gas.kohn024-eiap.ews.gic.ericsson.se';
export const EPME_INGRESS_HOST = __ENV.EPME_INGRESS_HOST
  ? __ENV.EPME_INGRESS_HOST
  : INGRESS_HOST;
export const INGRESS_URL = `${INGRESS_SCHEMA}://${INGRESS_HOST}`;
export const INGRESS_ROUTES_USER_URI = '/idm/usermgmt/v1/users';
export const EPME_INGRESS_URL = `${EPME_INGRESS_SCHEMA}://${EPME_INGRESS_HOST}`;
// EIC URIs
export const INGRESS_LOGIN_URI = '/auth/v1/login';
export const INGRESS_DCC_URI = '/dmm-data-collection-controller';
export const INGRESS_DC_URI = '/dmm-data-catalog';
export const INGRESS_ROUTES_URI = '/v1/routes';
export const INGRESS_DC_SUBSCRIPTIONS_URI = '/catalog/v1/subscriptions';
export const INGRESS_SUBSCRIPTIONS_URI = '/subscription/v1';
export const INGRESS_DCC_SUBSCRIPTIONS =
  INGRESS_DCC_URI + INGRESS_SUBSCRIPTIONS_URI;
export const INGRESS_DC_SUBSCRIPTIONS =
  INGRESS_DC_URI + INGRESS_DC_SUBSCRIPTIONS_URI;
export const EPME_ROUTE_ID = 'eric-oss-performance-monitoring-enabler';
export const INGRESS_ROUTES_RBAC_URI = '/idm/rolemgmt/v1/extapp/rbac';
// EIC Users
export const INGRESS_LOGIN_USER = __ENV.INGRESS_LOGIN_USER
  ? __ENV.INGRESS_LOGIN_USER
  : 'gas-user';
export const INGRESS_LOGIN_PASSWORD = __ENV.INGRESS_LOGIN_PASSWORD
  ? __ENV.INGRESS_LOGIN_PASSWORD
  : 'idunEr!css0n';
export const INGRESS_PME_TESTWARE_USER = __ENV.INGRESS_PME_TESTWARE_USER
  ? __ENV.INGRESS_PME_TESTWARE_USER
  : 'pme-testware-user';
export const INGRESS_SESSION_OPERATOR = __ENV.INGRESS_SESSION_OPERATOR
  ? __ENV.INGRESS_SESSION_OPERATOR
  : 'pme-session-operator';
export const INGRESS_CONFIGURATION_OPERATOR =
  __ENV.INGRESS_CONFIGURATION_OPERATOR
    ? __ENV.INGRESS_CONFIGURATION_OPERATOR
    : 'pme-configuration-operator';
export const INGRESS_CONFIGURATION_READER = __ENV.INGRESS_CONFIGURATION_READER
  ? __ENV.INGRESS_CONFIGURATION_READER
  : 'pme-configuration-reader';

// EIC Roles
export const GAS_ACCESS_ROLE = 'System_ReadOnly';
// RAPPS
export const { RAPP_ID } = __ENV;

// Test Params
export const VALIDATE_EPME_RBAC = __ENV.VALIDATE_EPME_RBAC
  ? __ENV.VALIDATE_EPME_RBAC
  : false;
export const X_TENNANT = 'master';
export const APPLICATION_FORM_URL_ENCODED = 'application/x-www-form-urlencoded';
export const APPLICATION_JSON = 'application/json';

export const DEFAULT_TIMEOUT = 60;
export const MAX_RETRY = 10;
export const STATUS_FINISHED = 'FINISHED';
export const STATUS_CREATED = 'CREATED';
export const STATUS_STOPPED = 'STOPPED';
export const EXECUTION_GROUP_COMPLEX = 'pme_complex_group';

export const EXECUTION_GROUPS_METADATA = new Map([
  [
    '5G|PM_COUNTERS|NRCellDU_GNBDU_1',
    {
      table: 'kpi_pme_cell_simple_nrcelldu_60',
      fullFdnKpiName: 'fdn_nrcelldu',
    },
  ],
  [
    '5G|PM_COUNTERS|NRCellCU_GNBCUCP_1',
    {
      table: 'kpi_pme_cell_simple_nrcellcu_60',
      fullFdnKpiName: 'fdn_nrcellcu',
    },
  ],
  [
    '4G|PM_COUNTERS|EUtranCellTDD_1',
    {
      table: 'kpi_pme_cell_simple_tdd_60',
      fullFdnKpiName: 'fdn_tdd',
    },
  ],
  [
    '4G|PM_COUNTERS|EUtranCellFDD_1',
    {
      table: 'kpi_pme_cell_simple_fdd_60',
      fullFdnKpiName: 'fdn_fdd',
    },
  ],
]);

export const EXECUTION_GROUPS_SIMPLE = [...EXECUTION_GROUPS_METADATA.keys()];

export const TOTAL_FDN_COUNT = 574;
export const EUTRANCELLFDD = 'eutrancellfdd';
export const EUTRANCELLTDD = 'eutrancelltdd';
export const UL_PUSCH_SINR_HOURLY = 'ul_pusch_sinr_hourly';
export const ONE_SECOND_MS = 1000;
export const MO_SESSIONS_PARTITION = 2;
export const HTTP_STATUS_OK = 200;
export const HTTP_STATUS_ACCEPTED = 202;
export const METRIC_STATUS_SUCCEEDED = 'succeeded';

const getLoginParams = (user, password = INGRESS_LOGIN_PASSWORD) => ({
  headers: {
    'X-Login': user,
    'X-password': password,
    'X-tenant': X_TENNANT,
    'Content-Type': APPLICATION_FORM_URL_ENCODED,
  },
});

export const INGRESS_LOGIN_PARAMS = getLoginParams(INGRESS_LOGIN_USER);
export const INGRESS_PME_TESTWARE_PARAMS = getLoginParams(
  INGRESS_PME_TESTWARE_USER,
);
export const INGRESS_SESSION_OPERATOR_PARAMS = getLoginParams(
  INGRESS_SESSION_OPERATOR,
);
export const INGRESS_CONFIGURATION_OPERATOR_PARAMS = getLoginParams(
  INGRESS_CONFIGURATION_OPERATOR,
);
export const INGRESS_CONFIGURATION_READER_PARAMS = getLoginParams(
  INGRESS_CONFIGURATION_READER,
);

export const KPI_HTTP_REQUESTS =
  'pme_kpi_processing_http_requests_duration_seconds_count';
export const DMM_HTTP_REQUESTS =
  'pme_dmm_processing_http_requests_duration_seconds_count';
export const PA_EXECUTION_TIME = 'pme_pa_execution_time_hourly_seconds_count';
export const MO_TOTAL = 'pme_mo_count_total';

export const ENDPOINT_KPI_CALC = '/kpi-handling/calc/v1/calculations';
export const ENDPOINT_DMM = '/dmm-data-catalog/catalog';
