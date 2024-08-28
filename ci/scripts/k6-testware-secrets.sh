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

ENVIRONMENT="$1"
FILE_NAME="testware-resources-secret.yaml"

# This is for design purposes and only for the team cluster, run this to generate a new testware-resources-secret.
# This output is currently stored in /ci/testware/testware-resources-secret.yaml

# Development environemnt settings will be used if no value is passed to the script.
API_URL=http://api.dev-staging-report.ews.gic.ericsson.se/api
GUI_URL=http://gui.dev-staging-report.ews.gic.ericsson.se/staging-reports
DATABASE_URL=postgresql://testware_user:testware@kroto017.rnd.gic.ericsson.se:30001/staging

if [[ "$ENVIRONMENT" == "APP" ]]; then
  echo "Using App Staging environment variables"
  FILE_NAME="app-staging-resources-secret.yaml"
  API_URL=http://api.app-staging-report.ews.gic.ericsson.se/api
  GUI_URL=http://gui.app-staging-report.ews.gic.ericsson.se/staging-reports
  DATABASE_URL=postgresql://testware_user:testware@kroto018.rnd.gic.ericsson.se:30004/staging
fi

if [[ "$ENVIRONMENT" == "PRODUCT" ]]; then
  echo "Using Product Staging environment variables"
  FILE_NAME="product-staging-resources-secret.yaml"
  API_URL=http://api.prod-staging-report.ews.gic.ericsson.se/api
  GUI_URL=http://gui.prod-staging-report.ews.gic.ericsson.se/staging-reports
  DATABASE_URL=postgresql://testware_user:testware@kroto018.rnd.gic.ericsson.se:30005/staging
fi

api_url_encoded=$(printf "$API_URL" | base64)
gui_url_encoded=$(printf "$GUI_URL" | base64)
database_url_encoded=$(printf "$DATABASE_URL" | base64)

# The cat command is putting the encoded url over multiple lines, check the output after running this script.
cat > "../testware/secrets/$FILE_NAME" <<EOL
apiVersion: v1
kind: Secret
metadata:
  name: testware-resources-secret
type: Opaque
data:
  api_url: $api_url_encoded
  gui_url: $gui_url_encoded
  database_url: $database_url_encoded
EOL