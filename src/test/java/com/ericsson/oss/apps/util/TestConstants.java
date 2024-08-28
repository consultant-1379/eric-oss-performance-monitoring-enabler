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

import static com.ericsson.oss.apps.util.Constants.VERSION;

import lombok.experimental.UtilityClass;

@UtilityClass
public class TestConstants {
    public static final String STATUS_FIELD = "status";
    public static final String REASON_FIELD = "reason";
    public static final String DETAIL_FIELD = "detail";

    // Session payload constants
    public static final String CLIENT_APP_ID = "rappid--1--261854e7-2b26-4dc7-8d68-5bf01cfe62b4";
    public static final String ALT_CLIENT_APP_ID = "rappid--1--261854e7-2b26-4dc7-8d68-99999";
    public static final String SESSION_REFERENCE = "client-app-execution-0";
    public static final String PME_SESSION_ID = "PME-5797a5db-client-app-execution-0";
    public static final String PME_SESSION_ID_2 = "PME-5797a5db-client-app-execution-1";
    public static final String PME_SESSION_ID_3 = "PME-5797a5db-client-app-execution-2";
    public static final String FDN_FDD = "SubNetwork=Europe,SubNetwork=Ireland,SubNetwork=NETSimW,ManagedElement=LTE01dg2ERBS00001,ENodeBFunction=1,EUtranCellFDD=LTE01dg2ERBS00001-1";
    public static final String FDN_TDD = "SubNetwork=Europe,SubNetwork=Ireland,SubNetwork=NETSimW,ManagedElement=LTE01dg2ERBS00001,ENodeBFunction=1,EUtranCellTDD=LTE01dg2ERBS00001-2";
    public static final String FDN_NRCELLCU = "SubNetwork=Europe,SubNetwork=Ireland,SubNetwork=NETSimW,ManagedElement=NR01gNodeBRadio00002,ENodeBFunction=1,NRCellCU=NR01gNodeBRadio00002-1";
    public static final String FDN_NRCELLDU = "SubNetwork=Europe,SubNetwork=Ireland,SubNetwork=NETSimW,ManagedElement=NR01gNodeBRadio00002,ENodeBFunction=1,NRCellDU=NR01gNodeBRadio00002-1";
    public static final Long PME_CONFIGURATION_ID = 1L;
    public static final String SESSION_CONFIGURATION = "session_configuration";
    public static final String WEEKEND_DAYS = "Saturday,Sunday";
    public static final int DURATION = 18;
    public static final Long DEFAULT_DURATION_L = 18L;

    // Configuration payload constants
    public static final String TEST_CONFIGURATION_NAME_ONE = "test_configuration_1";
    public static final String TEST_CONFIGURATION_NAME_TWO = "test_configuration_2";
    public static final String APPLICATION_JSON = "application/json";

    public static final String AUTHORIZATION = "authorization";
    public static final String TOKEN = "Bearer eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJnS05wcWxKQVZoSFpFdC0" +
            "ybGNLVzQ0Y3B6YWNRUUVxQURJV0VMQU5LUjZFIn0.eyJleHAiOjE3MTY1NDMxMTIsImlhdCI6MTcxNjU0MjgxMiwianRpIjoiN2QwZDc1O" +
            "GEtNjMxNC00MjUyLTg4YjQtYmQ4YWUxNTQwMzljIiwiaXNzIjoiaHR0cHM6Ly9laWMuc3RzdnAxZWljMjguc3Rzb3NzLnNlcm8uZ2ljLmV" +
            "yaWNzc29uLnNlL2F1dGgvcmVhbG1zL21hc3RlciIsImF1ZCI6ImFjY291bnQiLCJzdWIiOiJjNjQ0YTA3YS0zMTU3LTQwZjYtYjBlYy00Z" +
            "DE4NzJkZDJiMmMiLCJ0eXAiOiJCZWFyZXIiLCJhenAiOiJlbyIsInNlc3Npb25fc3RhdGUiOiJiMjc5MmU2ZS1iYzI4LTRlY2MtYmZiZC0" +
            "wMjUyODkxODIzZjMiLCJhY3IiOiIxIiwiYWxsb3dlZC1vcmlnaW5zIjpbImh0dHBzOi8vKiJdLCJyZWFsbV9hY2Nlc3MiOnsicm9sZXMiO" +
            "lsiZGVmYXVsdC1yb2xlcy1tYXN0ZXIiLCJMb2dWaWV3ZXJfRXh0QXBwc19BcHBsaWNhdGlvbl9PcGVyYXRvciIsIkxvZ1ZpZXdlcl9TeXN" +
            "0ZW1fQXBwbGljYXRpb25fT3BlcmF0b3IiLCJvZmZsaW5lX2FjY2VzcyIsIlN5c3RlbV9SZWFkT25seSIsIk1ldHJpY3NWaWV3ZXIiLCJNb" +
            "25pdG9yaW5nX0FwcGxpY2F0aW9uX1JlYWRPbmx5IiwidW1hX2F1dGhvcml6YXRpb24iLCJMb2dWaWV3ZXIiLCJMb2dBUElfRXh0QXBwc19" +
            "BcHBsaWNhdGlvbl9SZWFkT25seSIsIlNlYXJjaEVuZ2luZVJlYWRlciIsIkJVUl9BcHBsaWNhdGlvbl9SZWFkZXIiXX0sInJlc291cmNlX" +
            "2FjY2VzcyI6eyJhY2NvdW50Ijp7InJvbGVzIjpbIm1hbmFnZS1hY2NvdW50IiwibWFuYWdlLWFjY291bnQtbGlua3MiLCJ2aWV3LXByb2Z" +
            "pbGUiXX19LCJzY29wZSI6Im9wZW5pZCBwcm9maWxlIGVtYWlsIHJvbGVzIiwic2lkIjoiYjI3OTJlNmUtYmMyOC00ZWNjLWJmYmQtMDI1M" +
            "jg5MTgyM2YzIiwiZW1haWxfdmVyaWZpZWQiOmZhbHNlLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJwbWUtY29uZmlndXJhdGlvbi1vcGVyYXR" +
            "vciJ9.8Sx5NsrgJlpPyCOfanlh_oAIhZ14TgiDlU1MJD_xAsmWsb388hH6RaCf12bH5SgVofYwyyt_W_12MHMjYBQIDNAQSfrwDIUef-uw" +
            "FdUZvdJMvGHh5Q2LTClPqPVBZDphwjTeM3ZxmB3P_mTgon0sg32E-E9Awvc1LBEiy0IEm_d_ZjPYZgd27zIEVEE-BcJuVki561MhxS9y9" +
            "EktzA9KwI8iiG3DTSz1-1_GOtgDkw2Cs1Wtj1bB9pUFcFJIwNocPCwPbDzubiYdvNFTw6dE9I_u7ziLMnw3-yh5ub8GNhxf-oQ0ZPh3u" +
            "SQPPrL64Hy2XhMwJC8V0oJWlS_a9monxw";

    public static final String CONFIGURATIONS_URL = VERSION + "/configurations";

}
