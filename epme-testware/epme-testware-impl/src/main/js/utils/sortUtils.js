/*
 * COPYRIGHT Ericsson 2023
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

const sortByKpiName = (a, b) => a.kpiName.localeCompare(b.kpiName);

const sortKpiVerdicts = (kpiVerdicts = []) => kpiVerdicts.sort(sortByKpiName);

const sortByFdn = (a, b) => a.fdn.localeCompare(b.fdn);

export const sortVerdicts = (verdicts = []) =>
  verdicts
    .map(verdict => ({
      pmeSessionId: verdict.pmeSessionId,
      fdn: verdict.fdn,
      timestamp: verdict.timestamp,
      kpiVerdicts: sortKpiVerdicts(verdict.kpiVerdicts),
    }))
    .sort(sortByFdn);
