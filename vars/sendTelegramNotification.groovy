def call() {
    def telegramBotToken
    def telegramChannel   

    withCredentials([string(credentialsId: 'telegram_bot_token', variable: 'telegramBotToken'),
                     string(credentialsId: 'telegram_channel_id', variable: 'telegramChannel')]) {
        
        def body = """
                        {
                          "chat_id": "${telegramChannel}",
                          "text": "The job *${JOB_NAME}* Nr. *${BUILD_NUMBER}* is finished. \n\n *branch: ${env.BRANCH_NAME}* \n\n *result: ${currentBuild.result}* \n\n [Job Url](${BUILD_URL})",
                          "parse_mode": "Markdown"
                        }
                    """
                    
        echo 'Sending notification...'

        httpRequest url: "https://api.telegram.org/${telegramBotToken}/sendMessage", 
                    httpMode: 'POST',
                    contentType: 'APPLICATION_JSON',
                    requestBody: body
    }
}