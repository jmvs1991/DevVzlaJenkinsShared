def call(String project){
  pipeline {
      agent any
      environment{
          TELEGRAM_BOT_TOKEN = credentials('telegram_bot_token')
          TELEGRAM_CHANNEL = credentials('telegram_channel_id')
          PATH_PRJ = "./${project}/${project}.csproj"
      }
      stages {
          stage('Restore') {
              steps {
                  echo 'Restore Project'
                  sh 'dotnet clean'
                  sh 'dotnet restore --no-cache'
              }
          }
          stage('Build'){
              when{
                  anyOf{
                      changeset "${project}/**/*"
                  }
              }
              steps {
                  echo 'Build..'
                  sh "dotnet build -c Release ${PATH_PRJ}"
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