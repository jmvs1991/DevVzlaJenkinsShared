def call(String project, String folder, String jenkinsfile){
  pipeline {
      agent any
      environment{
          TELEGRAM_BOT_TOKEN = credentials('telegram_bot_token')
          TELEGRAM_CHANNEL = credentials('telegram_channel_id')
          AWS_CODE_ARTIFACT_DOMAIN = credentials('aws-code-artifact-domain')
          AWS_CODE_ARTIFACT_DOMAIN_OWNER = credentials('aws-code-artifact-domain-owner')
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
                    changeset "${jenkinsfile}"
                }
            }
            steps {
                updateGitlabCommitStatus name: 'Login', state: 'pending'
                awsLogin(AWS_CODE_ARTIFACT_DOMAIN, AWS_CODE_ARTIFACT_DOMAIN_OWNER, AWS_DEFAULT_REGION)
                
            }
            post { 
                unsuccessful { 
                    updateGitlabCommitStatus name: 'Login', state: 'unsuccessful'
                }
                success { 
                    updateGitlabCommitStatus name: 'Login', state: 'success'
                }
            }
        }    
        stage('Restore') {
            when{
                anyOf{
                    changeset "${folder}/**/*"
                    changeset "${jenkinsfile}"
                }
            }
            steps {
                updateGitlabCommitStatus name: 'Login', state: 'pending'
                echo 'Restore Project'
                sh 'dotnet clean'
                sh "dotnet restore ${PATH_PRJ} --no-cache"
                updateGitlabCommitStatus name: 'Login', state: 'success'
            }
            post { 
                unsuccessful { 
                    updateGitlabCommitStatus name: 'Restore', state: 'unsuccessful'
                }
                success { 
                    updateGitlabCommitStatus name: 'Restore', state: 'success'
                }
            }
        }
        stage('Build'){
            when{
                anyOf{
                    changeset "${folder}/**/*"
                    changeset "${jenkinsfile}"
                }
            }
            steps {
                echo 'Build..'
                sh "dotnet build -c Release ${PATH_PRJ}"
            }
            post { 
                unsuccessful { 
                    updateGitlabCommitStatus name: 'Build', state: 'unsuccessful'
                }
                success { 
                    updateGitlabCommitStatus name: 'Build', state: 'success'
                }
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
            post { 
                unsuccessful { 
                    updateGitlabCommitStatus name: 'Publish', state: 'unsuccessful'
                }
                success { 
                    updateGitlabCommitStatus name: 'Publish', state: 'success'
                }
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