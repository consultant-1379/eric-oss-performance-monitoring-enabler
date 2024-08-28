#!/bin/bash
#
# COPYRIGHT Ericsson 2024
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

#
# This is a temporary script, it will be removed in future but is required now to address the issue of the KafkaUser
# being overwritten.
#

DEFAULT_RAPP_ID=""
DEFAULT_NAMESPACE="eric-eic"

RAPP_ID=${DEFAULT_RAPP_ID}
NAMESPACE=${DEFAULT_NAMESPACE}

while getopts ":r:n:c:" option; do
  case $option in
    r) RAPP_ID="$OPTARG";;
    n) NAMESPACE="$OPTARG";;
    c) CONFIG_FILE="$OPTARG";;
    *)
      echo "Usage: $0 [-r rAppId] [-n namespace] [-c kube_config]"
      exit 1
      ;;
  esac
done

KAFKA_USER_FILE="${PWD}/ci/testware/testKafkaUser.yaml"

echo "##############################################"
echo "#    Starting KafkaUser Monitoring Process   #"
echo "##############################################"

COUNT=0
MAX=20

if [ -z "${CONFIG_FILE}" ];
then
  KUBE_DOCKER_IMAGE="armdocker.rnd.ericsson.se/proj-adp-cicd-drop/bob-py3kubehelmbuilder:latest"
  KUBECTL="docker run --init --rm --volume ${PWD}:${PWD} --volume ${PWD}/kubeconfig:/.kube/config --env KUBECONFIG=/.kube/config ${KUBE_DOCKER_IMAGE} kubectl"
else
  KUBECTL="kubectl --kubeconfig=${CONFIG_FILE}"
fi

# Apply the updated file, this should get overwritten once the tests kick off.
echo "##############################################"
echo "# Updating KafkaUser file with rAppId"
sed -i "s/name: rAppId/name: ${RAPP_ID}/g" "${KAFKA_USER_FILE}"
echo "##############################################"
cat "${KAFKA_USER_FILE}"
echo ""

echo "# Applying KafkaUser file: ${KAFKA_USER_FILE}"
${KUBECTL} --namespace "${NAMESPACE}" apply -f "${KAFKA_USER_FILE}"

echo "# Waiting for testware to start"
# Roughly the time taken for PME to start up.
sleep 80s

while [[ $COUNT -lt $MAX ]]
do
  KAFKA_USER=$(${KUBECTL} -n "${NAMESPACE}" get KafkaUser "${RAPP_ID}" | grep "${RAPP_ID}" | awk '{ print $1 }')
  echo "#"
  echo "# KAFKA_USER: ${KAFKA_USER}"
  echo "#"

  if [ -z "${KAFKA_USER}" ];
  then
    echo "# Could not find KafkaUser: ${RAPP_ID}"
  else
    KAFKA_USER_DESC=$(${KUBECTL} -n "${NAMESPACE}" get KafkaUser "${RAPP_ID}" -o json)
    if [[ $KAFKA_USER_DESC =~ "\"name\": \"epme-monitoring-objects\"" ]]
    then
      echo "# KafkaUser ${RAPP_ID} is already up to date"
      break
    else
      echo "##############################################"
      echo "# Applying KafkaUser file"
      ${KUBECTL} --namespace "${NAMESPACE}" apply -f "${KAFKA_USER_FILE}"
      break
    fi
  fi

  ((COUNT++))
  sleep 20s
done

echo "##############################################"
echo "#    Stopping KafkaUser Monitoring Process   #"
echo "##############################################"
