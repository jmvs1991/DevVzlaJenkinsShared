def call(String project, String artifactName) {
    pipeline {
        agent any
        environment {
            TELEGRAM_BOT_TOKEN = credentials('telegram_bot_token')
            TELEGRAM_CHANNEL = credentials('telegram_channel_id')
            AWS_CODE_ARTIFACT_DOMAIN = credentials('aws-code-artifact-domain')
            AWS_CODE_ARTIFACT_DOMAIN_OWNER = credentials('aws-code-artifact-domain-owner')
            AWS_DEFAULT_REGION = credentials('aws-default-region')
            APP_SETTINGS_TEST = credentials('appsettings.Test.json')
            APP_SETTINGS_STAGE = credentials('appsettings.Stage.json')
            APP_SETTINGS_MAIN = credentials('appsettings.Main.json')
            ARTIFACT_INITIALIZATION = "${artifactName}_initialization_${env.BRANCH_NAME}_${BUILD_NUMBER}.zip"
            ARTIFACT_UPDATES = "${artifactName}_updates_${env.BRANCH_NAME}_${BUILD_NUMBER}.zip"
            INITIALIZATION_FOLDER = "${project}.SchemaInitialization"
            UPDATES_FOLDER = "${project}.SchemaUpdates"
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
                                        booleanParam(name: 'PROCEED', description: 'Do you want to proceed?', defaultValue: false),
                                        booleanParam(name: 'INITIALIZE', description: 'Do you want to run the database initialization scripts?', defaultValue: false)
                                    ]
                                )
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
                steps {
                    awsLogin(AWS_CODE_ARTIFACT_DOMAIN, AWS_CODE_ARTIFACT_DOMAIN_OWNER, AWS_DEFAULT_REGION)
                }
            }
            stage('Initialize DB') {
                when {
                    anyOf {
                        branch 'develop'
                        branch 'stage'
                        branch 'main'
                    }
                    expression {
                        return env.INITIALIZE == "true"
                    }
                }
                steps {
                    script {
                        echo "Running database initialization scripts..."

                        dir(INITIALIZATION_FOLDER) {
                            sh 'dotnet clean'
                            sh 'cp $APP_SETTINGS_TEST .'
                            sh 'cp $APP_SETTINGS_STAGE .'
                            sh 'cp $APP_SETTINGS_MAIN .'
                            sh ("dotnet run Enviroment:${ENVIRONMENT}")
                        }
                    }
                }
            }
            stage('Initialize Artifact') {
                when {
                    anyOf {
                        branch 'develop'
                        branch 'stage'
                        branch 'main'
                    }
                    expression {
                        return env.INITIALIZE == "true"
                    }
                }
                steps {
                    script {
                        echo "Generating artifact"

                        dir(INITIALIZATION_FOLDER) {
                            def migrationFolder = "${ENVIRONMENT}_Initialization_migration";
                            def result = sh(script: "ls -d ${migrationFolder}", returnStatus: true)

                            if (result == 0) {
                                zip zipFile: "${ARTIFACT_INITIALIZATION}", archive: false, dir: "${migrationFolder}"
                                archiveArtifacts artifacts: "${ARTIFACT_INITIALIZATION}", fingerprint: true
                            } else if (result == 2) {
                                echo "El directorio '${migrationFolder}' está vacío."
                            } else {
                                echo "Error al verificar el directorio '${migrationFolder}'."
                            }
                        }
                    }
                }
            }
            stage('Update DB') {
                when {
                    anyOf {
                        branch 'develop'
                        branch 'stage'
                        branch 'main'
                    }
                }
                steps {
                    script {
                        echo "Running database updates scripts..."

                        dir(UPDATES_FOLDER) {
                            sh 'dotnet clean'
                            sh 'cp $APP_SETTINGS_TEST .'
                            sh 'cp $APP_SETTINGS_STAGE .'
                            sh 'cp $APP_SETTINGS_MAIN .'
                            sh ("dotnet run Enviroment:${ENVIRONMENT}")
                        }
                    }
                }
            }
            stage('Updates Artifact') {
                when {
                    anyOf {
                        branch 'develop'
                        branch 'stage'
                        branch 'main'
                    }
                }
                steps {
                    script {
                        echo "Generating artifact"

                        dir(UPDATES_FOLDER) {
                            def migrationFolder = "${ENVIRONMENT}_Updates_migration";
                            def result = sh(script: "ls -d ${migrationFolder}", returnStatus: true)

                            if (result == 0) {
                                zip zipFile: "${ARTIFACT_INITIALIZATION}", archive: false, dir: "${migrationFolder}"
                                archiveArtifacts artifacts: "${ARTIFACT_INITIALIZATION}", fingerprint: true
                            } else if (result == 2) {
                                echo "El directorio '${migrationFolder}' está vacío."
                            } else {
                                echo "Error al verificar el directorio '${migrationFolder}'."
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
