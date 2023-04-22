def call(String folder, String artifactName){
  pipeline{
      agent any
      environment{
          TELEGRAM_BOT_TOKEN = credentials('telegram_bot_token')
          TELEGRAM_CHANNEL = credentials('telegram_channel_id')
          PATH_PUB = "./dist/${project}"
          ARTIFACT = "${artifactName}_${env.BRANCH_NAME}_${BUILD_NUMBER}.zip"
      }
      tools {
        nodejs "NodeJS16"
      }
      stages{
          stage('Install'){
              steps {
                  echo 'Installing'
                  sh 'npm install --exact'
              }
          }
          stage('Linter') {
              steps {
                  echo "Running Linter"
                  sh 'npm run lint'
              }
          }
          stage('Build'){
              steps {
                  echo "Building"
                  sh 'npm run build:develop'
                  zip zipFile: "${ARTIFACT}", overwrite: true, archive: false, dir: "${PATH_PUB}"
                  archiveArtifacts artifacts: "${ARTIFACT}", fingerprint: true
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
