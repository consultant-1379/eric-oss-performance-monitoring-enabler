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

SERVICE_NAME="eric-oss-performance-monitoring-enabler"

DEV_DOCKER_REPO=proj-eric-oss-dev
DEV_HELM_REPO=proj-eric-oss-dev-helm-local
MAVEN_BUILD=true

USER_ID=$( echo "$(whoami)" | tr '[:upper:]' '[:lower:]' | awk -F'@' '{print $1}'| awk -F'+' '$2 != "" {print $2;next};{print $1}')
VERSION="$( cat VERSION_PREFIX )-${USER_ID}"

CBOS_IMAGE_REPO=$( grep "cbos-image-repo" common-properties.yaml | awk '{print $3}' | sed -e 's/^"//' -e 's/"$//')
CBOS_IMAGE_NAME=$( grep "cbos-image-name" common-properties.yaml | awk '{print $3}' | sed -e 's/^"//' -e 's/"$//')
CBOS_IMAGE_TAG=$( grep "cbos-image-version" common-properties.yaml | awk '{print $3}' | sed -e 's/^"//' -e 's/"$//')

NC='\033[0m' # No Color
BROWN='\033[0;33m'

function log() {
  echo -e "\n${BROWN} --- ${1} --- ${NC}\n"
}

function checkExitCode() {
    if [ $? -ne 0 ]; then
          log "ERROR: $1 "
          exit 255
    fi
}

function buildDockerImage {
  ./ci/scripts/updateKpiDefinitions.sh ./src/main/resources/KpiDefinitions.json ./src/main/resources/KpiDefinitionsShortForm.json

  if [[ $MAVEN_BUILD == 'true' ]]; then
    log "Installing maven dependencies"
    mvn clean install -Dmaven.test.skip=true
    checkExitCode "Failed to Build Maven Project"
  fi

  cp target/*SNAPSHOT.jar target/$SERVICE_NAME-$VERSION.jar

  log "Building Docker Image $REPO/$SERVICE_NAME:$VERSION"

  DOCKER_IMAGE="armdocker.rnd.ericsson.se/$DEV_DOCKER_REPO/$SERVICE_NAME:$VERSION"

  docker build . --tag $DOCKER_IMAGE --build-arg JAR_FILE=$SERVICE_NAME-$VERSION.jar --build-arg CBOS_IMAGE_REPO=${CBOS_IMAGE_REPO} --build-arg CBOS_IMAGE_NAME=${CBOS_IMAGE_NAME} --build-arg CBOS_IMAGE_TAG=${CBOS_IMAGE_TAG}
  docker push $DOCKER_IMAGE

  checkExitCode "Failed to Build Docker Image"
}

function buildTestware {
  log "Building testware"

  ./epme-testware/gradlew \
      -Depme.docker.repo="${DEV_DOCKER_REPO}" \
      -Depme.helm.repo="${DEV_HELM_REPO}" \
      -Depme.release.version="${VERSION}" \
      -p epme-testware clean pushDockerImage helmPublish

  checkExitCode "Failed to Build Testware"
}

while getopts ":s" option; do
  case $option in
    s)
      MAVEN_BUILD=false
      ;;
    *)
      echo "Usage: $0 [-s]"
      echo "    -s : Skip maven build"
      exit 1
      ;;
  esac
done

buildDockerImage
buildTestware

echo
echo
log "Building testware complete"

echo "DOCKER_REPO   :: ${DEV_DOCKER_REPO}"
echo "HELM_REPO     :: ${DEV_HELM_REPO}"
echo "CHART_VERSION :: ${VERSION}"