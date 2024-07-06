def call() {
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
            stage('Input Data') {
                steps {
                    script {
                        def userInput = input(
                            id: 'userInput', message: 'Por favor, proporciona la siguiente información:', parameters: [
                                string(name: 'DATA_SOURCE', description: 'Ip de la base de datos', defaultValue: ''),
                                string(name: 'USER', description: 'Usuario', defaultValue: ''),
                                string(name: 'PASSWORD', description: 'Password', defaultValue: ''),
                                booleanParam(name: 'PROCEED', description: '¿Deseas continuar con el proceso?', defaultValue: true)
                            ]
                        )
                        env.DATA_SOURCE = userInput.DATA_SOURCE
                        env.USER = userInput.USER
                        env.PASSWORD = userInput.PASSWORD
                        env.PROCEED = userInput.PROCEED
                    }
                }
            }
        }
    }
}
