#!/usr/bin/env groovy

def bob = "./bob/bob"
def ruleset = "ci/va-analysis/va-analysis-ruleset.yaml"

if (env.RELEASE) {
    stage('Additional VA scans') {
        parallel(
            "owasp_zap": {
                script{
                    withCredentials([usernamePassword(credentialsId: 'SERO_ARTIFACTORY', usernameVariable: 'SERO_ARTIFACTORY_REPO_USER', passwordVariable: 'SERO_ARTIFACTORY_REPO_PASS')]) {
                        sh "${bob} -r ${ruleset} zap-test"
                        archiveArtifacts "build/va-reports/zap-reports/*"
                    }
                }
            },
            "Trivy": {
                script {
                    sh "${bob} -r ${ruleset} trivy-inline-scan"
                    archiveArtifacts "build/va-reports/trivy-reports/**.*"
                    archiveArtifacts "trivy_metadata.properties"
                 }
            },
            "X-Ray": {
                script {
                    sleep(60)
                    withCredentials([usernamePassword(credentialsId: 'XRAY_SELI_ARTIFACTORY', usernameVariable: 'XRAY_USER', passwordVariable: 'XRAY_APIKEY')]) {
                        ci_pipeline_scripts.retryMechanism("${bob} -r ${ruleset} fetch-xray-report", 3)
                    }
                    archiveArtifacts "build/va-reports/xray-reports/xray_report.json"
                    archiveArtifacts "build/va-reports/xray-reports/raw_xray_report.json"
                 }
            },
            "Anchore-Grype": {
                script {
                    sh "${bob} -r ${ruleset} anchore-grype-scan"
                    archiveArtifacts "build/va-reports/anchore-reports/**.*"
                 }
            },
            "Kubehunter": {
                    script {
                        configFileProvider([configFile(fileId: "${K8S_CLUSTER_ID}", targetLocation: "${env.KUBECONFIG}")]) {}
                        sh "${bob} -r ${ruleset} kubehunter-scan"
                        archiveArtifacts "build/va-reports/kubehunter-report/**/*"
                    }
            },
            "NMAP Unicorn": {
                script {
                    withCredentials([usernamePassword(credentialsId: 'SERO_ARTIFACTORY', usernameVariable: 'SERO_ARTIFACTORY_REPO_USER', passwordVariable: 'SERO_ARTIFACTORY_REPO_PASS')]){
                            configFileProvider([configFile(fileId: "${K8S_CLUSTER_ID}", targetLocation: "${env.KUBECONFIG}")]) {}
                            sh "${bob} -r ${ruleset} nmap-port-scanning"
                            archiveArtifacts "build/va-reports/nmap_reports/**/**.*"
                     }
                 }
            }
        )
    }
    stage('Generate Vulnerability report V2.0') {
            script {
                sh "${bob} -r ${ruleset} generate-VA-report-V2:create-va-folders"
                sh "${bob} -r ${ruleset} generate-VA-report-V2:no-upload"
                archiveArtifacts allowEmptyArchive: true, artifacts: 'build/va-reports/Vulnerability_Report_2.0.md'

                sh "${bob} -r ${ruleset} delete-images:cleanup-anchore-trivy-images"
            }
    }
}