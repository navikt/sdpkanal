#!/usr/bin/env groovy

pipeline {
    agent any

    environment {
        ZONE = 'fss'
        APPLICATION_NAME = 'sdpkanal'
        DOCKER_SLUG = 'integrasjon'
        FASIT_ENVIRONMENT = 'q1'
    }

    stages {
        stage('initialize') {
            steps {
                init action: 'maven'
            }
        }
        stage('build') {
            steps {
                sh 'mvn clean compile -DskipTests'
            }
        }
        stage('run tests (unit & intergration)') {
            steps {
                sh 'mvn verify'
                slackStatus status: 'passed'
            }
        }
        stage('extract application files') {
            steps {
                sh 'mvn package -DskipTests'
            }
        }
        stage('push docker image') {
            steps {
                dockerUtils action: 'createPushImage'
            }
        }
        stage('validate & upload nais.yaml to nexus m2internal') {
            steps {
                nais action: 'validate'
                nais action: 'upload'
            }
        }
        stage('deploy to preprod') {
            steps {
                deployApp action: 'jiraPreprod'
            }
        }
    }
    post {
        always {
            postProcess action: 'always'
            junit '**/build/test-results/test/*.xml'
            archiveArtifacts artifacts: '**/build/libs/*', allowEmptyArchive: true
            archiveArtifacts artifacts: '**/build/install/*', allowEmptyArchive: true
        }
        success {
            postProcess action: 'success'
        }
        failure {
            postProcess action: 'failure'
        }
    }
}
