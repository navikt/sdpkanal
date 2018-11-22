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
                init action: 'gradle'
            }
        }
        stage('build') {
            steps {
                sh './gradlew build -x test'
            }
        }
        stage('run tests (unit & intergration)') {
            steps {
                sh './gradlew test'
                slackStatus status: 'passed'
            }
        }
        stage('extract application files') {
            steps {
                sh './gradlew installDist'
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
            junit testResults: '**/target/surefire-reports/*.xml', allowEmptyResults: true
            junit testResults: '**/target/failsafe-reports/*.xml', allowEmptyResults: true
            archiveArtifacts artifacts: '**/target/*.jar', allowEmptyArchive: true
        }
        success {
            postProcess action: 'success'
        }
        failure {
            postProcess action: 'failure'
        }
    }
}

tasks {
    "printVersion" {
        println(project.version)
    }
}
