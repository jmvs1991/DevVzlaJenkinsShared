def call(String project) {
    pipeline {
        agent any
        environment {
            TELEGRAM_BOT_TOKEN = credentials('telegram_bot_token')
            TELEGRAM_CHANNEL = credentials('telegram_channel_id')
            AWS_CODE_ARTIFACT_DOMAIN = credentials('aws-code-artifact-domain')
            AWS_CODE_ARTIFACT_DOMAIN_OWNER = credentials('aws-code-artifact-domain-owner')
            AWS_DEFAULT_REGION = credentials('aws-default-region')
        }
        stages {
            stage('Determine Environment') {
                steps {
                    script {
                        def branchName = env.BRANCH_NAME
                        switch(branchName) {
                            case 'develop':
                                env.ENVIRONMENT = "Test"
                                break
                            case 'stage':
                                env.ENVIRONMENT = "Stage"
                                break
                            case 'main':
                                env.ENVIRONMENT = "Main"
                                break
                            default:
                                error("Unknown branch: ${branchName}")
                        }
                        echo "Environment set to: ${env.ENVIRONMENT}"
                    }
                }
            }
            stage('Input Data') {
                steps {
                    script {
                        try {
                            timeout(time: 2, unit: 'MINUTES') {
                                def userInput = input(
                                    id: 'userInput', 
                                    message: 'Please provide the following information:', 
                                    parameters: [
                                        password(name: 'DATA_SOURCE', description: 'Database IP'),
                                        password(name: 'USER', description: 'User'),
                                        password(name: 'PASSWORD', description: 'Password'),
                                        booleanParam(name: 'PROCEED', description: 'Do you want to proceed?', defaultValue: false),
                                        booleanParam(name: 'INITIALIZE', description: 'Do you want to run the database initialization scripts?', defaultValue: false)
                                    ]
                                )
                                env.DATA_SOURCE = userInput.DATA_SOURCE
                                env.USER = userInput.USER
                                env.PASSWORD = userInput.PASSWORD
                                env.PROCEED = userInput.PROCEED.toString()
                                env.INITIALIZE = userInput.INITIALIZE.toString()
                            }
                        } catch (org.jenkinsci.plugins.workflow.steps.FlowInterruptedException e) {
                            echo "The information was not provided within the time limit. Aborting..."
                            currentBuild.result = 'ABORTED'
                            return
                        }
                    }
                }
            }
            stage('Login') {
                when {
                    expression {
                        return env.PROCEED == "true"
                    }
                }
                steps {
                    awsLogin(AWS_CODE_ARTIFACT_DOMAIN, AWS_CODE_ARTIFACT_DOMAIN_OWNER, AWS_DEFAULT_REGION)
                }
            }
            stage('Initialize DB') {
                when {
                    expression {
                        return env.PROCEED == "true" && env.INITIALIZE == "true"
                    }
                }
                steps {
                    script {
                        echo "Running database initialization scripts..."

                        dir("${project}.SchemaInitialization") {
                            wrap([$class: 'MaskPasswordsBuildWrapper', varPasswordPairs: [[password: "${PASSWORD}", var: 'PSWD'], [password: "${DATA_SOURCE}", var: 'PSWD'], [password: "${USER}", var: 'PSWD']]]) {
                                sh 'dotnet clean'
                                sh ('dotnet run Enviroment:$ENVIRONMENT DataSource:$DATA_SOURCE User:$USER Password=$PASSWORD')
                            }
                        }
                    }
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
