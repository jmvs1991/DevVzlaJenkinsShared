def call(String project, String artifactName, String dotnet = "net6") {
    pipeline {
        agent any
        tools {
            dotnetsdk "${dotnet}"
        }
        environment {
            TELEGRAM_BOT_TOKEN = credentials('telegram_bot_token')
            TELEGRAM_CHANNEL = credentials('telegram_channel_id')
            PATH_PRJ = "./${project}/${project}.csproj"
            PATH_PUB = "./${project}/bin/Release/net6.0/publish"
            ARTIFACT = "${artifactName}_${env.BRANCH_NAME}_${BUILD_NUMBER}.zip"
        }
        stages {
            stage('Restore') {
                steps {
                    echo 'Restore Project'
                    sh 'dotnet clean'
                    sh "dotnet restore ${PATH_PRJ} --no-cache"
                }
            }
            stage('Build') {
                steps {
                    echo 'Build..'
                    sh "dotnet publish -c Release ${PATH_PRJ}"
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
