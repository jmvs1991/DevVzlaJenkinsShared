def call() {
    def telegramApi = credentials('telegram_api')
    def telegramBotToken = credentials('telegram_bot_token')
    def telegramChannel = credentials('telegram_channel_id')

    def body = """
                    {
                      "chat_id": "${telegramChannel}",
                      "text": "The job *${JOB_NAME}* Nr. *${BUILD_NUMBER}* is finished. \n\n *branch: ${env.BRANCH_NAME}* \n\n *result: ${currentBuild.result}* \n\n [Job Url](${BUILD_URL})",
                      "parse_mode": "Markdown"
                    }
                """
    
    httpRequest url: "https://api.telegram.org/${telegramBotToken}/sendMessage", 
                httpMode: 'POST',
                contentType: 'APPLICATION_JSON',
                requestBody: body
}