properties([
    parameters([
        booleanParam(defaultValue: false, description: 'Do you want to run terrform destroy', name: 'terraform_destroy'),
        string(defaultValue: '', description: 'Provide SOURCE_PROJECT_NAME', name: 'SOURCE_PROJECT_NAME', trim: true)
    ])
])
def aws_region_var = ''
def environment = ''
if(params.SOURCE_PROJECT_NAME ==~ "dev.*"){
    aws_region_var = "us-east-1"
    environment = 'dev'
}
else if(params.SOURCE_PROJECT_NAME ==~ "qa.*"){
    aws_region_var = "us-east-2"
    environment = 'qa'
}
else if(params.SOURCE_PROJECT_NAME ==~ "master"){
    aws_region_var = "us-west-2"
    environment = 'prod'
}
else {
    error("SOURCE_PROJECT_NAME Name Doesnt Match RegEx")
}
def tf_vars = """
    s3_bucket = \"jenkins-terraform-evolvecybertraining\"
    s3_folder_project = \"terraform_ec2\"
    s3_folder_region = \"us-east-1\"
    s3_folder_type = \"class\"
    s3_tfstate_file = \"infrastructure.tfstate\"
    environment = \"${environment}\"
    region      = \"${aws_region_var}\"
    public_key  = \"ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABgQDPexpqnif8GWstuaK5mT00hIKwvK/s4W1jPEA8HWj0VDk75niLEQZVGQK4ktfTOCaSwhcqgaHBeUMP/f8fnKG4Ai4/CH9m4u+QHT2Hb8L/7jJOj733UGlCMMRCdnIwKV+vLNmUvoiPBHWtzUFgDvEjan31A3LzZ4FWB67lxwJflaUSMjda04OHpargXZaSBS5x6+9vE15Ql1HlfTpabYmY5LH8L9DZ2+a8z8O2R8LhwDffpct5VG3dxItiLZObDDPBAYyX+AkvOIzVhrtkjRTqXxkR3J2oE6P0tybGs7GbQDSHsfneQ9T6mowM8r9ydDWG/PdreOfZG47XTm7jsaKLBmpzZhf7xV6HtnyvztlxqGI8ubQ6sZvg8EZoFydkvDrTHnZhldnumzzP8dAFWwBBwmo+F1i5dX3cglk27JMWzuDSHDE0XDVtm/t83x7SjP+sD3y4uXKzkPZW4IdX9FRK44xgo5rdBGhIo/v/dtgZT39hmbUUlgxgzXktD2N45+0= nargiza@Nargizas-MacBook-Pro.local\"
    ami_id      = \"*\"
"""
node{
    stage("Pull Repo"){
        cleanWs()
        git url: 'https://github.com/ikambarov/terraform-ec2.git'
    }
    withCredentials([usernamePassword(credentialsId: 'jenkins-aws-access-key', passwordVariable: 'AWS_SECRET_ACCESS_KEY', usernameVariable: 'AWS_ACCESS_KEY_ID')]) {
        withEnv(["AWS_REGION=${aws_region_var}"]) {
            stage("Terrraform Init"){
                writeFile file: "${environment}.tfvars", text: "${tf_vars}"
                sh """
                    bash setenv.sh ${environment}.tfvars
                    terraform-0.13 init
                """
            }        
            if (terraform_destroy.toBoolean()) {
                stage("Terraform Destroy"){
                    sh """
                        terraform-0.13 destroy -var-file ${environment}.tfvars -auto-approve
                    """
                }
            }
            else {
                stage("Terraform Plan"){
                    sh """
                        terraform-0.13 plan -var-file ${environment}.tfvars
                    """
                }
            }
        }        
    }    
}

