properties([
    parameters([
        string(defaultValue: '', description: 'Please enter VM IP', name: 'nodeIP', trim: true),
        string(defaultValue: '', description: 'Please enter branch name', name: 'repo_branch', trim: true)
        ])
    ])

if (nodeIP?.trim()) {
    node {
        withCredentials([sshUserPrivateKey(credentialsId: 'jenkins-master-ssh-key', keyFileVariable: 'SSHKEY', passphraseVariable: '', usernameVariable: 'SSHUSERNAME')]) {
            stage('Pull repo') {
                git branch: '${repo_branch}', changelog: false, poll: false,  url: 'https://github.com/ikambarov/melodi.git'
            }
            stage("Install Apache"){
                sh 'ssh -o StrictHostKeyChecking=no -i $SSHKEY $SSHUSERNAME@${nodeIP} yum install httpd -y'
            }
            stage("Copy files") {
                sh 'scp -r -o StrictHostKeyChecking=no -i $SSHKEY  * $SSHUSERNAME@${nodeIP}:/var/www/html'
            }
            stage("Change ownership") {
                sh 'ssh -o StrictHostKeyChecking=no -i $SSHKEY   $SSHUSERNAME@${nodeIP} chown -R apache:apache /var/www/html'
            }
            stage("Restart") {
                sh 'ssh -o StrictHostKeyChecking=no -i $SSHKEY $SSHUSERNAME@${nodeIP} "systemctl start httpd && systemctl enable httpd"'
            }
            stage("CleanWorkspace") {
                cleanWs()
            }
            stage("Send a Message to Slack") {
                slackSend channel: 'apr_devops_2020', message: 'mission accomplished'
            }
        }
    }
}
else {
    error 'Please enter valid IP address'
}