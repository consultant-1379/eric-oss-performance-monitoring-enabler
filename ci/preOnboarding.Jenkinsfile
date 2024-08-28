#!/usr/bin/env groovy

def bob = "./bob/bob  -r \${WORKSPACE}/ci/pre_onboarding.yaml"

pipeline {
    agent {
        label env.SLAVE_LABEL
    }

    parameters {
        string(name: 'GERRIT_REFSPEC',
                defaultValue: 'refs/heads/master',
                description: 'Referencing to a commit by Gerrit RefSpec')
        string(name: 'SLAVE_LABEL',
                defaultValue: 'evo_docker_engine_gic_IDUN',
                description: 'Specify the slave label that you want the job to run on')
        string(name: 'INGRESS_PREFIX',
                defaultValue: '',
                description: 'The prefix to the ingress URL')
        string(name: 'INGRESS_HOST',
                defaultValue: '',
                description: 'The EIC APIGW Host')
        string(name: 'INGRESS_LOGIN_USER',
                defaultValue: '',
                description: 'The user name to use for login')
        string(name: 'INGRESS_LOGIN_PASSWORD',
                defaultValue: '',
                description: 'The password to use')
        string(name: 'KUBECONFIG_FILE_CREDENTIAL_ID',
                defaultValue: '',
                description: 'The jenkins credential for the kubeconfig of the environment')
        string(name: 'NAMESPACE',
                defaultValue: '',
                description: 'The namespace of the environment')
        string(name: 'INSTANTIATION_TYPE',
                defaultValue: '',
                description: 'The type of instantiation to be performed. "initial_instantiation" OR "instantiation_to_higher_version"')
        string(name: 'CHART_VERSION',
                defaultValue: '',
                description: 'The version of the epme testware chart')
        string(name: 'FUNCTIONAL_USER_SECRET',
                defaultValue: 'cloudman-user-creds',
                description: 'Jenkins secret ID for ARM Registry Credentials')
        string(name: 'BUCKET_NAME',
                defaultValue: 'RH_C16A013_OSTK_certificates',
                description: 'The name of bucket to get certs from.')
        string(name: 'DOCKER_REPO',
                defaultValue: 'proj-eric-oss-drop',
                description: 'The PME docker repo')
        string(name: 'HELM_REPO',
                defaultValue: 'proj-eric-oss-released-helm-local',
                description: 'The PME helm repo')
        booleanParam(name: 'KAFKA_TLS_ENABLED',
                        defaultValue: false,
                        description: 'The Kafka tls value')
    }

    options {
        skipDefaultCheckout true
        timestamps()
        timeout(time: 30, unit: 'MINUTES')
        buildDiscarder(logRotator(daysToKeepStr: '14', numToKeepStr: '40', artifactNumToKeepStr: '40', artifactDaysToKeepStr: '14'))
    }

    environment {
        INGRESS_SCHEMA = "${params.INGRESS_PREFIX}"
        INGRESS_HOST = "${params.INGRESS_HOST}"
        INGRESS_LOGIN_USER = "${params.INGRESS_LOGIN_USER}"
        INGRESS_LOGIN_PASSWORD = "${params.INGRESS_LOGIN_PASSWORD}"
        STAGING_LEVEL = "PRODUCT"
        TEST_PHASE = "PRE_ONBOARDING"
        KUBECONFIG_FILE_CREDENTIAL_ID = "${params.KUBECONFIG_FILE_CREDENTIAL_ID}"
        NAMESPACE = "${params.NAMESPACE}"
        INSTANTIATION_TYPE = "${params.INSTANTIATION_TYPE}"
        OPTIONS_FILE = "preOnboarding.options.json"
        CHART_VERSION = "${params.CHART_VERSION}"
        FUNCTIONAL_USER_SECRET = "${params.FUNCTIONAL_USER_SECRET}"
        DOCKER_REPO = "${params.DOCKER_REPO}"
        HELM_REPO = "${params.HELM_REPO}"
        BUCKET_NAME = "${params.BUCKET_NAME}"
        DATAFILE_NAME_LA = "la-http-server"
        DATAFILE_TYPE_CRT = "crt"
        DATAFILE_TYPE_KEY = "key"
        KAFKA_TLS_ENABLED = "${params.KAFKA_TLS_ENABLED}"
    }

    // Stage names (with descriptions) taken from ADP Microservice CI Pipeline Step Naming Guideline: https://eteamspace.internal.ericsson.com/pages/viewpage.action?pageId=1161861067
    stages {
        stage('Clean') {
            steps {
                echo "cleanup workspace using cleanws()"
                cleanWs()
            }
        }
        stage('Checkout SCM') {
            steps {
                echo 'Checkout SCM'
                checkout scm
                archiveArtifacts allowEmptyArchive: true, artifacts: 'ci/preOnboarding.Jenkinsfile'
            }
        }
        stage ('Fetch certs required for MTLS') {
            steps {
                withCredentials([usernamePassword(credentialsId: params.FUNCTIONAL_USER_SECRET, usernameVariable: 'FUNCTIONAL_USER_USERNAME', passwordVariable: 'FUNCTIONAL_USER_PASSWORD')]) {
                    sh '''
                    git submodule sync
                    git submodule update --init --recursive --remote
                    '''
                    sh "${bob} -r ci/pre_onboarding.yaml ost_bucket:download-files-by-name-in-ost-bucket-la-crt ost_bucket:download-files-by-name-in-ost-bucket-la-key"
                }
            }
        }
        stage('Create secrets required for MTLS') {
            steps {
                withCredentials([file(credentialsId: "$KUBECONFIG_FILE_CREDENTIAL_ID", variable: 'KUBECONFIG')]) {
                    sh "kubectl create secret generic mtls-secret --from-file=cert.crt=la-http-server.crt --from-file=keystore.key=la-http-server.key -n $NAMESPACE || true"
                }
            }
        }
        stage('Clean PVCs') {
            when {
                environment name: 'INSTANTIATION_TYPE', value: 'initial_instantiation'
            }
            steps {
                withCredentials([file(credentialsId: "$KUBECONFIG_FILE_CREDENTIAL_ID", variable: 'KUBECONFIG_FILE_PATH')]) {
                    sh "chmod 777 ci/scripts/preOnboarding_env_cleanup.sh"
                    sh "ci/scripts/preOnboarding_env_cleanup.sh"
                }
            }
        }
        stage('K6 Pre On Boarding Tests') {
            steps {
                ansiColor('xterm') {
                    withCredentials([
                        file(credentialsId: "$KUBECONFIG_FILE_CREDENTIAL_ID", variable: 'KUBECONFIG_FILE_PATH'),
                        usernamePassword(credentialsId: "$FUNCTIONAL_USER_SECRET", usernameVariable: 'FUNCTIONAL_USER_USERNAME', passwordVariable: 'FUNCTIONAL_USER_PASSWORD')]) {
                            sh "chmod 777 ci/scripts/createKafkaUser.sh"
                            sh "chmod 777 ci/scripts/k6-testware-execution.sh"
                            sh "ci/scripts/k6-testware-execution.sh "
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
}

