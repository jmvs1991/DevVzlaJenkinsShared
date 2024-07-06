def call() {
    pipeline {
        agent any
        environment {
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
                                string(name: 'PROJECT_NAME', description: 'Nombre del Proyecto', defaultValue: 'MiProyecto'),
                                booleanParam(name: 'PROCEED', description: '¿Deseas continuar con el proceso?', defaultValue: true)
                            ]
                        )
                        env.PROJECT_NAME = userInput.PROJECT_NAME
                        env.PROCEED = userInput.PROCEED
                    }
                }
            }
            stage('Login') {
                when {
                    expression {
                        return env.PROCEED.toBoolean()
                    }
                }
                steps {
                    script {
                        echo "Iniciando sesión en AWS para el proyecto ${env.PROJECT_NAME}"
                        awsLogin(AWS_CODE_ARTIFACT_DOMAIN, AWS_CODE_ARTIFACT_DOMAIN_OWNER, AWS_DEFAULT_REGION)
                    }
                }
            }
        }
        post {
            always {
                script {
                    cleanWs()
                }
            }
        }
    }
}
