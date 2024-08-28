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

export const PMSC_BASE_PATH = '/kpi-handling';
export const PMSC_DEFINITIONS_URI = `${PMSC_BASE_PATH}/model/v1/definitions`;
export const PMSC_COMPLEX_QUERY_URI = `${PMSC_BASE_PATH}/exposure/v1/kpi/kpi_pme_cell_complex_60`;
export const PMSC_QUERY_URI = `${PMSC_BASE_PATH}/exposure/v1/kpi`;
export const PMSC_CALCULATION_URI = `${PMSC_BASE_PATH}/calc/v1/calculations`;
export const RETRY_ATTEMPTS_FOR_SIMPLE_KPIS = 140;
