#!/usr/bin/env groovy
def bob = "./bob/bob -r \${WORKSPACE}/ci/post_instantiation.yaml"
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
        string(name: 'CHART_VERSION',
                defaultValue: '',
                description: 'The version of the epme testware chart')
        string(name: 'EPME_DEPLOYMENT',
                defaultValue: 'eric-oss-performance-monitoring-enabler',
                description: 'Deployment instance name of EPME')
        string(name: 'EPME_PREFIX',
                defaultValue: '/performance-monitoring-enabler',
                description: 'Prefix for EPME instance')
        string(name: 'FUNCTIONAL_USER_SECRET',
                defaultValue: 'cloudman-user-creds',
                description: 'Jenkins secret ID for ARM Registry Credentials')
        string(name: 'DOCKER_REPO',
                defaultValue: 'proj-eric-oss-drop',
                description: 'The PME docker repo')
        string(name: 'HELM_REPO',
                defaultValue: 'proj-eric-oss-released-helm-local',
                description: 'The PME helm repo')
        string(name: 'INGRESS_SESSION_OPERATOR',
                defaultValue: 'pme-session-operator',
                description: 'The PME session operator username')
        string(name: 'INGRESS_CONFIGURATION_OPERATOR',
                defaultValue: 'pme-configuration-operator',
                description: 'The PME configuration operator username')
        string(name: 'INGRESS_CONFIGURATION_READER',
                defaultValue: 'pme-configuration-reader',
                description: 'The PME configuration reader username')
        string(name: 'INGRESS_PME_TESTWARE_USER',
                defaultValue: 'pme-testware-user',
                description: 'The username for EPME testware user')
        booleanParam(name: 'INSTALL_K6_DEV_SECRETS',
                defaultValue: false,
                description: 'If true installs the K6 testware resources secrets (ONLY required for unofficial or test builds)')
        booleanParam(name: 'KAFKA_TLS_ENABLED',
                defaultValue: false,
                description: 'The Kafka tls value')
    }

    options {
        skipDefaultCheckout true
        timestamps()
        timeout(time: 120, unit: 'MINUTES')
        buildDiscarder(logRotator(daysToKeepStr: '14', numToKeepStr: '40', artifactNumToKeepStr: '40', artifactDaysToKeepStr: '14'))
    }

     environment {
        INGRESS_SCHEMA = "${params.INGRESS_PREFIX}"
        INGRESS_HOST = "${params.INGRESS_HOST}"
        INGRESS_LOGIN_USER = "${params.INGRESS_LOGIN_USER}"
        INGRESS_LOGIN_PASSWORD = "${params.INGRESS_LOGIN_PASSWORD}"
        STAGING_LEVEL = "PRODUCT"
        TEST_PHASE = "POST_INSTANTIATION"
        KUBECONFIG_FILE_CREDENTIAL_ID = "${params.KUBECONFIG_FILE_CREDENTIAL_ID}"
        NAMESPACE = "${params.NAMESPACE}"
        OPTIONS_FILE = "postInstantiation.options.json"
        CHART_VERSION = "${params.CHART_VERSION}"
        FUNCTIONAL_USER_SECRET = "${params.FUNCTIONAL_USER_SECRET}"
        DOCKER_REPO = "${params.DOCKER_REPO}"
        HELM_REPO = "${params.HELM_REPO}"
        INGRESS_PME_TESTWARE_USER = "${params.INGRESS_PME_TESTWARE_USER}"
        INSTALL_K6_DEV_SECRETS = "${params.INSTALL_K6_DEV_SECRETS}"
        EPME_DEPLOYMENT = "${params.EPME_DEPLOYMENT}"
        EPME_PREFIX = "${params.EPME_PREFIX}"
        INGRESS_SESSION_OPERATOR = "${params.INGRESS_SESSION_OPERATOR}"
        INGRESS_CONFIGURATION_OPERATOR = "${params.INGRESS_CONFIGURATION_OPERATOR}"
        INGRESS_CONFIGURATION_READER = "${params.INGRESS_CONFIGURATION_READER}"
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
                archiveArtifacts allowEmptyArchive: true, artifacts: 'ci/postInstantiation.Jenkinsfile'
                sh '''
                       git submodule sync
                       git submodule update --init --recursive --remote
                   '''
            }
        }
        stage('Test Preparation') {
            steps {
                withCredentials([file(credentialsId: "$KUBECONFIG_FILE_CREDENTIAL_ID", variable: 'KUBECONFIG_FILE_PATH')]) {
                    sh """
                        chmod 777 ./ci/scripts/postInstantiation_test_prep.sh
                        ./ci/scripts/postInstantiation_test_prep.sh
                    """
                    script {
                        def props = readProperties(file: "$WORKSPACE/prep-variables.properties")
                        env.RAPP_ID = props.RAPP_ID
                    }
                    sh "${bob} test-preparation-post-instantiation"
                }
            }
        }
        stage('K6 Post Instantiation E2E Tests') {
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

