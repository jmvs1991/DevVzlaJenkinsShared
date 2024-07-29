def call(String project, String jenkinsfile, String dotnet = "net6") {
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
        }
        stages {
            stage('Login') {
                when {
                    anyOf {
                        changeset "${project}/**/*"
                        changeset "${jenkinsfile}"
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
                        changeset "${jenkinsfile}"
                    }
                }
                steps {
                    echo 'Restore Project'
                    sh "dotnet clean ${PATH_PRJ}"
                    sh "dotnet restore ${PATH_PRJ} --no-cache"
                }
            }
            stage('Pack') {
                when {
                    anyOf {
                        changeset "${project}/**/*"
                        changeset "${jenkinsfile}"
                    }
                }
                steps {
                    echo 'Pack..'
                    sh "dotnet pack -c Release ${PATH_PRJ} --output nupkgs"
                }
            }
            stage('Publish') {
                when {
                    anyOf {
                        changeset "${project}/**/*"
                    }
                    anyOf {
                        branch 'main'
                    }
                }
                steps {
                    echo 'Pushing pkg..'
                    sh "dotnet nuget push ./nupkgs/*.nupkg --source ${AWS_SOURCE}"
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
