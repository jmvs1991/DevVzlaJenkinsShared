def call(String project, String artifactName, boolean withTest, String dotnet = "net6"){
  pipeline {
      agent any
      tools {
        dotnetsdk "${dotnet}"
      }
      environment{
          TELEGRAM_BOT_TOKEN = credentials('telegram_bot_token')
          TELEGRAM_CHANNEL = credentials('telegram_channel_id')
          PATH_TEST = "./${project}Test/${project}Test.csproj"
          PATH_PRJ = "./${project}/${project}.csproj"
          PATH_PUB = "./${project}/bin/Release/net6.0/publish"
          ARTIFACT = "${artifactName}_${env.BRANCH_NAME}_${BUILD_NUMBER}.zip"
      }
      stages {
          stage('Restore') {
              steps {
                  echo 'Restore Project'
                  sh 'dotnet clean'
                  sh 'dotnet restore --no-cache'
              }
          }
          stage('Test'){
              when{
                  anyOf{
                      changeset "${project}/**/*"
                      changeset "${project}Services/**/*"
                      changeset "${project}Test/**/*"
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
          stage('Build'){
              when{
                  anyOf{
                      branch 'develop'
                      branch 'stage'
                      branch 'main'
                  }
                  anyOf{
                      changeset "${project}/**/*"
                      changeset "${project}Services/**/*"
                  }
              }
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
