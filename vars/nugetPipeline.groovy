def call(String project, String folder){
  pipeline {
      agent any
      environment{
          TELEGRAM_BOT_TOKEN = credentials('telegram_bot_token')
          TELEGRAM_CHANNEL = credentials('telegram_channel_id')
          AWS_CODE_ARTIFACT_DOMAIN = credentials('aws-code-artifact-domain')
          AWS_CODE_ARTIFACT_DOMAIN_OWNER = credentials('aws-code-artifact-domain-owner')
          AWS_CODE_ARTIFACT_REPOSITORY = credentials('aws-code-artifact-repository')
          AWS_DEFAULT_REGION = credentials('aws-default-region')
          PATH_PRJ = "./${folder}/${project}.csproj"
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
              // when{
              //     anyOf{
              //         changeset "${folder}/**/*"
              //     }
              // }
              steps {
                  echo 'Build..'
                  sh "dotnet build -c Release ${PATH_PRJ}"
              }
          }
          stage('Login'){
              // when{
              //     anyOf{
              //         changeset "${folder}/**/*"
              //     }
              // }
              steps {
                  echo 'Build..'
                  sh "aws codeartifact login --tool dotnet --repository ${AWS_CODE_ARTIFACT_REPOSITORY} --domain ${AWS_CODE_ARTIFACT_DOMAIN} --domain-owner ${AWS_CODE_ARTIFACT_DOMAIN_OWNER} --region ${AWS_DEFAULT_REGION}"
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