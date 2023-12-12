def call(String project, String folder, String jenkinsfile, boolean forceSteps = false, boolean forcePublish = false){
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
          stage('Login'){
             when {
                    anyOf {
                        changeset "${folder}/**/*"
                        changeset "${jenkinsfile}"
                        expression {
                            forceSteps == true
                        }
                    }
                }
              steps {
                  echo 'Login'
                  sh 'npm run aws:login'
              }
          }
          stage('Install'){
                when {
                    anyOf {
                        changeset "${folder}/**/*"
                        changeset "${jenkinsfile}"
                        expression {
                            forceSteps == true
                        }
                    }
                }
              steps {
                  echo 'Installing'
                  sh 'npm install --exact'
              }
          }
          stage('Build'){
                when {
                    anyOf {
                        changeset "${folder}/**/*"
                        changeset "${jenkinsfile}"
                        expression {
                            forceSteps == true
                        }
                    }
                }
              steps {
                  echo 'Build'
                  sh "npm run build:${project}"
              }
          }
          stage('Publish'){
                when {
                    anyOf {
                        branch 'main'
                    }
                    anyOf {
                        changeset "${folder}/**/*"
                        expression {
                            forceSteps == true || forcePublish == true
                        }
                    }
                }
              steps {
                  echo 'Publishing'
                  sh "npm run publish:${project}"
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
