def call(String telegramBotToken, String telegramChannelId) {

    echo 'Sending notification...'

    def url = "https://api.telegram.org/${telegramBotToken}/sendMessage"
        
    def body = """
                    {
                      "chat_id": $telegramChannelId,
                      "text": "The job *${JOB_NAME}* Nr. *${BUILD_NUMBER}* is finished. \n\n *Started by: ${env.CHANGE_AUTHOR}* \n\n *Branch: ${env.BRANCH_NAME}* \n\n *Result: ${currentBuild.result}* \n\n [Job Url](${BUILD_URL})",
                      "parse_mode": "Markdown"
                    }
                """

    def response = sh script: "curl -X POST -H 'Content-Type: application/json' -d '${body}' ${url}", returnStdout: true

    if (response.contains('"ok":true')) {
        echo "Notification sent successfully"
    } else {
        error "Failed to send notification: ${response}"
    }
}
