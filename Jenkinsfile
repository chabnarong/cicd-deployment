pipeline {
    agent any

    environment {
        SONARQUBE_SERVER = 'sonarqube-server' // Update with your SonarQube server name in Jenkins
        SONARQUBE_TOKEN = credentials('sonarqube-token') // Jenkins credential ID for SonarQube
        JFROG_SERVER = 'jfrog-server' // Update with your JFrog Artifactory server name in Jenkins
        JFROG_CREDENTIALS = credentials('jfrog-credentials') // Jenkins credential ID for JFrog
        ANSIBLE_API_TOKEN = credentials('ansible-api-token') // Jenkins credential ID for Ansible Automation Platform API
        ANSIBLE_API_URL = 'https://ansible-platform.example.com/api/v2' // Ansible Automation Platform API URL
    }

    stages {
        stage('Preparation') {
            steps {
                echo 'Preparing the environment...'
                checkout scm
            }
        }

        stage('Build: .NET Core') {
            steps {
                script {
                    echo 'Building the .NET Core application...'
                    sh 'dotnet restore'
                    sh 'dotnet build --configuration Release'
                }
            }
        }

        stage('Code Scan: SonarQube') {
            steps {
                script {
                    withSonarQubeEnv(SONARQUBE_SERVER) {
                        sh "dotnet sonarscanner begin /k:\"my-project\" /d:sonar.host.url=$SONAR_HOST_URL /d:sonar.login=$SONARQUBE_TOKEN"
                        sh 'dotnet build'
                        sh 'dotnet sonarscanner end /d:sonar.login=$SONARQUBE_TOKEN'
                    }
                }
            }
        }

        stage('Dependency Scan: JFrog Xray') {
            steps {
                script {
                    sh "jfrog rt mvn-dep-tree build --server-id=$JFROG_SERVER --repo-resolve=libs-release --repo-deploy=libs-snapshot"
                }
            }
        }

        stage('Branch Deployment') {
            parallel {
                stage('SIT') {
                    when {
                        branch 'dev'
                    }
                    steps {
                        echo 'Deploying to SIT environment...'
                        script {
                            def response = httpRequest \
                                httpMode: 'POST', \
                                url: "$ANSIBLE_API_URL/job_templates/1/launch/", \
                                customHeaders: [[name: 'Authorization', value: "Bearer $ANSIBLE_API_TOKEN"]], \
                                contentType: 'APPLICATION_JSON', \
                                requestBody: '{"extra_vars": {"backup": true, "stop_iis": true, "start_iis": true}}'
                            echo "Response: ${response.content}"
                        }
                    }
                }

                stage('UAT') {
                    when {
                        branch 'uat'
                    }
                    steps {
                        echo 'Deploying to UAT environment...'
                        script {
                            def response = httpRequest \
                                httpMode: 'POST', \
                                url: "$ANSIBLE_API_URL/job_templates/2/launch/", \
                                customHeaders: [[name: 'Authorization', value: "Bearer $ANSIBLE_API_TOKEN"]], \
                                contentType: 'APPLICATION_JSON', \
                                requestBody: '{"extra_vars": {"backup": true, "stop_iis": true, "start_iis": true}}'
                            echo "Response: ${response.content}"
                        }
                    }
                }

                stage('Production') {
                    when {
                        branch 'prod'
                    }
                    steps {
                        echo 'Awaiting approval for Production deployment...'
                        input {
                            message 'Approve deployment to Production?'
                            ok 'Deploy'
                        }
                        script {
                            def response = httpRequest \
                                httpMode: 'POST', \
                                url: "$ANSIBLE_API_URL/job_templates/3/launch/", \
                                customHeaders: [[name: 'Authorization', value: "Bearer $ANSIBLE_API_TOKEN"]], \
                                contentType: 'APPLICATION_JSON', \
                                requestBody: '{"extra_vars": {"backup": true, "stop_iis": true, "start_iis": true}}'
                            echo "Response: ${response.content}"
                        }
                    }
                }
            }
        }
    }

    post {
        always {
            echo 'Pipeline execution completed.'
        }
        success {
            echo 'Pipeline completed successfully!'
        }
        failure {
            echo 'Pipeline failed. Please check the logs for errors.'
        }
    }
}
