def call(String domain, String owner, String region){
    echo 'Login...'

    def repositories = ['BankCore', 'HandlersCore', 'MessageCore', 'NetCore', 'RestClientsCore', 'SettingsCore', 'Templates']

    repositories.each {repo ->
        echo "Login in ${repo} repository"
        sh "aws codeartifact login --tool dotnet --repository ${repo} --domain ${domain} --domain-owner ${owner} --region ${region}" 
    }
}
