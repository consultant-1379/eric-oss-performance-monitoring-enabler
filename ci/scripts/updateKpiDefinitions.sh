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

#########
# main
#########
KPI_DEFINITION_FILE=$1
OUTPUT_LOCATION=$2
echo "Removing counterValue field KPI definitions for Application Staging tests"
cp ${KPI_DEFINITION_FILE} ${OUTPUT_LOCATION}
sed -i -e 's/\.counterValue//g' ${OUTPUT_LOCATION}
