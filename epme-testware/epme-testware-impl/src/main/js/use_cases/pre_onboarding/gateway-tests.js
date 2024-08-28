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

import { group, check } from 'k6';

import { isSessionValid } from '../../utils/validationUtils.js';

import * as gateway from '../../modules/api-gateway.js';

function verifySessionCreation() {
  group('WHEN no existing JSESSIONID', () => {
    check(gateway.getSessionId(), {
      'THEN JSESSIONID is created and valid': r => isSessionValid(r),
    });
  });
}

module.exports = {
  verifySessionCreation,
};
