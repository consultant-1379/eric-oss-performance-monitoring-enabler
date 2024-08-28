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

declare -r SERVICE_NAME="eric-oss-performance-monitoring-enabler"
declare -r REPO="proj-eric-oss-dev"

CBOS_IMAGE_REPO=$( grep "cbos-image-repo" common-properties.yaml | awk '{print $3}' | sed -e 's/^"//' -e 's/"$//')
CBOS_IMAGE_NAME=$( grep "cbos-image-name" common-properties.yaml | awk '{print $3}' | sed -e 's/^"//' -e 's/"$//')
CBOS_IMAGE_TAG=$( grep "cbos-image-version" common-properties.yaml | awk '{print $3}' | sed -e 's/^"//' -e 's/"$//')

USER_ID=$( echo "$(whoami)" | tr '[:upper:]' '[:lower:]' | awk -F'@' '{print $1}'| awk -F'+' '$2 != "" {print $2;next};{print $1}')

SERVICE_NAME_SIGNUM="${SERVICE_NAME}-${USER_ID}"

VERSION=$(cat ./VERSION_PREFIX)
IMAGE_VERSION="$VERSION-$USER_ID"

GATEWAY_URL="performance-monitoring-enabler-$USER_ID"
CSAR_VERSION="$VERSION-0"

WORKDIR=$(pwd)
TEMP_WORKDIR="$WORKDIR/tmp"

MAVEN_BUILD=false
UPDATE_HELM_DEPENDENCIES=false
CLEAN=false
VERBOSE=false
SLIMMED_CSAR=false

NC='\033[0m' # No Color
BROWN='\033[0;33m'

function log() {
  echo -e "\n${BROWN} --- ${1} --- ${NC}\n"
}

function verboseLog() {
  if [[ $VERBOSE = true ]]; then
    echo -e "\n${1}\n"
  fi
}

function checkExitCode() {
    if [ $? -ne 0 ]; then
          log "ERROR: $1 "
          exit 255
    fi
}

function buildImage {
  if [[ $MAVEN_BUILD = true ]]; then
    export SELI_ARTIFACTORY_REPO_USER=${USER_ID} && export SELI_ARTIFACTORY_REPO_PASS=${ARTIFACTORY_PASS}
    log "Installing maven dependencies"
    if [[ $VERBOSE = true ]]; then
        mvn clean install -Dmaven.test.skip=true
      else
        echo "Running Maven Build.."
        mvn clean install -Dmaven.test.skip=true > /dev/null 2>&1
    fi
    checkExitCode "Failed to Build Maven Project"
  fi
  cp target/*SNAPSHOT.jar target/$SERVICE_NAME-$IMAGE_VERSION.jar

  log "Building Docker Image $REPO/$SERVICE_NAME:$IMAGE_VERSION"
  if [[ $VERBOSE = true ]]; then
      docker build . --tag armdocker.rnd.ericsson.se/$REPO/$SERVICE_NAME:$IMAGE_VERSION --build-arg JAR_FILE=$SERVICE_NAME-$IMAGE_VERSION.jar --build-arg CBOS_IMAGE_REPO=${CBOS_IMAGE_REPO} --build-arg CBOS_IMAGE_NAME=${CBOS_IMAGE_NAME} --build-arg CBOS_IMAGE_TAG=${CBOS_IMAGE_TAG}
    else
      echo "Running Docker Build.."
      docker build . --tag armdocker.rnd.ericsson.se/$REPO/$SERVICE_NAME:$IMAGE_VERSION --build-arg JAR_FILE=$SERVICE_NAME-$IMAGE_VERSION.jar --build-arg CBOS_IMAGE_REPO=${CBOS_IMAGE_REPO} --build-arg CBOS_IMAGE_NAME=${CBOS_IMAGE_NAME} --build-arg CBOS_IMAGE_TAG=${CBOS_IMAGE_TAG} > /dev/null 2>&1
  fi

  checkExitCode "Failed to Build Docker Image"
}

function updatePmeCharts() {
    log "Pulling Chart dependencies ${SERVICE_NAME}"
    helm dependency update "$TEMP_WORKDIR/charts/${SERVICE_NAME}" 2>&1 | grep -v "skipping loading invalid"
}

function populateTemplatesInChartAndCsar() {
  log "Populating Chart and CSAR metadata"

  verboseLog "Updating Chart name"

  MSYS_NO_PATHCONV=1 docker run --rm -v "${TEMP_WORKDIR}":/workdir mikefarah/yq -i \
    "
      .name = \"${SERVICE_NAME_SIGNUM}\"
    " charts/$SERVICE_NAME/Chart.yaml

  verboseLog "Updating eric-product-info"

  MSYS_NO_PATHCONV=1 docker run --rm -v "${TEMP_WORKDIR}":/workdir mikefarah/yq -i \
    "
      .images.eric-oss-performance-monitoring-enabler.repoPath = \"${REPO}\" |
      .images.eric-oss-performance-monitoring-enabler.tag = \"${IMAGE_VERSION}\"
    " charts/$SERVICE_NAME/eric-product-info.yaml

  verboseLog "Updating AppDescriptor"

  MSYS_NO_PATHCONV=1 docker run --rm -v "${TEMP_WORKDIR}":/workdir mikefarah/yq -i \
    "
      .[\"Description of an APP\"].APPName = \"${SERVICE_NAME_SIGNUM}\" |
      .[\"Description of an APP\"].APPVersion = \"${CSAR_VERSION}\" |
      .APPComponent.NameofComponent = \"${SERVICE_NAME_SIGNUM}\" |
      .APPComponent.Version = \"${CSAR_VERSION}\"
    " csar_template/Definitions/AppDescriptor.yaml

  verboseLog "Updating eric-oss-performance-monitoring-enablerASD"

  MSYS_NO_PATHCONV=1 docker run --rm -v "${TEMP_WORKDIR}":/workdir mikefarah/yq -i \
    "
      .deploymentItems.artifactId = \"OtherDefinitions/ASD/${SERVICE_NAME_SIGNUM}-${CSAR_VERSION}.tgz\"
    " csar_template/OtherDefinitions/ASD/eric-oss-performance-monitoring-enablerASD.yaml

  verboseLog "Updating values.yaml"

  MSYS_NO_PATHCONV=1 docker run --rm -v "${TEMP_WORKDIR}":/workdir mikefarah/yq -i \
    "
      .nameOverride = \"pme-${USER_ID}\" |
      .fullnameOverride = \"pme-${USER_ID}\" |
      .apiGateway.route.pathPrefix = \"/${GATEWAY_URL}\" |
      .kafka.consumerGroup = \"/pme-client-${USER_ID}-consumer-group\" |
      .eric-oss-performance-monitoring-enabler-pg.nameOverride = \"pme-${USER_ID}-pg\" |
      .eric-oss-performance-monitoring-enabler-pg.host = \"pme-${USER_ID}-pg\" |
      .eric-oss-performance-monitoring-enabler-pg.credentials.kubernetesSecretName = \"pme-${USER_ID}-postgres-secret\"
    " charts/$SERVICE_NAME/values.yaml
}

function packageAppIntoCsar {
  mkdir -p "$TEMP_WORKDIR/output"

  cp -r "$WORKDIR/ci/csar_additional_values/" "$TEMP_WORKDIR/"
  cp -r "$WORKDIR/ci/csar_template/" "$TEMP_WORKDIR/"
  cp -r "$WORKDIR/charts/" "$TEMP_WORKDIR/"

  if [[ $VERBOSE = true ]]; then
      dos2unix "$WORKDIR/entrypoint.sh"
      find "$TEMP_WORKDIR/" -type f -print0 | xargs -0 dos2unix --
  else
      dos2unix "$WORKDIR/entrypoint.sh"
      find "$TEMP_WORKDIR/" -type f -print0 | xargs -0 dos2unix -- > /dev/null 2>&1
  fi

  buildImage

  populateTemplatesInChartAndCsar

  if [[ $UPDATE_HELM_DEPENDENCIES = true ]]; then
    updatePmeCharts
  fi

  log "Packaging helm chart"
  helm package --version $CSAR_VERSION "$TEMP_WORKDIR/charts/$SERVICE_NAME" -d "$TEMP_WORKDIR/csar_template/OtherDefinitions/ASD"

  checkExitCode "Failed to Package helm chart"

  bundleImages

  packageCsar
}

function packageCsar {
  log "Packaging csar file to $TEMP_WORKDIR/output"

  MSYS_NO_PATHCONV=1 docker run --init --rm \
    --volume "$TEMP_WORKDIR/output":/tmp/csar/ \
    --volume "$HOME"/.docker:/root/.docker \
    --volume /var/run/docker.sock:/var/run/docker.sock \
    --workdir /target \
    --volume "$TEMP_WORKDIR/csar_template":/target \
    armdocker.rnd.ericsson.se/proj-eric-oss-dev-test/releases/eric-oss-app-package-tool:latest \
    generate --tosca /target/Metadata/Tosca.meta \
    --name "$SERVICE_NAME_SIGNUM-$CSAR_VERSION" \
    --images /tmp/csar/docker.tar \
    --helm3 \
    --output /tmp/csar

    checkExitCode "Failed to Package csar"

    echo "$SERVICE_NAME_SIGNUM-$CSAR_VERSION.csar saved"
}

function bundleImages() {
  local images
  images=$(helm template "$TEMP_WORKDIR/charts/$SERVICE_NAME/" -f "$TEMP_WORKDIR/csar_additional_values/site-values.yaml" | grep image: | awk '{ print $2 }' | uniq)
  log "Bundling images"
  echo "Images in chart:"
  echo "$images"
  echo

  if [[ "$SLIMMED_CSAR" == true ]]; then
    images=$(echo "$images" | grep "$SERVICE_NAME")
    echo "Only including EPME docker images in CSAR:"
    echo "$images"
    echo
  fi

  for image in $images
  do
    if [[ "$(docker images -q "$image" 2> /dev/null)" == "" ]]; then
      echo "Image: $image is not present. Pulling $image...";
      docker pull $image;
      echo
    fi
    echo "Retagging: $image to $(echo "${image//armdocker.rnd.ericsson.se\//}")"
    docker tag $image "$(echo "${image//armdocker.rnd.ericsson.se\//}")"
  done
  echo
  echo "Saving images to tar"
  docker save $(echo "${images//armdocker.rnd.ericsson.se\//}" | tr '\n' ' ') -o "$TEMP_WORKDIR/output/docker.tar"

  checkExitCode "Failed to bundle images"
}

function clearLocalImages(){
  docker rmi -f armdocker.rnd.ericsson.se/$REPO/$SERVICE_NAME:$IMAGE_VERSION
  docker rmi -f $REPO/$SERVICE_NAME:$IMAGE_VERSION
}

function readArgs(){
  while getopts mcvus flag
  do
    case "${flag}" in
      c) CLEAN=true;;
      v) VERBOSE=true;;
      m) MAVEN_BUILD=true;;
      u) UPDATE_HELM_DEPENDENCIES=true;;
      s) SLIMMED_CSAR=true;;
    esac
  done
}

#########
# main
#########

readArgs $@

echo "Running with: CLEAN_TEMP_WORKDIR (-c):" $CLEAN ", MAVEN_BUILD (-m):" $MAVEN_BUILD ", UPDATE_HELM_DEPENDENCIES (-u)" $UPDATE_HELM_DEPENDENCIES ", VERBOSE (-v)" $VERBOSE ", SLIMMED_CSAR (-s)" $SLIMMED_CSAR

if [[ $CLEAN = true ]]; then
    log "Cleaning temp workdir '$TEMP_WORKDIR'"
    rm -rf "$TEMP_WORKDIR"
    clearLocalImages
    checkExitCode "Failed to clean TEMP_WORKDIR '$TEMP_WORKDIR'"
fi

packageAppIntoCsar
