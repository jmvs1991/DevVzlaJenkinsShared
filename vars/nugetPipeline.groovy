def call(String project, String folder, String jenkinsfile, boolean forceSteps = false, boolean forcePublish = false, String dotnet = "net6") {
    pipeline {
        agent any
        tools {
            dotnetsdk "${dotnet}"
        }
        environment {
            TELEGRAM_BOT_TOKEN = credentials('telegram_bot_token')
            TELEGRAM_CHANNEL = credentials('telegram_channel_id')
            AWS_CODE_ARTIFACT_DOMAIN = credentials('aws-code-artifact-domain')
            AWS_CODE_ARTIFACT_DOMAIN_OWNER = credentials('aws-code-artifact-domain-owner')
            AWS_DEFAULT_REGION = credentials('aws-default-region')
            AWS_SOURCE = credentials('aws-source')
            PATH_PRJ = "./${folder}/${project}.csproj"
            PATH_PKG = "./${folder}/bin/Release/*.nupkg"
        }
        stages {
            stage('Login') {
                when {
                    anyOf {
                        tag "release-${project}-*"
                        changeset "${folder}/**/*"
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
                        tag "release-${project}-*"
                        changeset "${folder}/**/*"
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
            stage('Build') {
                when {
                    anyOf {
                        tag "release-${project}-*"
                        changeset "${folder}/**/*"
                        changeset "${jenkinsfile}"
                        expression {
                            forceSteps == true
                        }
                    }
                }
                steps {
                    echo 'Build..'
                    sh "dotnet build -c Release ${PATH_PRJ}"
                }
            }
            stage('Publish') {
                when {
                    anyOf {
                        tag "release-${project}-*"
                        expression {
                            forceSteps == true || forcePublish == true
                        }
                    }
                }
                steps {
                    echo 'Pushing pkg..'
                    sh "dotnet nuget push ${PATH_PKG} --source ${AWS_SOURCE}"
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
