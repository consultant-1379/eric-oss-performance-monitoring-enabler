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

import { group, sleep } from 'k6';

import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.2/index.js';

import { htmlReport } from '/modules/plugins/eric-k6-static-report-plugin/eric-k6-static-report-plugin.js';

import * as gateway from './use_cases/pre_onboarding/gateway-tests.js';
import * as epme from './use_cases/post_instantiation/epme-tests.js';
import * as epmeAppStaging from './use_cases/app_staging/epme-app-staging-tests.js';
import * as rbac from './use_cases/post_instantiation/rbac-tests.js';
import { logData } from './modules/common.js';
import {
  createPmeTestwareUser,
  deletePmeTestwareUser,
} from './modules/pme-testware-user.js';
import { VALIDATE_EPME_RBAC } from './modules/constants.js';
// Summary
export function handleSummary(data) {
  logData('Preparing the end-of-test summary...');

  const result = { stdout: textSummary(data) };
  result['/reports/k6-test-results.html'] = htmlReport(data);
  result['/reports/summary.json'] = JSON.stringify(data);
  return result;
}

// Scenarios
export function appStagingScenario() {
  logData('APP_STAGING');
  __ENV.STAGING_LEVEL = 'APP';

  group('GIVEN EPME has started successfully', () => {
    epme.verifySessionEndpointReady();
    epmeAppStaging.verifyKpiDefinitionsPosted();
    epme.verifyEpmeSession();
    epmeAppStaging.verifyMonitoringObjectPersistence();
    epmeAppStaging.verifyPmeExecution();
    epmeAppStaging.verifyVerdicts();
    epme.verifySessionFinished();
    epmeAppStaging.verifyKpisCalculatedCorrectly();
  });
}

export function preOnboardingScenario() {
  logData('PRE_ONBOARDING');
  group('GIVEN The API Gateway is available', () => {
    createPmeTestwareUser();
    gateway.verifySessionCreation();
    deletePmeTestwareUser();
  });
}

export function postInstantiationScenario() {
  logData('POST_INSTANTIATION');
  createPmeTestwareUser();
  group(
    'GIVEN Data Collection Controller and Data Catalog is available',
    () => {
      epme.putIDSFile();
    },
  );

  group('GIVEN The User Administration API is available', () => {
    if (VALIDATE_EPME_RBAC) {
      rbac.verifyEpmeRbac();
    }
    rbac.verifySessionOperator();
    rbac.verifyConfigurationReader();
    rbac.verifyConfigurationOperator();
  });

  logData('Sleeping for 60s to allow RBAC to take effect');
  sleep(60);

  group('GIVEN EPME has instantiated successfully', () => {
    epme.verifyEpmeApiOnboarding();
    epme.verifySessionEndpointReady();
    epme.verifyTopicsReady();
  });

  if (VALIDATE_EPME_RBAC) {
    group('GIVEN EPME RBAC is in place', () => {
      rbac.verifyRbacEnforced();
    });
  }

  group('GIVEN PM Stats calculator service is available', () => {
    epme.verifyPersistedKpiDefinitions();
    epme.verifySimpleKpisAndRetrieveMoFdn();
  });

  group('GIVEN EPME is ready', () => {
    epme.verifyEpmeSession();
    epme.verifyGetEpmeSessionById();
    epme.verifySecondEpmeSession();
    epme.verifyThirdEpmeSession();
    epme.verifyCreateSessionConfiguration();
    epme.verifyUpdateSessionConfiguration();
    epme.verifyGetAllSessionConfigurations();
    epme.verifyGetOneSessionConfiguration();
    epme.verifyDeleteSessionConfiguration();
    epme.verifyProduceMonitoringObjects();
    epme.verifySessionFinished();
    epme.verifyVerdicts();
    epme.verifyEpmeSessionRetention();
    epme.verifyStopSession();
    epme.verifyKpisCalculated();
  });

  group('GIVEN The Log transformer is in place', () => {
    epme.verifyLogStreamingIsSuccessful();
    epme.verifyAuditLogIsSuccessful();
  });

  group('GIVEN Tests are finished', () => {
    rbac.verifyCleanUpEpmeRbac();
    // epme.verifyRoutesUpdated();
    // epme.verifyMetricsAreIncremented();
  });
  deletePmeTestwareUser();
}
