def call(){
  pipeline{
      agent any
      environment{
          TELEGRAM_BOT_TOKEN = credentials('telegram_bot_token')
          TELEGRAM_CHANNEL = credentials('telegram_channel_id')
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
