#!/usr/bin/env groovy
def bob = "./bob/bob -r \${WORKSPACE}/ci/app_staging.yaml"

pipeline {
    agent {
        label env.SLAVE_LABEL
    }

    parameters {
        string(name: 'GERRIT_REFSPEC',
                defaultValue: 'refs/heads/master',
                description: 'Referencing to a commit by Gerrit RefSpec')
        string(name: 'SLAVE_LABEL',
                defaultValue: 'GridEngine',
                description: 'Specify the slave label that you want the job to run on')
        string(name: 'INGRESS_PREFIX',
                defaultValue: 'http',
                description: 'The prefix to the ingress URL')
        string(name: 'INGRESS_HOST',
                defaultValue: 'hall127.rnd.gic.ericsson.se',
                description: 'The host to run on')
        string(name: 'DOCKER_REPO',
                defaultValue: 'proj-eric-oss-dev',
                description: 'The PME docker repo')
        string(name: 'HELM_REPO',
                defaultValue: 'proj-eric-oss-released-helm-local',
                description: 'The PME helm repo')
        string(name: 'CHART_VERSION',
                defaultValue: '',
                description: 'The PME Version under test')
        string(name: 'KUBECONFIG_FILE_CREDENTIAL_ID',
                defaultValue: 'hall127_kubeconfig',
                description: 'The jenkins credential for the kubeconfig of the environment')
        string(name: 'NAMESPACE',
                defaultValue: 'pme-int-testing',
                description: 'The namespace to run the App Staging')
        string(name: 'FUNCTIONAL_USER_SECRET',
                defaultValue: 'cloudman-user-creds',
                description: 'Jenkins secret ID for ARM Registry Credentials')
        booleanParam(name: 'INSTALL_K6_DEV_SECRETS',
                defaultValue: false,
                description: 'If true installs the K6 testware resources secrets (ONLY required for unoffical or test builds)')
        booleanParam(name: 'KAFKA_TLS_ENABLED',
                defaultValue: false,
                description: 'The Kafka tls value')
    }

    options {
        timestamps()
        timeout(time: 60, unit: 'MINUTES')
        buildDiscarder(logRotator(daysToKeepStr: '14', numToKeepStr: '40', artifactNumToKeepStr: '40', artifactDaysToKeepStr: '14'))
    }

     environment {
        INGRESS_SCHEMA = "${params.INGRESS_PREFIX}"
        INGRESS_HOST = "${params.INGRESS_HOST}"
        DOCKER_REPO = "${params.DOCKER_REPO}"
        HELM_REPO = "${params.HELM_REPO}"
        CHART_VERSION = "${params.CHART_VERSION}"
        KUBECONFIG_FILE_CREDENTIAL_ID = "${params.KUBECONFIG_FILE_CREDENTIAL_ID}"
        NAMESPACE = "${params.NAMESPACE}"
        STAGING_LEVEL = "APP"
        OPTIONS_FILE = "appStaging.options.json"
        FUNCTIONAL_USER_SECRET = "${params.FUNCTIONAL_USER_SECRET}"
        INSTALL_K6_DEV_SECRETS = "${params.INSTALL_K6_DEV_SECRETS}"
        KAFKA_TLS_ENABLED = "${params.KAFKA_TLS_ENABLED}"
    }

    stages {
        stage('Clean') {
            steps {
                sh '''
                    rm ./charts/eric-oss-performance-monitoring-enabler-integration/*.lock || true
                    rm ./charts/eric-oss-performance-monitoring-enabler-integration/charts/*.tgz || true
                    rm -rf .helm
                    git submodule sync
                    git submodule update --init --recursive --remote
                '''
                withCredentials([file(credentialsId: "$KUBECONFIG_FILE_CREDENTIAL_ID", variable: 'KUBECONFIG_FILE_PATH')]) {
                sh "${bob} clean-env"
                }
            }
        }
        stage('Helm Dependency Update') {
            steps {
                 script {
                    withCredentials([usernamePassword(credentialsId: 'SELI_ARTIFACTORY', usernameVariable: 'SELI_ARTIFACTORY_REPO_USER', passwordVariable: 'SELI_ARTIFACTORY_REPO_PASS')
                    ]) {
                        sh "${bob} helm-repo-init"
                    }
                }
            }
        }
        stage('Integration Install') {
            steps {
                sh "${bob} helm-install-dependencies"
            }
        }
        stage('Create Kafka Topics') {
            steps {
                withCredentials([file(credentialsId: "$KUBECONFIG_FILE_CREDENTIAL_ID", variable: 'KUBECONFIG_FILE_PATH')]) {
                sh "${bob} create-kafka-topics"
                }
            }
        }
        stage('Service Install') {
            steps {
                sh "${bob} helm-install-pme"
            }
        }
        stage('Test Preparation') {
            steps {
                sh "${bob} test-preparation"
            }
        }
        stage('K6 EPME App Integration Tests') {
            steps {
                ansiColor('xterm') {
                    withCredentials([
                        file(credentialsId: "$KUBECONFIG_FILE_CREDENTIAL_ID", variable: 'KUBECONFIG_FILE_PATH'),
                        usernamePassword(credentialsId: "$FUNCTIONAL_USER_SECRET", usernameVariable: 'FUNCTIONAL_USER_USERNAME', passwordVariable: 'FUNCTIONAL_USER_PASSWORD')]) {
                            sh '''
                                chmod 777 ci/scripts/createKafkaUser.sh
                                chmod 777 ci/scripts/k6-testware-execution.sh
                                ci/scripts/k6-testware-execution.sh
                            '''
                        }
                }
            }
            post {
                always {
                    archiveArtifacts allowEmptyArchive: true, artifacts: 'doc/Test_Report/k6.log.gz'
                }
            }
        }
    }
    post {
            always {
                script {
                    sh "rm -rf ./*"
                }
            }
     }
}