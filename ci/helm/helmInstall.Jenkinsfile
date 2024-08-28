#!/usr/bin/env groovy
def bob = "./bob/bob"
def customRulesBob = "${bob} -r ci/helm/helm-install-ruleset.yaml"
def ciBob = "${bob} -r ci/common_ruleset2.0.yaml"
try {
    stage('Helm Install (Custom)') {
        sh "${ciBob} helm-dry-run"
        sh "${customRulesBob} helm-install"
        sh "${ciBob} healthcheck"
        sh "${ciBob} kaas-info || true"
        archiveArtifacts allowEmptyArchive: true, artifacts: 'build/kaas-info.log'
    }
} catch (e) {
    sh "${ciBob} kaas-info || true"
    archiveArtifacts allowEmptyArchive: true, artifacts: 'build/kaas-info.log'
    withCredentials([usernamePassword(credentialsId: 'SERO_ARTIFACTORY', usernameVariable: 'SERO_ARTIFACTORY_REPO_USER', passwordVariable: 'SERO_ARTIFACTORY_REPO_PASS')]) {
        sh "${ciBob} collect-k8s-logs || true"
    }
    archiveArtifacts allowEmptyArchive: true, artifacts: "k8s-logs/*"
    sh "${ciBob} delete-namespace"
    throw e
}