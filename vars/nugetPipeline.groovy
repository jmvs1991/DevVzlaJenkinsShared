def call(String project, String folder){
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
          PATH_PRJ = "./${folder}/${project}.csproj"
          PATH_PKG = "./${folder}/bin/Release/*.nupkg"
      }
      stages {
        stage('Login'){
            when{
                anyOf{
                    changeset "${folder}/**/*"
                }
            }
            steps {
                echo 'Build..'
                sh "aws configure set aws_access_key_id ${AWS_ACCESS_KEY_ID}"
                sh "aws configure set aws_secret_access_key ${AWS_SECRET_ACCESS_KEY}"
                sh "aws codeartifact login --tool dotnet --repository ${AWS_CODE_ARTIFACT_REPOSITORY} --domain ${AWS_CODE_ARTIFACT_DOMAIN} --domain-owner ${AWS_CODE_ARTIFACT_DOMAIN_OWNER} --region ${AWS_DEFAULT_REGION}"
            }
        }    
        stage('Restore') {
            when{
                anyOf{
                    changeset "${folder}/**/*"
                }
            }
            steps {
                echo 'Restore Project'
                sh 'dotnet clean'
                sh "dotnet restore ${PATH_PRJ} --no-cache"
            }
        }
        stage('Build'){
            when{
                anyOf{
                    changeset "${folder}/**/*"
                }
            }
            steps {
                echo 'Build..'
                sh "dotnet build -c Release ${PATH_PRJ}"
            }
        }
        stage('Publish'){
            when{
                anyOf{
                    branch 'main'
                }
                anyOf{
                    changeset "${folder}/**/*"
                }
            }
            steps {
                echo 'Pushing pkg..'
                sh "dotnet nuget push ${PATH_PKG} --source ${AWS_SOURCE}"
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