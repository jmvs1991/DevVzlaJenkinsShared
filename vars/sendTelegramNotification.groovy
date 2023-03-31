def call(String telegramBotToken, String telegramChannelId) {

    echo 'Sending notification...'

    def url = "https://api.telegram.org/${telegramBotToken}/sendMessage"
        
    def body = """
                    {
                      "chat_id": $telegramChannelId,
                      "text": "The job *${JOB_NAME}* Nr. *${BUILD_NUMBER}* is finished. \n\n *branch: ${env.BRANCH_NAME}* \n\n *result: ${currentBuild.result}* \n\n [Job Url](${BUILD_URL})",
                      "parse_mode": "Markdown"
                    }
                """

    def response = sh script: "curl -X POST -H 'Content-Type: application/json' -d '${body}' ${url}", returnStdout: true

    if (response.contains('"ok":true')) {
        echo "Notification sent successfully"
    } else {
        error "Failed to send notification: ${response}"
    }

    // withCredentials([string(credentialsId: 'telegram_bot_token', variable: 'telegramBotToken'),
    //                  string(credentialsId: 'telegram_channel_id', variable: 'telegramChannel')]) {
        
    //     // def body = """
    //     //                 {
    //     //                   "chat_id": "${telegramChannel}",
    //     //                   "text": "The job *${JOB_NAME}* Nr. *${BUILD_NUMBER}* is finished. \n\n *branch: ${env.BRANCH_NAME}* \n\n *result: ${currentBuild.result}* \n\n [Job Url](${BUILD_URL})",
    //     //                   "parse_mode": "Markdown"
    //     //                 }
    //     //             """

    //     echo 'Sending notification...'

    //     // httpRequest url: "https://api.telegram.org/$telegramBotToken/sendMessage", 
    //     //             httpMode: 'POST',
    //     //             contentType: 'APPLICATION_JSON',
    //     //             requestBody: body

    //     def url = "https://api.telegram.org/${telegramBotToken}/sendMessage"
        
    //     // def body = {
    //     //   chat_id: "${telegramChannel}",
    //     //   text: "The job *${JOB_NAME}* Nr. *${BUILD_NUMBER}* is finished. \n\n *branch: ${env.BRANCH_NAME}* \n\n *result: ${currentBuild.result}* \n\n [Job Url](${BUILD_URL})",
    //     //   parse_mode: "Markdown"
    //     // }

    //     def body = """
    //                     {
    //                       "chat_id": $telegramChannel,
    //                       "text": "The job *${JOB_NAME}* Nr. *${BUILD_NUMBER}* is finished. \n\n *branch: ${env.BRANCH_NAME}* \n\n *result: ${currentBuild.result}* \n\n [Job Url](${BUILD_URL})",
    //                       "parse_mode": "Markdown"
    //                     }
    //                 """

    //     def response = sh script: "curl -X POST -H 'Content-Type: application/json' -d '${body}' ${url}", returnStdout: true

    //     if (response.contains('"ok":true')) {
    //         echo "Notification sent successfully"
    //     } else {
    //         error "Failed to send notification: ${response}"
    //     }
    // }
}