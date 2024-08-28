#
# COPYRIGHT Ericsson 2023 - 2024
#
#
#
# The copyright to the computer program(s) herein is the property of
#
# Ericsson Inc. The programs may be used and/or copied only with written
#
# permission from Ericsson Inc. or in accordance with the terms and
#
# conditions stipulated in the agreement/contract under which the
#
# program(s) have been supplied.
#

#!/bin/bash
CUR_DIR=$(pwd)

OUTPUT_TRUSTSTORE_FILE="/tmp/truststore.jks"
TEMP_DIR="/tmp/temporaryStoreFiles"

KEYSTORE_OUTPUT_FILE_PATH="/tmp/eric-log-transformer.p12"

function importCacerts()
{
  if [ -d $TRUSTSTORE_CERTIFICATE_MOUNT_PATH ];
  then
    for CAFILE in $TRUSTSTORE_CERTIFICATE_MOUNT_PATH/*.crt
    do
        mkdir -p $TEMP_DIR && cd $_
        FILE_COUNT=$(csplit -f la-individual- $CAFILE '/-----BEGIN CERTIFICATE-----/' '{*}' --elide-empty-files | wc -l)

        for INDIVIDUALCERT in *
          do
            keytool --noprompt -trustcacerts -importcert \
                    -file $INDIVIDUALCERT -alias $CAFILE-$INDIVIDUALCERT -keystore $OUTPUT_TRUSTSTORE_FILE \
                    -deststorepass $TRUSTSTORE_PASS 2>&1
        done
        cd /
        rm -rf $TEMP_DIR
    done
  fi
}

function saveMTLSKeyStore()
{
  if [ -d $KEYSTORE_CERTIFICATE_MOUNT_PATH ];
  then
      mkdir -p $TEMP_DIR && cd $_
      for FILE in $KEYSTORE_CERTIFICATE_MOUNT_PATH/*;
      do
        if [[ $FILE == *.crt ]];
        then
          CERT_FILE=$FILE
        else
          KEY_FILE=$FILE
        fi
      done
        openssl pkcs12 -export -in $CERT_FILE -inkey $KEY_FILE -out $KEYSTORE_OUTPUT_FILE_PATH \
           -password pass:$ERIC_LOG_TRANSFORMER_KEYSTORE_PW -name rapp -noiter -nomaciter
    cd /
    rm -rf $TEMP_DIR
  fi
}
date;
if [ "$TLS_ENABLED" == "true" ]; then
  if [ -n "$TRUSTSTORE_CERTIFICATE_MOUNT_PATH" ]; then
    echo "Importing truststore from secrets"
    importCacerts
  fi
  if [ -n "$KEYSTORE_OUTPUT_FILE_PATH"  ]; then
    if [ "$LOG_STREAMING_METHOD" == "direct" ] || [ "$LOG_STREAMING_METHOD" == "dual" ]; then
      echo "Importing keystore from secrets"
      saveMTLSKeyStore
    fi
  fi
fi
date;

java $JAVA_OPTS $TLS_JAVA_OPTS \
    -Djavax.net.ssl.trustStore=$OUTPUT_TRUSTSTORE_FILE \
    -Djavax.net.ssl.trustStoreType=jks \
    -Djavax.net.ssl.trustStorePassword="$TRUSTSTORE_PASS" \
    -jar eric-oss-performance-monitoring-enabler-app.jar