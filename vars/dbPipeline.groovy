def call() {
    pipeline {
        agent any
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
        }
    }
}
