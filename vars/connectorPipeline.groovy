def call(String project, String artifact, String dotnet = "net6", String solutionName = "") {
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
            PATH_PRJ = "./${project}/${project}.csproj"
            PATH_PUB = "${artifact}_${env.BRANCH_NAME}"
            ARTIFACT = "${artifact}_${env.BRANCH_NAME}_${BUILD_NUMBER}.zip"
        }
        stages {
            stage('Login') {
                steps {
                    awsLogin(AWS_CODE_ARTIFACT_DOMAIN, AWS_CODE_ARTIFACT_DOMAIN_OWNER, AWS_DEFAULT_REGION)
                }
            }
            stage('Restore') {
                steps {
                    script {
                        def cleanCommand = "dotnet clean"
                        if (solutionName?.trim()) {
                            cleanCommand += " ${solutionName}"
                        }
                        echo 'Restore Project'
                        sh cleanCommand
                    }
                    sh "dotnet restore ${PATH_PRJ} --no-cache"
                }
            }
            stage('Build') {
                when {
                    anyOf {
                        branch 'develop'
                        branch 'stage'
                        branch 'main'
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
