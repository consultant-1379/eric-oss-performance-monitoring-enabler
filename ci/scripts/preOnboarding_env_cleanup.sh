#!/bin/bash
#
# COPYRIGHT Ericsson 2023
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
DOCKER_CONTAINER_NAME="PME_rApp_preonboarding_cleanup_$(date +%d-%m-%y_%H-%M-%S)_${BUILD_NUMBER}"

COMMAND="docker run --init --rm \
       --name ${DOCKER_CONTAINER_NAME} \
       --volume ${KUBECONFIG_FILE_PATH}:/.kube/config \
       --env KUBECONFIG=/.kube/config \
       ${KUBE_DOCKER_IMAGE} kubectl get pvc -n ${NAMESPACE} -l app=eric-oss-performance-monitoring-enabler-pg
"

printf '# Command to collect PVCs:\n#\t%s\n\n' "$COMMAND"
PVCS=$($COMMAND 2>&1)

exit_status=$?
if [ $exit_status -ne 0 ]; then
  echo "# Failed to retrieve PVCs. ${PVCS}"
  exit 1;
fi

if [ "${PVCS}" == "" ] || [ "${PVCS}" == "No resources found in ${NAMESPACE} namespace." ]; then
  echo "# No PVCs to delete. ${PVCS}"
  exit 0;
fi

PVCS=$(echo "$PVCS" | tail -n +2 | awk '{ print $1 }')

printf '# PVCs found:\n#\t%s\n\n' "${PVCS}"

COMMAND="docker run --init --rm \
  --name ${DOCKER_CONTAINER_NAME} \
  --volume ${KUBECONFIG_FILE_PATH}:/.kube/config \
  --env KUBECONFIG=/.kube/config \
  ${KUBE_DOCKER_IMAGE} kubectl delete pvc -n ${NAMESPACE} ${PVCS}
"

printf '# Command to delete PVCs:\n#\t%s\n\n' "$COMMAND"
$COMMAND

exit_status=$?
echo "###########################################################"
if [ $exit_status -ne 0 ]; then
  echo "# Failed to delete PVCs."
  exit 1;
fi

echo "SUCCESS"
exit 0

