#!/usr/bin/env groovy

stage('helm-init-custom') {
    echo "creating short form KPI Definitins for App staging"
    script {
        sh "chmod 777 ./ci/scripts/updateKpiDefinitions.sh"
        sh "./ci/scripts/updateKpiDefinitions.sh ./src/main/resources/KpiDefinitions.json ./src/main/resources/KpiDefinitionsShortForm.json"
    }
}