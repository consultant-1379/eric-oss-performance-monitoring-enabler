#!/bin/bash
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

echo "#####################################################################"
KUBE_DOCKER_IMAGE="armdocker.rnd.ericsson.se/proj-adp-cicd-drop/bob-py3kubehelmbuilder:latest"
DOCKER_CONTAINER_NAME="PME_rApp_postinstantation_prereq_$(date +%d-%m-%y_%H-%M-%S)_${BUILD_NUMBER}"
KAFKA_TOPIC_CONTAINER_NAME="PME_rApp_kafka_topic_creation_$(date +%d-%m-%y_%H-%M-%S)_${BUILD_NUMBER}"

COMMAND="docker run --init --rm \
       --name ${DOCKER_CONTAINER_NAME} \
       --volume ${KUBECONFIG_FILE_PATH}:/.kube/config \
       --env KUBECONFIG=/.kube/config \
       ${KUBE_DOCKER_IMAGE} kubectl describe deployment -n ${NAMESPACE} ${EPME_DEPLOYMENT}
"

printf '# Command to get Deployment:\n#\t%s\n\n' "$COMMAND"
DEPLOYMENT=$($COMMAND 2>&1)

exit_status=$?
if [ $exit_status -ne 0 ] || [ "${DEPLOYMENT}" == "No resources found in ${NAMESPACE} namespace." ]; then
  echo "# Failed to retrieve Deployment. ${DEPLOYMENT}"
  exit 1;
fi

RAPP_ID=$(echo "${DEPLOYMENT}" | grep "IAM_CLIENT_ID" | awk '{print $2}')
export RAPP_ID

echo "# Copying test preparation variables into ${WORKSPACE}/prep-variables.properties"
echo "RAPP_ID=${RAPP_ID}" >> "${WORKSPACE}/prep-variables.properties"

cat "${WORKSPACE}/prep-variables.properties"

echo "# Creating epme-verdicts and epme-monitoring-objects kafka topics"

KAFKA_TOPIC_COMMAND="docker run --init --rm \
       --name ${KAFKA_TOPIC_CONTAINER_NAME} \
       --volume ${KUBECONFIG_FILE_PATH}:/.kube/config \
       --volume ${WORKSPACE}/ci/csar_template/OtherDefinitions/ASD:/templates \
       --env KUBECONFIG=/.kube/config \
       ${KUBE_DOCKER_IMAGE} kubectl apply -n ${NAMESPACE} -f /templates/kafka-topics.yaml
"

$KAFKA_TOPIC_COMMAND

exit_status=$?
if [ $exit_status -ne 0 ] ; then
  echo "# Failed to create epme-verdicts and epme-monitoring-objects kafka topics"
  exit 1;
fi

exit 0

