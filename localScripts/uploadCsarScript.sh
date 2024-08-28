#! /bin/bash
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

HOSTNAME=gas.stsvp1eic28.stsoss.sero.gic.ericsson.se
USERNAME=appmgr-user
PASSWORD=idunEr!css0n
FILE=


function log() {
  echo -e "\n$(date) --- ${1} \n"
}

function checkExitCode() {
    if [ $? -ne 0 ]; then
          log "ERROR: $1 "
          exit 255
    fi
}

function readArgs(){
  while getopts h:u:p:f: flag
  do
    case "${flag}" in
      h) HOSTNAME=${OPTARG};;
      u) USERNAME=${OPTARG};;
      p) PASSWORD=${OPTARG};;
      f) FILE=${OPTARG};;
    esac
  done
}

function getSessionId() {
  JSESSIONID=$(
    curl -sk --location --request POST "https://${HOSTNAME}/auth/v1/login" \
    --header "X-Login: ${USERNAME}" \
    --header "X-password: ${PASSWORD}"
  )

  checkExitCode "Failed to get JSESSIONID"

  log "JSESSIONID: ${JSESSIONID}"
}

function uploadCsar() {
  log "Uploading CSAR file ${FILE}"

  curl -k# --location --request POST "https://${HOSTNAME}/app-manager/onboarding/v1/apps" \
    --header "Cookie: JSESSIONID=${JSESSIONID}" \
    -o /tmp/response_body.txt \
    --form "file=@\"${FILE}\"" | echo

  checkExitCode "Failed to get JSESSIONID"

  cat /tmp/response_body.txt

  log "Uploading CSAR file successfully"
}

#########
# main
#########

readArgs $@

if [[ -z "${FILE}" ]]; then
  log "ERROR: argument -f <path-to-file> is missing. The following arguments can be used:
    -h: AppMgr Hostname (default: gas.stsvp1eic28.stsoss.sero.gic.ericsson.se)
    -u: AppMgr Username (default: appmgr-user)
    -p: AppMgr PASSWORD (default: idunEr!css0n)
    -f: File path to the CSAR (required)"
  exit 255
fi

log "Running with arguments:
  HOSTNAME (-h): ${HOSTNAME}
  USERNAME (-u): ${USERNAME}
  PASSWORD (-p): ${PASSWORD}
  FILE (-f): ${FILE}"

rm /tmp/response_body.txt
getSessionId
uploadCsar
