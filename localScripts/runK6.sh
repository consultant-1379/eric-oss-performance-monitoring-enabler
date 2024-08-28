#! /bin/bash
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

export TERM=cygwin

USER_ID=$( echo "$(whoami)" | tr '[:upper:]' '[:lower:]' | awk -F'@' '{print $1}'| awk -F'+' '$2 != "" {print $2;next};{print $1}')

DEFAULT_CONFIG_FILE="/c/Users/${USER_ID}/.kube/config"
DEFAULT_NAMESPACE="eric-eic"
DEFAULT_SCENARIO="PRE"
DEFAULT_HOST="gas.stsvp1eic28.stsoss.sero.gic.ericsson.se"
DEFAULT_INGRESS_LOGIN_USER="gas-user"
DEFAULT_INGRESS_LOGIN_PASSWORD="idunEr!css0n"
DEFAULT_EPME_DEPLOYMENT="eric-oss-performance-monitoring-enabler"
DEFAULT_EPME_PREFIX="/performance-monitoring-enabler"
DEFAULT_KAFKA_TLS_ENABLED="false"

CONFIG_FILE=${DEFAULT_CONFIG_FILE}
NAMESPACE=${DEFAULT_NAMESPACE}
SCENARIO=${DEFAULT_SCENARIO}
INGRESS_HOST=${DEFAULT_HOST}
INGRESS_LOGIN_USER=${DEFAULT_INGRESS_LOGIN_USER}
INGRESS_LOGIN_PASSWORD=${DEFAULT_INGRESS_LOGIN_PASSWORD}
EPME_DEPLOYMENT="${DEFAULT_EPME_DEPLOYMENT}"
EPME_PREFIX="${DEFAULT_EPME_PREFIX}"
VALIDATE_EPME_RBAC=false
KAFKA_TLS_ENABLED="${DEFAULT_KAFKA_TLS_ENABLED}"

while getopts ":c:n:s:i:u:p:d:e:r:k" option; do
  case $option in
    c) CONFIG_FILE="$OPTARG";;
    n) NAMESPACE="$OPTARG";;
    s) SCENARIO="$OPTARG";;
    i) INGRESS_HOST="$OPTARG";;
    u) INGRESS_LOGIN_USER="$OPTARG";;
    p) INGRESS_LOGIN_PASSWORD="$OPTARG";;
    d) EPME_DEPLOYMENT="$OPTARG";;
    e) EPME_PREFIX="$OPTARG";;
    r) VALIDATE_EPME_RBAC=true;;
    k) KAFKA_TLS_ENABLED="$OPTARG";;
    *)
      echo "Usage: $0 [-c kube_config] [-n namespace] [-s scenario (APP|PRE|POST)] [-i ingress_host] [-u ingress_username] [-p ingress_password] [-d epme_deployment] [-e epme_prefix] [-r validate_epme_rbac] [-k kafka_tls_enabled]"
      exit 1
      ;;
  esac
done

echo "Running with:
CONFIG_FILE (-c):" $CONFIG_FILE ",
NAMESPACE (-n):" $NAMESPACE ",
SCENARIO (-s)" $SCENARIO ",
INGRESS_HOST (-i)" $INGRESS_HOST ",
INGRESS_LOGIN_USER (-u)" $INGRESS_LOGIN_USER ",
INGRESS_LOGIN_PASSWORD (-p)" $INGRESS_LOGIN_PASSWORD ",
EPME_DEPLOYMENT (-d)" $EPME_DEPLOYMENT ",
EPME_PREFIX (-e)" $EPME_PREFIX ",
VALIDATE_EPME_RBAC (-r)" $VALIDATE_EPME_RBAC

CHART_VERSION="1.0.0"
INGRESS_SCHEMA="https"
OPTIONS_FILE="preOnboarding.options.json"
RAPP_ID=$(kubectl --kubeconfig=$CONFIG_FILE --namespace $NAMESPACE describe deploy ${EPME_DEPLOYMENT} | grep IAM_CLIENT_ID | awk '{ print $2 }')
BUILD_URL="http://localhost/job/1"
ENVIRONMENT="development"
EPME_INGRESS_SCHEMA="${INGRESS_SCHEMA}"
EPME_INGRESS_HOST="${INGRESS_HOST}"
BUILD_ID="$(date +"%s")"

if [[ "$SCENARIO" == "APP" ]]; then
  echo "# Setting App Staging environment"
  OPTIONS_FILE="appStaging.options.json"
  INGRESS_SCHEMA="http"
elif [[ "$SCENARIO" == "POST" ]]; then
  echo "# Setting post instantiation environment"
  OPTIONS_FILE="postInstantiation.options.json"
else
  echo "# Setting pre onboarding environment"
fi

HOME=$(PWD)
TESTWARE_DIR="$PWD/epme-testware"
TESTWARE_CHART_DIR="${TESTWARE_DIR}/epme-testware-chart"
USER_ID=$( echo "$(whoami)" | tr '[:upper:]' '[:lower:]' | awk -F'@' '{print $1}'| awk -F'+' '$2 != "" {print $2;next};{print $1}')
DOCKER_PATH="armdocker.rnd.ericsson.se/proj-eric-oss-dev"
DOCKER_IMAGE_NAME="eric-oss-performance-monitoring-enabler-testware-$USER_ID:$CHART_VERSION"
DOCKER_IMAGE="${DOCKER_PATH}/${DOCKER_IMAGE_NAME}"
RELEASE_NAME="epme-testware"
RELEASE_NAME_SIGNUM="${RELEASE_NAME}-${USER_ID}"
INGRESS_PME_TESTWARE_USER="pme-testware-user-${USER_ID}"
EPME_DB_HOST="${EPME_DEPLOYMENT}-pg"
EPME_DB_SECRET="${EPME_DEPLOYMENT}-postgres-secret"
PG_ACCESS_LABEL_KEY="${EPME_DEPLOYMENT}-pg-access"

HELM_EPME_ENV="env.INGRESS_SCHEMA=${INGRESS_SCHEMA},\
env.INGRESS_HOST=${INGRESS_HOST},\
env.INGRESS_LOGIN_USER=${INGRESS_LOGIN_USER},\
env.INGRESS_LOGIN_PASSWORD=${INGRESS_LOGIN_PASSWORD},\
env.RAPP_ID=${RAPP_ID},\
env.BUILD_ID=${BUILD_ID},\
env.EPME_INGRESS_SCHEMA=${INGRESS_SCHEMA},\
env.EPME_PREFIX=${EPME_PREFIX},\
env.EPME_INGRESS_HOST=${INGRESS_HOST},\
env.INGRESS_PME_TESTWARE_USER=${INGRESS_PME_TESTWARE_USER},\
env.EPME_DB_HOST=${EPME_DB_HOST},\
env.PG_ACCESS_LABEL_KEY=${PG_ACCESS_LABEL_KEY},\
env.VALIDATE_EPME_RBAC=${VALIDATE_EPME_RBAC},\
postgres.secretName=${EPME_DB_SECRET},\
kafka.tls.enabled=${KAFKA_TLS_ENABLED}"

HELM_K6_ENV="env.BUILD_URL=${BUILD_URL},\
env.OPTIONS_FILE=${OPTIONS_FILE},\
env.ENVIRONMENT=${ENVIRONMENT},\
env.APP_VERSION=${CHART_VERSION},\
env.PRODUCT_VERSION=${CHART_VERSION},\
env.TEST_VERSION=${CHART_VERSION}"

HELM_SET="${HELM_K6_ENV},${HELM_EPME_ENV}"

echo "# Creating Docker File Content"

${TESTWARE_DIR}/gradlew -p ${TESTWARE_DIR} clean copyDockerfile helmPackage

cd "${TESTWARE_DIR}/epme-testware-impl/build/docker/input"
echo "# Building Docker Image: ${DOCKER_IMAGE}"
docker build . -t "${DOCKER_IMAGE}"

echo "# Pushing Docker Image: ${DOCKER_IMAGE}"
docker push "${DOCKER_IMAGE}"

cd "${TESTWARE_DIR}/epme-testware-chart/build/helm/charts"
echo "# Updating Testware Chart"

OLD_TEXT="armdocker.rnd.ericsson.se\/proj-eric-oss-drop\/oss-testware\/eric-oss-performance-monitoring-enabler-testware:1.0.0"
NEW_TEXT="armdocker.rnd.ericsson.se\/proj-eric-oss-dev\/$DOCKER_IMAGE_NAME"
sed -i "s/$OLD_TEXT/$NEW_TEXT/g" "${PWD}/epme-testware-chart/values.yaml"

CHART_NAME="${RELEASE_NAME_SIGNUM}-chart-${CHART_VERSION}.tgz"
echo "# Removing old chart ${CHART_NAME}"
rm -rf ${CHART_NAME}
tar -czf ${CHART_NAME} "${RELEASE_NAME}-chart"

echo "# Removing any existing ${RELEASE_NAME}"
helm --kubeconfig=$CONFIG_FILE uninstall --namespace $NAMESPACE $RELEASE_NAME_SIGNUM

sleep 5s

echo "# Installing ${RELEASE_NAME}"
helm --kubeconfig=$CONFIG_FILE install --namespace $NAMESPACE $RELEASE_NAME_SIGNUM $CHART_NAME --set $HELM_SET --wait

echo "# Setting execution schedule to 10 minutes"
kubectl --kubeconfig=$CONFIG_FILE -n ${NAMESPACE} set env deployment/${EPME_DEPLOYMENT} EXECUTION_SCHEDULE="0 $(date -d "-50 minutes" +"%M") * * * *"

if [[ "$KAFKA_TLS_ENABLED" == "true" ]]; then
  cd ${HOME}
  echo "# KAFKA_TLS_ENABLED: ${KAFKA_TLS_ENABLED}, Running Create KafkaUser Script"
  ${HOME}/ci/scripts/createKafkaUser.sh -r "${RAPP_ID}" -n "${NAMESPACE}" -c "${CONFIG_FILE}"
  git checkout "${HOME}/ci/testware/testKafkaUser.yaml"
fi
