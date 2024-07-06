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
                        env.ENVIRONMENT = determineEnvironment(branchName)
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
                                        string(name: 'DATA_SOURCE', description: 'Database IP', defaultValue: ''),
                                        string(name: 'USER', description: 'User', defaultValue: ''),
                                        password(name: 'PASSWORD', description: 'Password'),
                                        booleanParam(name: 'PROCEED', description: 'Do you want to proceed?', defaultValue: true),
                                        booleanParam(name: 'INITIALIZE', description: 'Do you want to run the database initialization scripts?', defaultValue: false)
                                    ]
                                )
                                // Asigna la entrada del usuario a variables de entorno de manera segura
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
                    withCredentials([string(credentialsId: 'aws-credentials', variable: 'AWS_ACCESS_KEY_ID'),
                                     string(credentialsId: 'aws-secret-key', variable: 'AWS_SECRET_ACCESS_KEY')]) {
                        awsLogin(AWS_CODE_ARTIFACT_DOMAIN, AWS_CODE_ARTIFACT_DOMAIN_OWNER, AWS_DEFAULT_REGION)
                    }
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
                            sh 'dotnet clean'
                            withCredentials([password(credentialsId: 'db-password', variable: 'DB_PASSWORD')]) {
                                sh """
                                    dotnet run \\
                                    Environment=$env.ENVIRONMENT \\
                                    DataSource=$env.DATA_SOURCE \\
                                    User=$env.USER \\
                                    Password=$DB_PASSWORD
                                """
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

def determineEnvironment(branchName) {
    switch(branchName) {
        case 'develop':
            return "Test"
        case 'stage':
            return "Stage"
        case 'main':
            return "Main"
        default:
            error("Unknown branch: ${branchName}")
    }
}
