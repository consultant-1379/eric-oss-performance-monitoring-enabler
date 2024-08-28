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

import http from 'k6/http';

import {
  DEFAULT_TIMEOUT,
  HTTP_STATUS_OK,
  INGRESS_LOGIN_PARAMS,
  INGRESS_LOGIN_URI,
  INGRESS_URL,
  MAX_RETRY,
} from './constants.js';

const TIMEOUT = 'timeout';

function logData(message, data = '') {
  /* eslint no-console: ["error", { allow: ["log"] }] */
  console.log('<<');
  console.log(new Date().toISOString(), message, data);
  console.log('>>');
}

function httpPost(url, uri, formData, params = {}, options = {}) {
  const timeout = options[TIMEOUT] ? options[TIMEOUT] : DEFAULT_TIMEOUT;
  params[TIMEOUT] = `${timeout.toString()}s`;
  logData(`POST: ${url.concat(uri)}`, formData);

  let response = {};
  let retryCount = 0;
  while (retryCount < MAX_RETRY) {
    response = http.post(url.concat(uri), formData, params);
    if (response && response.body) {
      break;
    } else {
      retryCount += 1;
      logData(`RETRY: ${retryCount}`);
    }
  }

  logData('POST RESPONSE: ', response);
  return response;
}

function httpPostJson(url, uri, request, params = {}, options = {}) {
  return httpPost(url, uri, JSON.stringify(request), params, options);
}

function createSession(url, uri, body, headers, options) {
  const response = httpPostJson(url, uri, body, headers, options);
  const sessionId =
    response.status === HTTP_STATUS_OK && response.body ? response.body : '';
  return `JSESSIONID=${sessionId}`;
}

function createGasSession(options = {}) {
  const sessionId = createSession(
    INGRESS_URL,
    INGRESS_LOGIN_URI,
    '',
    INGRESS_LOGIN_PARAMS,
    options,
  );
  logData('CREATE GAS SESSION', sessionId);
  return sessionId;
}

function httpPut(url, uri, formData, params = {}, options = {}) {
  const timeout = options[TIMEOUT] ? options[TIMEOUT] : DEFAULT_TIMEOUT;
  params[TIMEOUT] = `${timeout.toString()}s`;
  logData(`PUT: ${url.concat(uri)}`, formData);

  let response = {};
  let retryCount = 0;
  while (retryCount < MAX_RETRY) {
    response = http.put(url.concat(uri), formData, params);
    if (response && response.body) {
      break;
    } else {
      retryCount += 1;
      logData(`RETRY: ${retryCount}`);
    }
  }

  logData('PUT RESPONSE: ', response);
  return response;
}

function httpPutJson(url, uri, request, params = {}, options = {}) {
  return httpPut(url, uri, JSON.stringify(request), params, options);
}

function httpGet(url, uri, params = {}, options = {}) {
  const timeout = options[TIMEOUT] ? options[TIMEOUT] : DEFAULT_TIMEOUT;
  params[TIMEOUT] = `${timeout.toString()}s`;
  logData(`GET: ${url.concat(uri)}`, params);

  let response = {};
  let retryCount = 0;
  while (retryCount < MAX_RETRY) {
    response = http.get(url.concat(uri), params);
    if (response && response.status) {
      break;
    } else {
      retryCount += 1;
      logData(`RETRY: ${retryCount}`);
    }
  }

  logData('GET RESPONSE: ', response);
  return response;
}

function httpDelete(url, uri, formData = undefined, params = {}, options = {}) {
  const timeout = options[TIMEOUT] ? options[TIMEOUT] : DEFAULT_TIMEOUT;
  params[TIMEOUT] = `${timeout.toString()}s`;
  logData(`DELETE: ${url.concat(uri)}`, params);

  let response = {};
  let retryCount = 0;
  while (retryCount < MAX_RETRY) {
    response = http.del(url.concat(uri), formData, params);
    if (response && response.status) {
      break;
    } else {
      retryCount += 1;
      logData(`RETRY: ${retryCount}`);
    }
  }

  logData('DELETE RESPONSE: ', response);
  return response;
}

function httpDeleteJson(
  url,
  uri,
  formData = undefined,
  params = {},
  options = {},
) {
  return httpDelete(url, uri, JSON.stringify(formData), params, options);
}

function httpRequest(requestType, url, uri, formData, params = {}) {
  logData(requestType + ': '.concat(url.concat(uri)), formData);

  let response = {};
  let retryCount = 0;
  while (retryCount < MAX_RETRY) {
    response = http.request(requestType, url.concat(uri), formData, params);
    if (response && response.body) {
      break;
    } else {
      retryCount += 1;
      logData('RETRY: '.concat(retryCount));
    }
  }
  logData(`${requestType} RESPONSE: `, response);
  return response;
}

module.exports = {
  createSession,
  createGasSession,
  httpDelete,
  httpGet,
  httpPut,
  httpPostJson,
  httpPutJson,
  logData,
  httpDeleteJson,
  httpRequest,
};
