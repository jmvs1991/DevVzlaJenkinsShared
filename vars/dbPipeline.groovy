def call() {
    pipeline {
        agent any
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
