def call(String project, String artifactName, boolean withTest, String jenkinsfile, boolean forceSteps) {
    pipeline {
        agent any
        tools {
            dotnetsdk 'net6'  // The name you gave when configuring the tool
        }
        environment {
            TELEGRAM_BOT_TOKEN = credentials('telegram_bot_token')
            TELEGRAM_CHANNEL = credentials('telegram_channel_id')
            AWS_CODE_ARTIFACT_DOMAIN = credentials('aws-code-artifact-domain')
            AWS_CODE_ARTIFACT_DOMAIN_OWNER = credentials('aws-code-artifact-domain-owner')
            AWS_DEFAULT_REGION = credentials('aws-default-region')
            PATH_TEST = "./${project}Test/${project}Test.csproj"
            PATH_PRJ = "./${project}/${project}.csproj"
            PATH_PUB = "${artifactName}_${env.BRANCH_NAME}"
            ARTIFACT = "${artifactName}_${env.BRANCH_NAME}_${BUILD_NUMBER}.zip"
        }
        stages {
            stage('Login') {
                when {
                    anyOf {
                        changeset "${project}/**/*"
                        changeset "${project}Cache/**/*"
                        changeset "${project}Services/**/*"
                        changeset "${project}Test/**/*"
                        changeset "${jenkinsfile}"
                        expression {
                            forceSteps == true
                        }
                    }
                }
                steps {
                    awsLogin(AWS_CODE_ARTIFACT_DOMAIN, AWS_CODE_ARTIFACT_DOMAIN_OWNER, AWS_DEFAULT_REGION)
                }
            }
            stage('Restore') {
                when {
                    anyOf {
                        changeset "${project}/**/*"
                        changeset "${project}Cache/**/*"
                        changeset "${project}Services/**/*"
                        changeset "${project}Test/**/*"
                        changeset "${jenkinsfile}"
                        expression {
                            forceSteps == true
                        }
                    }
                }
                steps {
                    echo 'Restore Project'
                    sh 'dotnet clean'
                    sh "dotnet restore ${PATH_PRJ} --no-cache"
                }
            }
            stage('Test') {
                when {
                    anyOf {
                        changeset "${project}/**/*"
                        changeset "${project}Cache/**/*"
                        changeset "${project}Services/**/*"
                        changeset "${project}Test/**/*"
                        changeset "${jenkinsfile}"
                        expression {
                            forceSteps == true
                        }
                    }
                    expression {
                        withTest == true
                    }
                }
                steps {
                    echo 'Testing..'
                    sh "dotnet test ${PATH_TEST}"
                }
            }
            stage('Build') {
                when {
                    anyOf {
                        branch 'develop'
                        branch 'stage'
                        branch 'main'
                    }
                    anyOf {
                        changeset "${project}/**/*"
                        changeset "${project}Cache/**/*"
                        changeset "${project}Services/**/*"
                        changeset "${jenkinsfile}"
                        expression {
                            forceSteps == true
                        }
                    }
                }
                steps {
                    echo 'Build..'
                    sh "dotnet publish -c Release ${PATH_PRJ} -o ${PATH_PUB}"
                    zip zipFile: "${ARTIFACT}", overwrite: true, archive: false, dir: "${PATH_PUB}"
                    archiveArtifacts artifacts: "${ARTIFACT}", fingerprint: true
                }
            }
        }
        post {
            always {
                sendTelegramNotification(TELEGRAM_BOT_TOKEN, TELEGRAM_CHANNEL)
                cleanWs()
            }
        }
    }
}
