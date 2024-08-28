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

import sql from 'k6/x/sql';

import { DATE_FORMAT_24H, POSTGRES_URL } from './epme-constants.js';
import { logData } from './common.js';

const db = sql.open('postgres', POSTGRES_URL);

function getMonitoringObjects(sessionIds) {
  const sessionIdList = sessionIds.map(id => `'${id}'`).join(',');

  return sql.query(
    db,
    `SELECT fdn, pme_session_id, state,
        TO_CHAR(start_time, '${DATE_FORMAT_24H}') AS start_time_formatted,
        TO_CHAR(last_processed_time, '${DATE_FORMAT_24H}') AS last_processed_time_formatted,
        TO_CHAR(end_time, '${DATE_FORMAT_24H}') AS end_time_formatted
      FROM monitoring_object 
      WHERE pme_session_id in (${sessionIdList})
      ORDER BY fdn;`,
  );
}

function close() {
  try {
    db.close();
  } catch (e) {
    logData('Failed to close db', e);
  }
}

module.exports = {
  close,
  getMonitoringObjects,
};
