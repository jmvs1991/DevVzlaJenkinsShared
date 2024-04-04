def call(String project) {
    pipeline {
        agent any
        environment {
            TELEGRAM_BOT_TOKEN = credentials('telegram_bot_token')
            TELEGRAM_CHANNEL = credentials('telegram_channel_id')
            AWS_CODE_ARTIFACT_DOMAIN = credentials('aws-code-artifact-domain')
            AWS_CODE_ARTIFACT_DOMAIN_OWNER = credentials('aws-code-artifact-domain-owner')
            AWS_DEFAULT_REGION = credentials('aws-default-region')
            AWS_SOURCE = credentials('aws-source')
            PATH_PRJ = "./${project}.csproj"
        }
        stages {
            stage('Login') {
                when {
                    anyOf {
                        branch 'main'
                    }
                }
                steps {
                    awsLogin(AWS_CODE_ARTIFACT_DOMAIN, AWS_CODE_ARTIFACT_DOMAIN_OWNER, AWS_DEFAULT_REGION)
                }
            }
            stage('Pack') {
                when {
                    anyOf {
                        branch 'main'
                    }
                }
                steps {
                    echo 'Pack..'
                    sh "dotnet pack -c Release ./ --output nupkgs"
                }
            }
            stage('Publish') {
                when {
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
            }
        }
    }
}
