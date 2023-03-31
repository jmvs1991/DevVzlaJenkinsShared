def call(String project, String artifact){
  pipeline{
      agent any
      environment{
          PATH_PRJ = "./${project}/${project}.csproj"
          PATH_PUB = "./${project}/bin/Release/net6.0/publish"
          ARTIFACT = "artifact_${env.BRANCH_NAME}_${BUILD_NUMBER}.zip"
      }
      stages{
          stage('Restore') {
              steps {
                  echo 'Restore Project'
                  sh 'dotnet clean'
                  sh 'dotnet restore --no-cache'
              }
          }
          stage('Build') {
              when{
                  anyOf{
                      branch 'develop'
                      branch 'stage'
                      branch 'main'
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
        }
      }
  }
}
