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

import {
  Reader,
  Writer,
  Connection,
  SchemaRegistry,
  SCHEMA_TYPE_AVRO,
  ISOLATION_LEVEL_READ_UNCOMMITTED,
  TLS_1_2,
} from 'k6/x/kafka';

import exec from 'k6/x/exec';

import { sleep } from 'k6';
import { logData } from './common.js';

import {
  KAFKA_TLS,
  KAFKA_URL,
  KAFKA_MONITORING_OBJECT_TOPIC,
  MONITORING_OBJECTS_SCHEMA,
  KAFKA_VERDICT_TOPIC,
  VERDICT_SCHEMA,
} from './epme-constants.js';

const BROKERS = [KAFKA_URL];
const SESSION_TIMEOUT = 60000000000;
const KAFKA_CRED_DIR = '/certs/kafka-credentials';
const TLS_CERT_DIR = '/certs/eric-sec-sip-tls-trusted-root-cert';
const TLS_CRT = 'tls.crt';
const TLS_KEY = 'tls.key';
const CA_CERT = 'ca.crt';

function createKafkaConfig() {
  logData(
    `Creating Kafka configuration with tls: '${
      KAFKA_TLS ? 'enabled' : 'disabled'
    }'`,
  );
  if (KAFKA_TLS) {
    const credentials = [...exec.command('ls', [KAFKA_CRED_DIR]).split('\n')];
    const trustedRootCert = [...exec.command('ls', [TLS_CERT_DIR]).split('\n')];

    logData(`${KAFKA_CRED_DIR} files:`, credentials);
    logData(`${TLS_CERT_DIR} files:`, trustedRootCert);

    if (!credentials.includes(TLS_CRT) || !credentials.includes(TLS_KEY)) {
      throw new Error(
        `${KAFKA_CRED_DIR} does not contain ${TLS_CRT} or ${TLS_KEY}`,
      );
    }

    if (!trustedRootCert.includes(CA_CERT)) {
      throw new Error(`${TLS_CERT_DIR} does not contain ${CA_CERT}`);
    }

    return {
      brokers: BROKERS,
      sessionTimeout: SESSION_TIMEOUT,
      tls: {
        enableTLS: true,
        clientCertPem: `${KAFKA_CRED_DIR}/${TLS_CRT}`,
        clientKeyPem: `${KAFKA_CRED_DIR}/${TLS_KEY}`,
        serverCaPem: `${TLS_CERT_DIR}/${CA_CERT}`,
        insecureSkipTlsVerify: false,
        minVersion: TLS_1_2,
      },
    };
  }
  return {
    brokers: BROKERS,
    sessionTimeout: SESSION_TIMEOUT,
  };
}

const KAFKA_CONFIG = createKafkaConfig();

function createConnection() {
  const cfg = {
    address: KAFKA_URL,
  };

  Object.assign(cfg, KAFKA_CONFIG);
  return new Connection(cfg);
}

function createReader() {
  const cfg = {
    groupID: 'epme-testware-group',
    groupTopics: [KAFKA_VERDICT_TOPIC],
    autoCreateTopic: false,
    maxWait: '10m',
    connectLogger: true,
    isolationLevel: ISOLATION_LEVEL_READ_UNCOMMITTED,
    maxAttempts: 100,
  };

  Object.assign(cfg, KAFKA_CONFIG);
  return new Reader(cfg);
}

function createWriter() {
  const cfg = {
    topic: KAFKA_MONITORING_OBJECT_TOPIC,
    autoCreateTopic: false,
  };

  Object.assign(cfg, KAFKA_CONFIG);
  return new Writer(cfg);
}

// Creates a new Writer object to produce messages to Kafka
const writer = createWriter();

const connection = createConnection();

const schemaRegistry = new SchemaRegistry();

let numberOfMessages = 0;

function getNumberOfMessages() {
  return numberOfMessages;
}

/**
 * This MUST be called when you are finished using Kafka.
 */
function close() {
  try {
    writer.close();
    connection.close();
  } catch (e) {
    logData('Failed to close kafka connection', e);
  }
}

function getTopics() {
  return connection.listTopics();
}

function sendMonitoringObjects(monitoringObjects) {
  if (!monitoringObjects.length) {
    return [];
  }

  for (const monitoringObject of monitoringObjects) {
    const _data = {
      pmeSessionId: monitoringObject.pmeSessionId,
      fdn: monitoringObject.fdn,
      time: monitoringObject.time,
      state: monitoringObject.state,
    };

    const messages = [
      {
        value: schemaRegistry.serialize({
          data: _data,
          schema: { schema: MONITORING_OBJECTS_SCHEMA },
          schemaType: SCHEMA_TYPE_AVRO,
        }),
      },
    ];

    try {
      writer.produce({ messages });
    } catch (e) {
      logData('Failed to send Kafka message(s)', e);
    }
  }
  numberOfMessages = monitoringObjects.length;
  sleep(5);
  return monitoringObjects;
}

function consumeVerdicts() {
  logData(`Listening for verdicts from topic '${KAFKA_VERDICT_TOPIC}'`);

  const reader = createReader();

  try {
    const verdicts = reader.consume({ limit: numberOfMessages });

    logData(`Consumed ${verdicts.length} verdicts`);

    return verdicts
      .map(({ value }) =>
        schemaRegistry.deserialize({
          data: value,
          schemaType: SCHEMA_TYPE_AVRO,
          schema: { schema: VERDICT_SCHEMA },
        }),
      )
      .map(({ pmeSessionId, fdn, kpiVerdicts }) => ({
        // omit timestamp
        pmeSessionId,
        fdn,
        kpiVerdicts,
      }));
  } catch (e) {
    logData('Failed to consume Kafka verdicts', e);
    return [];
  }
}

module.exports = {
  close,
  getTopics,
  sendMonitoringObjects,
  consumeVerdicts,
  getNumberOfMessages,
};
