/*
 * COPYRIGHT Ericsson 2024
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

import * as rbac from '../../modules/rbac.js';
import * as epme from '../../modules/epme.js';
import {
  INGRESS_CONFIGURATION_OPERATOR,
  INGRESS_CONFIGURATION_READER,
  INGRESS_SESSION_OPERATOR,
  VALIDATE_EPME_RBAC,
} from '../../modules/constants.js';
import {
  isStatusOk,
  isStatusNoContent,
  isStatusNotFound,
  isSessionValid,
  isStatusForbidden,
} from '../../utils/validationUtils.js';
import {
  EPME_SESSION_PAYLOAD,
  getEpmeConfiguration,
} from '../../modules/epme-constants.js';

function verifyEpmeRbac() {
  group('WHEN Delete current EPME RBAC', () => {
    check(rbac.deleteEpmeRbac(), {
      'THEN status code is 404 or 204': r =>
        isStatusNotFound(r.status) || isStatusNoContent(r.status),
    });
  });

  group('WHEN create EPME RBAC', () => {
    check(rbac.createEpmeRbac(), {
      'THEN status code is 200': r => isStatusOk(r.status),
    });
  });

  rbac.clearSession();
}

function verifyUser(
  name,
  getUser,
  deleteUser,
  createUser,
  getSessionId,
  clearSession,
) {
  const response = getUser();
  const testUser = JSON.parse(response.body).find(
    user => user.username === name,
  );

  if (testUser) {
    group(`WHEN ${name} already exists, delete user`, () => {
      check(deleteUser(), {
        'THEN status code is 204': r => isStatusNoContent(r.status),
      });
    });
  }

  group(`WHEN create ${name}`, () => {
    check(createUser(), {
      'THEN status code is 200': r => isStatusOk(r.status),
    });
  });

  group(`WHEN login as ${name}`, () => {
    check(getSessionId(), {
      'THEN JSESSIONID is created and valid': r => isSessionValid(r),
    });
  });

  rbac.clearSession();
  clearSession();
}
function verifySessionOperator() {
  verifyUser(
    INGRESS_SESSION_OPERATOR,
    rbac.getSessionOperator,
    rbac.deleteSessionOperator,
    rbac.createSessionOperator,
    epme.getSessionOperatorSessionId,
    epme.cleanSession,
  );
}

function verifyConfigurationReader() {
  verifyUser(
    INGRESS_CONFIGURATION_READER,
    rbac.getConfigurationReader,
    rbac.deleteConfigurationReader,
    rbac.createConfigurationReader,
    epme.getConfigurationReaderSessionId,
    epme.cleanSession,
  );
}

function verifyConfigurationOperator() {
  verifyUser(
    INGRESS_CONFIGURATION_OPERATOR,
    rbac.getConfigurationOperator,
    rbac.deleteConfigurationOperator,
    rbac.createConfigurationOperator,
    epme.getConfigurationOperatorSessionId,
    epme.cleanSession,
  );
}

function verifyRbacEnforced() {
  epme.getSessionId();
  group('WHEN Logged in as PME Testware user and GET sessions', () => {
    check(epme.getAllEpmeSessions(), {
      'THEN status code is 403': r => isStatusForbidden(r.status),
    });
  });

  group('WHEN Logged in as PME Testware user and POST sessions', () => {
    check(epme.createEpmeSessionWithPayload(EPME_SESSION_PAYLOAD), {
      'THEN status code is 403': r => isStatusForbidden(r.status),
    });
  });

  group('WHEN Logged in as PME Testware user and GET configurations', () => {
    check(epme.getOneSessionConfiguration(), {
      'THEN status code is 403': r => isStatusForbidden(r.status),
    });
  });

  group('WHEN Logged in as PME Testware user and POST configurations', () => {
    check(epme.createEpmeConfiguration(getEpmeConfiguration()), {
      'THEN status code is 403': r => isStatusForbidden(r.status),
    });
  });
  epme.cleanSession();

  epme.getSessionOperatorSessionId();
  group('WHEN Logged in as Session Operator and GET sessions', () => {
    check(epme.getAllEpmeSessions(), {
      'THEN status code is 200': r => isStatusOk(r.status),
    });
  });

  group('WHEN Logged in as Session Operator and GET configurations', () => {
    check(epme.getOneSessionConfiguration(), {
      'THEN status code is 403': r => isStatusForbidden(r.status),
    });
  });

  group('WHEN Logged in as Session Operator and POST configurations', () => {
    check(epme.createEpmeConfiguration(getEpmeConfiguration()), {
      'THEN status code is 403': r => isStatusForbidden(r.status),
    });
  });
  epme.cleanSession();

  epme.getConfigurationReaderSessionId();
  group('WHEN Logged in as Configuration Reader and GET sessions', () => {
    check(epme.getAllEpmeSessions(), {
      'THEN status code is 403': r => isStatusForbidden(r.status),
    });
  });

  group('WHEN Logged in as Configuration Reader and POST sessions', () => {
    check(epme.createEpmeSessionWithPayload(EPME_SESSION_PAYLOAD), {
      'THEN status code is 403': r => isStatusForbidden(r.status),
    });
  });

  group('WHEN Logged in as Configuration Reader and GET configurations', () => {
    check(epme.getAllSessionConfigurations(), {
      'THEN status code is 200': r => isStatusOk(r.status),
    });
  });

  group(
    'WHEN Logged in as Configuration Reader and POST configurations',
    () => {
      check(epme.createEpmeConfiguration(getEpmeConfiguration()), {
        'THEN status code is 403': r => isStatusForbidden(r.status),
      });
    },
  );
  epme.cleanSession();

  epme.getConfigurationOperatorSessionId();
  group('WHEN Logged in as Configuration Operator and GET sessions', () => {
    check(epme.getAllEpmeSessions(), {
      'THEN status code is 403': r => isStatusForbidden(r.status),
    });
  });

  group('WHEN Logged in as Configuration Operator and POST sessions', () => {
    check(epme.createEpmeSessionWithPayload(EPME_SESSION_PAYLOAD), {
      'THEN status code is 403': r => isStatusForbidden(r.status),
    });
  });

  group(
    'WHEN Logged in as Configuration Operator and GET configurations',
    () => {
      check(epme.getAllSessionConfigurations(), {
        'THEN status code is 200': r => isStatusOk(r.status),
      });
    },
  );
  epme.cleanSession();
}

function verifyCleanUpEpmeRbac() {
  if (VALIDATE_EPME_RBAC) {
    group('WHEN Delete EPME RBAC', () => {
      check(rbac.deleteEpmeRbac(), {
        'THEN status code is 204': r => isStatusNoContent(r.status),
      });
    });
  }

  group('WHEN Delete Session Operator', () => {
    check(rbac.deleteSessionOperator(), {
      'THEN status code is 204': r => isStatusNoContent(r.status),
    });
  });

  group('WHEN Delete Configuration Reader', () => {
    check(rbac.deleteConfigurationReader(), {
      'THEN status code is 204': r => isStatusNoContent(r.status),
    });
  });

  group('WHEN Delete Configuration Operator', () => {
    check(rbac.deleteConfigurationOperator(), {
      'THEN status code is 204': r => isStatusNoContent(r.status),
    });
  });

  rbac.clearSession();
}

module.exports = {
  verifyEpmeRbac,
  verifySessionOperator,
  verifyConfigurationReader,
  verifyConfigurationOperator,
  verifyRbacEnforced,
  verifyCleanUpEpmeRbac,
};
