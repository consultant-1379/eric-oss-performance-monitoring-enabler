/*******************************************************************************
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
 ******************************************************************************/

package com.ericsson.oss.apps.util;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.ericsson.oss.apps.api.model.EpmeWeekendDays;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Constants {

    public static final String VERSION = "/v1alpha3";
    public static final String COLON = ":";
    public static final String EMPTY_STRING = "";

    //PMSC
    public static final String PMSC = "pmsc";
    public static final String PMSQS = "pmsqs";
    public static final String KPI = "kpi";
    public static final String PME_CELL_COMPLEX = "kpi_pme_cell_complex_60";
    public static final String ORDER_BY_FDN = "fullFdn asc";
    public static final String JSON = "json";

    //IAM ACCESS
    public static final String HTTPS = "https";
    public static final String IAM = "iam";
    public static final String JSON_PROPERTY_ACCESS_TOKEN = "access_token";
    public static final String JSON_PROPERTY_EXPIRES_IN = "expires_in";
    public static final String CLIENT_CREDENTIALS = "client_credentials";
    public static final String GRANT_TYPE = "grant_type";
    public static final String CLIENT_ID = "client_id";
    public static final String CLIENT_SECRET = "client_secret";

    //DMM Data Discovery Access
    public static final String DMM_DATA_DISCOVERY = "data-discovery";
    public static final String RAPP_ID = "rAppId";
    public static final String SUBSCRIPTION_STATUS_ACTIVE = "ACTIVE";
    public static final List<String> PME_SUBSCRIPTION_NAMES = List.of(
            "pme-nrcelldu-subscription",
            "pme-nrcellcu-subscription",
            "pme-ep-ngu-subscription",
            "pme-eutrancell-fdd-subscription",
            "pme-eutrancell-tdd-subscription");

    //DMM Kafka Access
    public static final String MONITORING_OBJECT_TOPIC = "epme-monitoring-objects";
    public static final String VERDICT_TOPIC = "epme-verdicts";

    //Connection
    public static final int MAX_TOTAL_CONNECTIONS_PER_ROUTE = 20;
    public static final int MAX_TOTAL_CONNECTIONS = 50;
    public static final int CONNECT_TIMEOUT_SECONDS = 30;
    public static final int REQUEST_TIMEOUT_SECONDS = 10;
    public static final int SOCKET_TIMEOUT_SECONDS = 60;
    public static final int CONNECTION_TIME_TO_LIVE_SECONDS = 30;
    public static final String STANDARD_COOKIE_SPEC = "standard";

    // Certificate management
    public static final int CERT_FILE_CHECK_SCHEDULE_IN_SECONDS = 300;
    public static final String JKS = "JKS";

    // TIMEZONE
    public static final String UTC = "UTC";

    // Validation
    public static final String VALIDATION_FAILED = "Validation Failed";
    public static final String CLIENT_ID_NON_NULL = "Client ID cannot be null";
    public static final String CLIENT_ID_PATTERN = "Client ID must contain only alphanumeric, underscore or dash characters";
    public static final String CLIENT_ID_LENGTH = "Client ID must only have length of 4 - 64 characters";
    public static final String SESSION_NON_NULL = "Session cannot be null";
    public static final String SESSION_REFERENCE_NON_NULL = "Session reference cannot be null";
    public static final String SESSION_REFERENCE_PATTERN = "Session reference must contain only alphanumeric, underscore or dash characters";
    public static final String SESSION_REFERENCE_LENGTH = "Session reference must only have length of 4 - 64 characters";
    public static final String SESSION_DURATION_RANGE = "Session duration must be between 1 - 24 hours";
    public static final String SESSION_CONFIG_ID_NON_NULL = "PME Config ID cannot be null";
    public static final String SESSION_ALREADY_EXISTS = "Session already exists for Client ID and Session reference";
    public static final String SESSION_DOES_NOT_EXIST = "Session with provided ID does not exist";
    public static final String SESSION_STATUS_INVALID = "Session status must have value STOPPED";
    public static final String HOURLY_EXECUTION_WILL_COMPLETE = "Any ongoing hourly execution for this session will complete";
    public static final String SESSION_IS_STOPPED = "Session is already stopped";
    public static final String SESSION_UPDATE_FAILED = "Session failed to update";
    public static final String PME_CONFIG_ID_DOES_NOT_EXIST = "Provided PME Config ID does not exist";
    public static final String DELETE_FAILED = "Failed to delete from database";
    public static final String UPDATE_FAILED = "Failed to update database";
    public static final String GET_FAILED = "Failed to read configuration from database";
    public static final String PROVIDE_A_VALID_CLIENT_ID_AND_SESSION_ID = "Provide a valid Client ID and Session ID";
    public static final String PROVIDE_A_VALID_CONFIGURATION_ID = "Provide a valid Configuration ID";
    public static final String CONFIGURATION_ASSOCIATED_WITH_SESSION_DELETE = "Only Configurations not associated with a Session can be deleted";
    public static final String CONFIGURATION_ASSOCIATED_WITH_SESSION_UPDATE = "Only Configurations not associated with a Session can be updated";
    public static final String CONFIGURATION_USED = "Configuration with provided ID is used in a Session";
    public static final String CONFIGURATION_DOES_NOT_EXIST = "Configuration with provided ID does not exist";
    public static final String CONFIGURATION_NAME_ALREADY_EXISTS = "Configuration name is already used in an existing configuration";
    public static final String CONFIGURATION_NAME_CANNOT_BE_CHANGED = "Configuration name cannot be changed";
    public static final String CONFIGURATION_ID_CANNOT_BE_CHANGED = "Configuration id cannot be changed";

    public static final String CONFIG_NAME_NON_NULL = "Configuration name cannot be null";
    public static final String CONFIG_NAME_INVALID_PATTERN = "Configuration name must contain only alphanumeric, underscore or dash characters";
    public static final String CONFIG_NAME_INVALID_LENGTH = "Configuration name must only have length of 1 - 255 characters";
    public static final String WEEKEND_DAYS_NON_NULL = "The weekendDays cannot be null";
    public static final String WEEKEND_DAYS_INVALID_VALUE = "The weekendDays must be one of " + Arrays.stream(EpmeWeekendDays.values()).toList();
    public static final String FIXED_THRESHOLD_KPIS_NON_NULL_OR_EMPTY = "The fixedThresholdKpis cannot be null or empty";
    public static final String FIXED_THRESHOLD_VALUE_NON_NULL = "The fixedThreshold cannot be null";
    public static final String MAX_CONFIGURATIONS_EXCEEDED = "The maximum number of Configurations cannot exceed: ";
    public static final String MAX_KPIS_PER_CONFIGURATIONS_EXCEEDED = "The maximum number of kpis in a Configuration cannot exceed: ";
    public static final String ENDC_SETUP_SUCCESS_RATE_CAPTURED_GNODEB = "en_dc_setup_sr_captured_gnodeb_hourly";
    public static final String DIFFERENTIATED_INITAL_ERAB_ESTABLISHMENT_SR_ENODEB = "diff_initial_erab_establishment_sr_enodeb_hourly";
    public static final String SCG_ACTIVE_RADIO_RESOURCE_RETAINABILITY_GNODEB = "scg_active_radio_resource_retainability_gnodeb_hourly";
    public static final String AVG_DL_MAC_DRB_THROUGHPUT = "avg_dl_mac_drb_throughput_hourly";
    public static final String AVG_UL_MAC_UE_THROUGHPUT = "avg_ul_mac_ue_throughput_hourly";
    public static final String NORMALIZED_AVG_DL_MAC_CELL_THROUGHPUT_TRAFFIC_HOURLY = "normalized_avg_dl_mac_cell_throughput_traffic_hourly";
    public static final String ENDC_PS_CELL_CHANGE_SUCCESS_RATE = "endc_ps_cell_change_success_rate_hourly";
    public static final String NR_TO_LTE_INTER_RAT_HANDOVER_SR_GNODEB_HOURLY = "nr_to_lte_inter_rat_handover_sr_gnodeb_hourly";
    public static final String NR_HANDOVER_SUCCESS_RATE_GNODEB_HOURLY = "nr_handover_success_rate_gnodeb_hourly";
    public static final String PARTIAL_CELL_AVAILABILITY_GNODEB_HOURLY = "partial_cell_availability_gnodeb_hourly";
    public static final String CELL_AVAILABILITY_HOURLY = "cell_availability_hourly";
    public static final String INITIAL_AND_ADDED_E_RAB_ESTABLISHMENT_SR_HOURLY = "initial_and_added_e_rab_establishment_sr_hourly";
    public static final String AVG_UL_PDCP_UE_THROUGHPUT_CELL = "avg_ul_pdcp_ue_throughput_cell_hourly";
    public static final String AVG_DL_PDCP_UE_THROUGHPUT_CELL = "avg_dl_pdcp_ue_throughput_cell_hourly";
    public static final String UL_PUSCH_SINR_HOURLY = "ul_pusch_sinr_hourly";
    public static final String CELL_HANDOVER_SUCCESS_RATE_HOURLY = "cell_handover_success_rate_hourly";
    public static final String E_RAB_RETAINABILITY_PERCENTAGE_LOST_HOURLY = "e_rab_retainability_percentage_lost_hourly";
    public static final String AVG_DL_LATENCY_HOURLY = "avg_dl_latency_hourly";
    public static final String VOIP_CELL_INTEGRITY_HOURLY = "voip_cell_integrity_hourly";

    public static final String KPI_VALUE_NOT_IN_RANGE = "The fixedThreshold value for kpi '%s' is not in the range %s";
    public static final String KPI_VALUE_INVALID_STEP_SIZE = "The fixedThreshold value for kpi '%s' must have a step size of %s";
    public static final String KPI_NAME_INVALID = "The kpi '%s' is not a valid kpiName, it must be one of %s";

    // Server errors
    public static final String INTERNAL_SERVER_ERROR = "Internal Server Error";
    public static final String PERSISTENCE_FAILED = "Failed to persist change to the database";
    public static final String UNEXPECTED_ERROR = "Unexpected error";

    // LOG CONTROL FILE WATCHER
    public static final int LOG_CONTROL_FILE_CHECK_SCHEDULE_IN_SECONDS = 40;
    public static final String FACILITY_KEY = "facility";
    public static final String NON_AUDIT_LOG = "security/authorization messages";
    public static final String SUBJECT_KEY = "subject";
    public static final String AUDIT_LOG = "log audit";

    @UtilityClass
    public static final class MetricConstants {
        public static final String TAG = "tag";
        public static final String DESCRIPTION = "description";
        public static final String REGISTER = "register";
        public static final String CODE = "code";
        public static final String ENDPOINT = "endpoint";
        public static final String METHOD = "method";
        public static final String STATUS = "status";
        public static final String FAILED = "failed";
        public static final String SKIPPED = "skipped";
        public static final String SUCCEEDED = "succeeded";
        public static final String URI = "uri";

        public static final String TOKEN_GATEWAY_ENDPOINT = "/auth/realms/master/protocol/openid-connect/token";
        public static final String KPI_ENDPOINT = "/kpi-handling";
        public static final String DMM_ENDPOINT = "/dmm-data-catalog/catalog";
        public static final String API_GATEWAY_ENDPOINT = "/auth/v1/login";

        public static final String CONFIGURATION_ENDPOINT = VERSION + "/configurations";
        public static final String CONFIGURATION_ID_ENDPOINT = VERSION + "/configurations/{configurationId}";
        public static final String SESSION_ENDPOINT = VERSION + "/sessions/";
        public static final String SESSION_ID_ENDPOINT = VERSION + "sessions/{sessionId}";
        public static final String SESSION_ID_STATUS_ENDPOINT = VERSION + "/sessions/{sessionId}/status";

        public static final String HTTP_SERVER_REQUEST = "http.server.requests";
        public static final String HTTP_CLIENT_REQUEST = "http.client.requests";

        public static final String APIGATEWAY_SESSIONID_HTTP_REQUESTS = "apigateway.sessionid.http.requests";
        public static final String KPI_HTTP_REQUESTS = "kpi.processing.http.requests";
        public static final String DMM_HTTP_REQUESTS = "dmm.processing.http.requests";
        public static final String TOKEN_GATEWAY_REQUESTS = "tokengateway.http.requests";
        public static final String PERFORMANCE_MONITOR_PREFIX = "pme.";
        public static final String KPI_HTTP_REQUESTS_DURATION_SECONDS = PERFORMANCE_MONITOR_PREFIX
                + "kpi.processing.http.requests.duration.seconds";
        public static final String DMM_HTTP_REQUESTS_DURATION_SECONDS = PERFORMANCE_MONITOR_PREFIX
                + "dmm.processing.http.requests.duration.seconds";
        public static final String APIGATEWAY_SESSIONID_HTTP_REQUESTS_DURATION_SECONDS = PERFORMANCE_MONITOR_PREFIX
                + "apigateway.sessionid.http.requests.duration.seconds";
        public static final String TOKENGATEWAY_HTTP_REQUESTS_DURATION_SECONDS = PERFORMANCE_MONITOR_PREFIX
                + "tokengateway.http.requests.duration.seconds";
        public static final String PME_HTTP_REQUESTS_DURATION_SECONDS = PERFORMANCE_MONITOR_PREFIX
                + "http.requests.duration.seconds";
        public static final String ACTUATOR_PREFIX = PERFORMANCE_MONITOR_PREFIX + "act.";
        public static final String PME_SESSION_CREATED_COUNT = PERFORMANCE_MONITOR_PREFIX + "session.created.count";
        public static final String PME_SESSION_STARTED_COUNT = PERFORMANCE_MONITOR_PREFIX + "session.started.count";
        public static final String PME_SESSION_FINISHED_COUNT = PERFORMANCE_MONITOR_PREFIX + "session.finished.count";
        public static final String PME_SESSION_STOPPED_COUNT = PERFORMANCE_MONITOR_PREFIX + "session.stopped.count";
        public static final String PME_VERDICT_DEGRADED_COUNT = PERFORMANCE_MONITOR_PREFIX + "verdict.degraded.count";
        public static final String PME_VERDICT_NOT_DEGRADED_COUNT = PERFORMANCE_MONITOR_PREFIX + "verdict.not.degraded.count";
        public static final String PME_VERDICT_NOT_POSSIBLE_COUNT = PERFORMANCE_MONITOR_PREFIX + "verdict.not.possible.count";
        public static final String PME_MO_KPI_RETRIEVAL_COUNT = PERFORMANCE_MONITOR_PREFIX + "mo.kpi.retrieval.count";
        public static final String PME_MO_KPI_NULL_COUNT = PERFORMANCE_MONITOR_PREFIX + "mo.kpi.null.count";
        public static final String PME_MO_MONITORED_COUNT = PERFORMANCE_MONITOR_PREFIX + "mo.monitored.count";
        public static final String PME_MO_MONITORED_LOOKBACK_COUNT = PERFORMANCE_MONITOR_PREFIX + "mo.monitored.lookback.count";
        public static final String PME_MO_COUNT = PERFORMANCE_MONITOR_PREFIX + "mo.count";
        public static final String PME_MO_DISCARDED_COUNT = PERFORMANCE_MONITOR_PREFIX + "mo.discarded.count";
        public static final String PME_PA_EXECUTION_TIME_HOURLY = PERFORMANCE_MONITOR_PREFIX + "pa.execution.time.hourly";
        public static final Map<String, String> METRIC_DESCRIPTIONS = Map.ofEntries(
                Map.entry(APIGATEWAY_SESSIONID_HTTP_REQUESTS_DURATION_SECONDS, "The total duration of api gateway http requests."),
                Map.entry(KPI_HTTP_REQUESTS_DURATION_SECONDS, "The total duration of kpi http requests."),
                Map.entry(TOKENGATEWAY_HTTP_REQUESTS_DURATION_SECONDS, "The total duration of token gateway http requests."),
                Map.entry(DMM_HTTP_REQUESTS_DURATION_SECONDS, "The total duration of dmm http requests."),
                Map.entry(PME_HTTP_REQUESTS_DURATION_SECONDS, "The total duration of pme http requests."),
                Map.entry(PME_SESSION_CREATED_COUNT, "The total count of CREATED sessions."),
                Map.entry(PME_SESSION_STARTED_COUNT, "The total count of STARTED sessions."),
                Map.entry(PME_SESSION_FINISHED_COUNT, "The total count of FINISHED sessions."),
                Map.entry(PME_SESSION_STOPPED_COUNT, "The total count of STOPPED sessions."),
                Map.entry(PME_VERDICT_DEGRADED_COUNT, "The total count of DEGRADED verdicts."),
                Map.entry(PME_VERDICT_NOT_DEGRADED_COUNT, "The total count of NOT DEGRADED verdicts."),
                Map.entry(PME_VERDICT_NOT_POSSIBLE_COUNT, "The total count of NOT POSSIBLE verdicts."),
                Map.entry(PME_MO_KPI_RETRIEVAL_COUNT, "The total count of MOs for which KPIs were retrieved from PM Stats query service."),
                Map.entry(PME_MO_KPI_NULL_COUNT, "The total count of MOs for which KPIs were null."),
                Map.entry(PME_MO_COUNT, "The total count of MOs received for monitoring."),
                Map.entry(PME_MO_DISCARDED_COUNT, "The total count of discarded MOs received."),
                Map.entry(PME_MO_MONITORED_COUNT, "The total count of MOs monitored."),
                Map.entry(PME_MO_MONITORED_LOOKBACK_COUNT, "The total count of MOs monitored in a lookback scenario."),
                Map.entry(PME_PA_EXECUTION_TIME_HOURLY, "The time taken for an hourly PA execution."));
    }
}
