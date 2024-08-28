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

HOSTNAME="eic.stsvp1eic28.stsoss.sero.gic.ericsson.se"
IAM_URL="https://${HOSTNAME}"
IAM_LOGIN="kcadmin"
IAM_PASSWORD="idunEr!css0n"
# Kebab-case for subscription-friendly ID
IAM_CLIENT="epme-local-client"

access_token=

function log() {
  echo -e "\n$(date) --- ${1} \n"
}

function readArgs(){
  while getopts h:u:p:c: flag
  do
    case "${flag}" in
      h) HOSTNAME=${OPTARG};;
      u) IAM_LOGIN=${OPTARG};;
      p) IAM_PASSWORD=${OPTARG};;
      c) IAM_CLIENT=${OPTARG};;
    esac
  done
}

function getAccessToken() {
  echo "Retrieving access token"

  access_token=$(curl -sk --location "${IAM_URL}/auth/realms/master/protocol/openid-connect/token" \
                   --header 'Content-Type: application/x-www-form-urlencoded' \
                   --data-urlencode 'grant_type=password' \
                   --data-urlencode "username=${IAM_LOGIN}" \
                   --data-urlencode "password=${IAM_PASSWORD}" \
                   --data-urlencode 'client_id=admin-cli' | jq -r .access_token)
}

function createNewClient() {
  echo "Creating new client"

  status_code=$(curl -sk -w '%{response_code}' -o /tmp/response_body.txt -X POST "${IAM_URL}/auth/admin/realms/master/clients"  \
    --data "{
      \"clientId\":\"${IAM_CLIENT}\",
      \"clientAuthenticatorType\":\"client-secret\",
      \"enabled\": true,
      \"attributes\": {
        \"ExternalClient\": \"true\"
      },
      \"publicClient\": false,
      \"standardFlowEnabled\": false,
      \"serviceAccountsEnabled\": true,
      \"protocol\":\"openid-connect\"
    }" \
    --header "Content-Type: application/json" \
    --header "Authorization: Bearer ${access_token}")

  echo "Status code of create client :: ${status_code}"

  if [[ "${status_code}" -ne '201' ]] && [[ "${status_code}" -ne '409' ]] ; then
    echo "Failed to create client. Status code:: ${status_code}"
    echo "Response body:"
    cat /tmp/response_body.txt
    exit 1
  fi
}

function getRoles() {
  roles=$(curl -sk -X GET "${IAM_URL}/auth/admin/realms/master/roles"  \
    --header "Content-Type: application/json" \
    --header "Authorization: Bearer ${access_token}")
  echo "${roles}"
}



#########
# main
#########

readArgs $@

log "Running with arguments:
  HOSTNAME (-h): ${HOSTNAME}
  IAM_LOGIN (-u): ${IAM_LOGIN}
  IAM_PASSWORD (-p): ${IAM_PASSWORD}
  IAM_CLIENT (-c): ${IAM_CLIENT}"

getAccessToken

createNewClient

# Assign roles to client
client=$(curl -sk -X GET "${IAM_URL}/auth/admin/realms/master/clients?clientId=${IAM_CLIENT}"  \
  --header "Content-Type: application/json" \
  --header "Authorization: Bearer ${access_token}" | jq .[0])

serviceAccountId=$(curl -sk "${IAM_URL}/auth/admin/realms/master/clients/$(echo "${client}" | jq -r .id)/service-account-user" \
                   --header "Authorization: Bearer ${access_token}" | jq -r .id)

echo "Retrieving roles"
roles=$(getRoles)
echo "Assigned all roles to client"

# Assign roles to client
curl -k "${IAM_URL}/auth/admin/realms/master/users/${serviceAccountId}/role-mappings/realm" \
  --header 'Content-Type: application/json' \
  --header "Authorization: Bearer ${access_token}" \
  --data "${roles}"

echo -e "\n\n"

echo "==================================================================
  Client ID: $(echo "${client}" | jq -r -C .clientId)
  Client Secret: $(echo "${client}" | jq -r -C .secret)
=================================================================="