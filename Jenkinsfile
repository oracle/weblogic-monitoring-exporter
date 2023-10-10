// Copyright (c) 2023, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
//
pipeline {
    agent any

    tools {
        maven 'maven-3.8.7'
        jdk 'jdk21'
    }

    stages {
        stage ('Build') {
            steps {
                withMaven(globalMavenSettingsConfig: 'wkt-maven-settings-xml', publisherStrategy: 'EXPLICIT') {
                    sh "mvn -DtrimStackTrace=false clean install"
                }
            }
        }
        stage ('Sync') {
            when {
                branch 'main'
                anyOf {
                    not { triggeredBy 'TimerTrigger' }
                    tag 'v*'
                }
            }
            steps {
                build job: "wkt-sync", parameters: [ string(name: 'REPOSITORY', value: 'weblogic-monitoring-exporter') ]
            }
        }
    }
    post {
        failure {
            slackSend channel: '#wkt-build-failure-notifications',
                      botUser: false, color: 'danger',
                      message: "Build <${env.BUILD_URL}|${env.JOB_NAME}:${env.BUILD_NUMBER}> failed"
        }
    }
}