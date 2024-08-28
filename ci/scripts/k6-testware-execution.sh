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

# Docker images used
KUBE_DOCKER_IMAGE="armdocker.rnd.ericsson.se/proj-adp-cicd-drop/bob-py3kubehelmbuilder:latest"
K6_CLI_IMAGE="armdocker.rnd.ericsson.se/proj-eric-oss-dev-test/k6-reporting-tool-cli:latest"

# The id user & group
ID_U=`id -u`
ID_G=`id -g`

# The kubectl & helm docker images
KUBECTL="docker run --init --rm --volume ${PWD}:${PWD} --volume ${PWD}/kubeconfig:/.kube/config --env KUBECONFIG=/.kube/config ${KUBE_DOCKER_IMAGE} kubectl"
HELM="docker run --init --rm --user ${ID_U}:${ID_G} --volume ${HOME}:${HOME} --volume ${PWD}:${PWD} --volume ${PWD}/kubeconfig:/.kube/config --env KUBECONFIG=/.kube/config ${KUBE_DOCKER_IMAGE} helm"

# EPME chart variables
EPME_TESTWARE_NAME="epme-testware-chart"

if [[ -z "$EPME_DEPLOYMENT" ]]; then
  EPME_DEPLOYMENT="eric-oss-performance-monitoring-enabler"
fi

if [[ -z "$EPME_PREFIX" ]]; then
  EPME_PREFIX="/performance-monitoring-enabler"
fi

EPME_DB_HOST="${EPME_DEPLOYMENT}-pg"
EPME_DB_SECRET="${EPME_DEPLOYMENT}-postgres-secret"
PG_ACCESS_LABEL_KEY="${EPME_DEPLOYMENT}-pg-access"
TESTWARE_REPO="epme-testware-repo"
TESTWARE_PATH="https://arm.seli.gic.ericsson.se/artifactory/${HELM_REPO}"
HELM_EPME_ENV="env.INGRESS_SCHEMA=${INGRESS_SCHEMA},env.INGRESS_HOST=${INGRESS_HOST},env.INGRESS_LOGIN_USER=${INGRESS_LOGIN_USER},env.INGRESS_LOGIN_PASSWORD=${INGRESS_LOGIN_PASSWORD},env.RAPP_ID=${RAPP_ID},env.BUILD_ID=${BUILD_ID},env.EPME_INGRESS_SCHEMA=${INGRESS_SCHEMA},env.EPME_INGRESS_HOST=${INGRESS_HOST},env.INGRESS_PME_TESTWARE_USER=${INGRESS_PME_TESTWARE_USER},env.EPME_PREFIX=${EPME_PREFIX},env.EPME_DB_HOST=${EPME_DB_HOST},postgres.secretName=${EPME_DB_SECRET},env.PG_ACCESS_LABEL_KEY=${PG_ACCESS_LABEL_KEY},kafka.tls.enabled=${KAFKA_TLS_ENABLED}"
HELM_K6_ENV="env.BUILD_URL=${BUILD_URL},env.OPTIONS_FILE=${OPTIONS_FILE},env.ENVIRONMENT=production,env.APP_VERSION=${CHART_VERSION},env.PRODUCT_VERSION=${CHART_VERSION},env.TEST_VERSION=${CHART_VERSION},env.INGRESS_SESSION_OPERATOR=${INGRESS_SESSION_OPERATOR},env.INGRESS_CONFIGURATION_OPERATOR=${INGRESS_CONFIGURATION_OPERATOR},env.INGRESS_CONFIGURATION_READER=${INGRESS_CONFIGURATION_READER},env.VALIDATE_EPME_RBAC=true"
HELM_SET="${HELM_K6_ENV},${HELM_EPME_ENV}"
RELEASE_NAME="${EPME_TESTWARE_NAME}-${BUILD_ID}"

function installK6DevSecrets() {
  echo "# Installing K6 development secrets"
  ${KUBECTL} apply -f ${PWD}/ci/testware/secrets/testware-resources-secret.yaml --namespace ${NAMESPACE}
}

echo "#####################################################################"
echo "# Creating kubeconfig"

echo "# Copying ${KUBECONFIG_FILE_PATH}"
cp ${KUBECONFIG_FILE_PATH} ${PWD}/kubeconfig
chmod -f 777 ${PWD}/kubeconfig | true

chmod -f 777 ${PWD}/.helm/repositories.yaml | true
chmod -f 777 ${PWD}/.helm/repositories.lock | true

echo "#####################################################################"
echo "# Getting testware secrets"

echo "# INSTALL_K6_DEV_SECRETS: ${INSTALL_K6_DEV_SECRETS}"

if [[ "$INSTALL_K6_DEV_SECRETS" == "true" ]]; then
  installK6DevSecrets
else
  if [[ "$STAGING_LEVEL" == "APP" ]]; then
    echo "# Installing K6 app staging secrets"
    ${KUBECTL} apply -f ${PWD}/ci/testware/secrets/app-staging-resources-secret.yaml --namespace ${NAMESPACE}
  else
    echo "# Installing K6 product secrets"
    ${KUBECTL} apply -f ${PWD}/ci/testware/secrets/product-staging-resources-secret.yaml --namespace ${NAMESPACE}
  fi
fi

# Due to issue with Jenkins including colour characters into variables temp file is required
K6_TMP_DIR="${PWD}/.k6-tmp/"
K6_TMP_FILE="${K6_TMP_DIR}/file.txt"
rm -rf ${K6_TMP_DIR} || true
mkdir -p ${K6_TMP_DIR}

RPT_API_URL_BASE_64=$($KUBECTL get secrets/testware-resources-secret --template={{.data.api_url}} --namespace $NAMESPACE)
echo ${RPT_API_URL_BASE_64} > ${K6_TMP_FILE}
RPT_API_URL=$(cat -A $K6_TMP_FILE | base64 -di)
echo "# RPT_API_URL: ${RPT_API_URL}"

RPT_GUI_URL_BASE_64=$($KUBECTL get secrets/testware-resources-secret --template={{.data.gui_url}} --namespace $NAMESPACE)
echo ${RPT_GUI_URL_BASE_64} > ${K6_TMP_FILE}
RPT_GUI_URL=$(cat -A $K6_TMP_FILE | base64 -di)
echo "# RPT_GUI_URL: ${RPT_GUI_URL}"

TESTWARE_CLI="docker run --rm -t -e RPT_API_URL=${RPT_API_URL} -e RPT_GUI_URL=${RPT_GUI_URL} -v ${PWD}:${PWD} --user ${ID_U}:${ID_G} ${K6_CLI_IMAGE} testware-cli"

echo "#####################################################################"
echo "# Creating a K6 Job for ${BUILD_URL}"

${TESTWARE_CLI} create-job --jenkins-url ${BUILD_URL} --testware-count 1 --timeout 3900

echo "#####################################################################"
echo "# Preparing for Helm install"
echo "# K6 ENV: ${HELM_SET}"

echo "# helm repo remove"
${HELM} repo remove ${TESTWARE_REPO} --repository-cache ${PWD}/.helm/cache --repository-config ${PWD}/.helm/repositories.yaml
echo "# helm repo add"
${HELM} repo add ${TESTWARE_REPO} ${TESTWARE_PATH} --username ${FUNCTIONAL_USER_USERNAME} --password ${FUNCTIONAL_USER_PASSWORD} --repository-cache ${PWD}/.helm/cache --repository-config ${PWD}/.helm/repositories.yaml
echo "# helm repo install"
${HELM} install ${RELEASE_NAME} ${TESTWARE_REPO}/${EPME_TESTWARE_NAME} --version ${CHART_VERSION} --namespace ${NAMESPACE} --set ${HELM_SET} --repository-cache ${PWD}/.helm/cache --repository-config ${PWD}/.helm/repositories.yaml

EXIT_STATUS=$?
if [ $EXIT_STATUS -ne 0 ]; then
    echo "# Helm install failed!"
    echo "# Command: ${HELM} install ${RELEASE_NAME} ${TESTWARE_REPO}/${EPME_TESTWARE_NAME} --version ${CHART_VERSION} --namespace ${NAMESPACE} --set ${HELM_SET} --repository-cache ${PWD}/.helm/cache --repository-config ${PWD}/.helm/repositories.yaml"
    exit 1
fi

if [[ "$KAFKA_TLS_ENABLED" == "true" ]]; then
  echo "# KAFKA_TLS_ENABLED: ${KAFKA_TLS_ENABLED}, Running Create KafkaUser Script"
  ${PWD}/ci/scripts/createKafkaUser.sh -r "${RAPP_ID}" -n "${NAMESPACE}" &
fi

echo "#####################################################################"
echo "# Monitoring K6 tests"

${TESTWARE_CLI} wait-testware --url ${BUILD_URL} --path ${PWD} --delay 20 --retries 375 || true
echo "# ${TESTWARE_CLI}"

# Finished at this point, print complete status
FINISHED_STATUS=$($TESTWARE_CLI get-status --url ${BUILD_URL} --path ${PWD})
echo "# ${FINISHED_STATUS}"

TESTWARE_ID_CMD=$($TESTWARE_CLI get-status --url ${BUILD_URL} --path ${PWD} | grep id\: | awk -F: '{print $2}' | tr -d '[:space:]')
echo ${TESTWARE_ID_CMD} > ${K6_TMP_FILE}
TESTWARE_ID=$(cat -A $K6_TMP_FILE | sed -r 's/\^\[\[[[:digit:]]+[[:alpha:]]//g' | sed -r 's/\$//g')
echo "# TESTWARE_ID: ${TESTWARE_ID}"

TESTWARE_ID_CMD=$($TESTWARE_CLI get-status --url ${BUILD_URL} --path ${PWD} | grep passed\: | awk -F: '{print $2}' | tr -d '[:space:]')
echo ${TESTWARE_ID_CMD} > ${K6_TMP_FILE}
TEST_SUCCESS=$(cat -A $K6_TMP_FILE | sed -r 's/\^\[\[[[:digit:]]+[[:alpha:]]//g' | sed -r 's/\$//g')
echo "# TEST_SUCCESS: ${TEST_SUCCESS}"

if [[ -z "$TEST_SUCCESS" ]]; then
  TEST_SUCCESS="False"
fi

echo "#####################################################################"
echo "# Fetching K6 logs"

${TESTWARE_CLI} list-logs --id ${TESTWARE_ID}
${TESTWARE_CLI} download-log --id ${TESTWARE_ID} --type k6 --path ${PWD}/doc/Test_Report

echo "#####################################################################"
echo "# Preparing for Helm uninstall"

echo "# helm repo uninstall"
${HELM} uninstall ${RELEASE_NAME} --namespace ${NAMESPACE}
echo "# helm repo remove"
${HELM} repo remove ${TESTWARE_REPO} --repository-cache ${PWD}/.helm/cache --repository-config ${PWD}/.helm/repositories.yaml

echo "#####################################################################"

# Remove the temp k6 folder
rm -rf ${K6_TMP_DIR} || true

if [[ "$TEST_SUCCESS" == "False" ]]; then
  echo "# K6 Test failed!"
  exit 1;
else
  echo "# K6 Tests passed!"
  exit 0;
fi
