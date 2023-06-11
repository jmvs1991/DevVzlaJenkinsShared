def call(String project, String artifactName, boolean withTest){
  pipeline {
      agent any
      environment{
          TELEGRAM_BOT_TOKEN = credentials('telegram_bot_token')
          TELEGRAM_CHANNEL = credentials('telegram_channel_id')
          AWS_CODE_ARTIFACT_DOMAIN = credentials('aws-code-artifact-domain')
          AWS_CODE_ARTIFACT_DOMAIN_OWNER = credentials('aws-code-artifact-domain-owner')
          AWS_CODE_ARTIFACT_REPOSITORY = credentials('aws-code-artifact-repository')
          AWS_ACCESS_KEY_ID = credentials('aws-access-key-id')
          AWS_SECRET_ACCESS_KEY = credentials('aws-secret-access-key-id')
          AWS_DEFAULT_REGION = credentials('aws-default-region')
          AWS_SOURCE = credentials('aws-source')
          PATH_TEST = "./${project}Test/${project}Test.csproj"
          PATH_PRJ = "./${project}/${project}.csproj"
          PATH_PUB = "./${project}/bin/Release/net6.0/publish"
          ARTIFACT = "${artifactName}_${env.BRANCH_NAME}_${BUILD_NUMBER}.zip"
      }
      stages {
          stage('Login'){
                when{
                    anyOf{
                        changeset "${folder}/**/*"
                    }
                }
                steps {
                    echo 'Logn..'
                    sh "aws configure set aws_access_key_id ${AWS_ACCESS_KEY_ID}"
                    sh "aws configure set aws_secret_access_key ${AWS_SECRET_ACCESS_KEY}"
                    sh "aws codeartifact login --tool dotnet --repository ${AWS_CODE_ARTIFACT_REPOSITORY} --domain ${AWS_CODE_ARTIFACT_DOMAIN} --domain-owner ${AWS_CODE_ARTIFACT_DOMAIN_OWNER} --region ${AWS_DEFAULT_REGION}"
                }
          }
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
                      changeset "${project}Cache/**/*"
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
                      changeset "${project}Cache/**/*"
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
        }
    }
  }
}