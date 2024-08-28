#!/usr/bin/env groovy

def bob = "./bob/bob"
def ruleset = "ci/testware/build-ruleset.yaml"

stage('Testware Build') {
    echo "Building epme testware"
    script {
        sh """
            chmod 777 ./epme-testware/gradlew
        """
        withCredentials([usernamePassword(credentialsId: 'SELI_ARTIFACTORY', usernameVariable: 'SELI_ARTIFACTORY_REPO_USER', passwordVariable: 'SELI_ARTIFACTORY_REPO_PASS')]) {
            ansiColor('xterm') {
                if (env.RELEASE) {
                    sh "${bob} -r ${ruleset} publish-pipeline"
                } else {
                    sh "${bob} -r ${ruleset} pcr-pipeline"
                }
            }
        }
    }
}