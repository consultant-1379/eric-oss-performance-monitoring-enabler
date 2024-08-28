#!/usr/bin/env groovy

def bob = "./bob/bob"
def ruleset = "ci/helm-unittest_local_ruleset.yaml"

stage('helm-unittest Test') {
    if (env.RELEASE) {
        echo "Skipping helm-unittest Test"
    }
    else {
        script {
            ansiColor('xterm') {
                sh "${bob} -r ${ruleset} test"
            }
        }
    }
}
