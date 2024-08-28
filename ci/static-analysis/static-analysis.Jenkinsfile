#!/usr/bin/env groovy

def bob = "./bob/bob"
def ruleset = "ci/static-analysis/static-analysis-ruleset.yaml"

stage('Static Analysis Checker') {
    script {
        try {
            sh "${bob} -r ${ruleset} build-k6-static-analysis-image"
            sh "${bob} -r ${ruleset} static-analysis"
        } finally {
            sh "${bob} -r ${ruleset} delete-k6-static-analysis-image"
        }
    }
}
