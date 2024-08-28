#!/usr/bin/env groovy

def mergedContract="${WORKSPACE}/target/merged-contract-report.txt"
def mergedJunitAndSpock="${WORKSPACE}/target/merged-junit-and-spock-report.txt"

stage('Archive Test Results') {
    sh 'chmod 777 ci/scripts/appendTestTotals.sh'
    sh "ci/scripts/appendTestTotals.sh ${mergedContract} ${mergedJunitAndSpock}"

    archiveArtifacts allowEmptyArchive: true, artifacts: 'target/*.txt'
}
