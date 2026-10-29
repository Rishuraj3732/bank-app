pipeline {
    agent any

    environment {
        KUBECONFIG = 'C:\\Users\\HP\\.kube\\config'
        MAVEN_HOME = 'C:\\Program Files\\apache-maven-3.9.16'
        DOCKERHUB_NAMESPACE = 'rishuraj1'
    }

    options {
        timestamps()
    }

    stages {
        stage('Check tools and cluster') {
            steps {
                bat 'java -version'
                bat '"%MAVEN_HOME%\\bin\\mvn.cmd" -version'
                bat 'docker info'
                bat '''
@echo off
kubectl config get-contexts
kubectl config use-context docker-desktop
if errorlevel 1 (
    echo ERROR: Could not select docker-desktop from %KUBECONFIG%.
    exit /b 1
)
kubectl get --raw=/version
if errorlevel 1 exit /b 1
kubectl get nodes
if errorlevel 1 exit /b 1
'''
                bat 'if not exist .env (echo Required root .env file is missing.& exit /b 1)'
                bat 'findstr /R /C:"^[ ]*MYSQL_ROOT_PASSWORD[ ]*=" .env >nul || (echo MYSQL_ROOT_PASSWORD is missing from root .env.& exit /b 1)'
            }
        }

        stage('Create namespace and database Secret') {
            steps {
                bat 'kubectl apply -f k8s\\01-namespace.yaml'
                bat '''
@powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "$secret = kubectl get secret bank-database -n bank -o name 2>$null; if ($LASTEXITCODE -ne 0) { $entry = Get-Content .env | Where-Object { $_.Trim().StartsWith('MYSQL_ROOT_PASSWORD=') } | Select-Object -First 1; if (-not $entry) { Write-Error 'MYSQL_ROOT_PASSWORD is missing from root .env'; exit 1 }; $entry = $entry.Trim(); $password = $entry.Substring($entry.IndexOf('=') + 1).Trim().Trim([char]34).Trim([char]39); if (-not $password) { Write-Error 'MYSQL_ROOT_PASSWORD is empty'; exit 1 }; kubectl create secret generic bank-database -n bank ('--from-literal=mysql-root-password=' + $password) --dry-run=client -o yaml | kubectl apply -f -; if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE } } else { Write-Output 'Using the existing bank-database Secret to preserve the MySQL PVC credentials.' }"
'''
            }
        }

        stage('Test customer and savings directories') {
            steps {
                bat '"%MAVEN_HOME%\\bin\\mvn.cmd" -f "CustomerService\\CustomerService\\pom.xml" -Dtest=CustomerServiceImplTest,CustomerControllerTest test'
                bat '"%MAVEN_HOME%\\bin\\mvn.cmd" -f "AccountService\\AccountService\\pom.xml" -Dtest=SavingServiceImplTest,SavingsControllerTest test'
            }
        }

        stage('Package Java services') {
            steps {
                bat '"%MAVEN_HOME%\\bin\\mvn.cmd" -f "DiscoveryService\\DiscoveryService\\pom.xml" -DskipTests package'
                bat '"%MAVEN_HOME%\\bin\\mvn.cmd" -f "AccountService\\AccountService\\pom.xml" -DskipTests package'
                bat '"%MAVEN_HOME%\\bin\\mvn.cmd" -f "CustomerService\\CustomerService\\pom.xml" -DskipTests package'
                bat '"%MAVEN_HOME%\\bin\\mvn.cmd" -f "identity-service (2) (1)\\identity-service\\identity-service\\pom.xml" -DskipTests package'
                bat '"%MAVEN_HOME%\\bin\\mvn.cmd" -f "swiggy-gateway (2)\\swiggy-gateway\\swiggy-gateway\\pom.xml" -DskipTests package'
            }
        }

        stage('Build local images') {
            steps {
                bat 'docker build -t discoveryservice:latest .\\DiscoveryService\\DiscoveryService'
                bat 'docker build -t accountservice:latest .\\AccountService\\AccountService'
                bat 'docker build -t customer-service:latest .\\CustomerService\\CustomerService'
                bat 'docker build -t identity-service:latest ".\\identity-service (2) (1)\\identity-service\\identity-service"'
                bat 'docker build -t swiggy-gateway:latest ".\\swiggy-gateway (2)\\swiggy-gateway\\swiggy-gateway"'
                bat 'docker build -t bank-frontend:latest --build-arg VITE_AUTH_API_URL=http://localhost:8765 --build-arg VITE_BANK_API_URL=http://localhost:8765 .\\BankService\\BankService'
            }
        }

        stage('Publish versioned images') {
            steps {
                script {
                    withCredentials([usernamePassword(
                        credentialsId: 'dockerhub-credentials',
                        usernameVariable: 'DOCKERHUB_USERNAME',
                        passwordVariable: 'DOCKERHUB_TOKEN'
                    )]) {
                        try {
                            bat '''
@echo off
@powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "$env:DOCKERHUB_TOKEN | docker login --username $env:DOCKERHUB_USERNAME --password-stdin"
if errorlevel 1 exit /b 1
'''
                            def images = [
                                [local: 'discoveryservice', remote: 'discoveryservice'],
                                [local: 'accountservice', remote: 'accountservice'],
                                [local: 'customer-service', remote: 'customer-service'],
                                [local: 'identity-service', remote: 'identity-service'],
                                [local: 'swiggy-gateway', remote: 'swiggy-gateway'],
                                [local: 'bank-frontend', remote: 'bank-frontend']
                            ]
                            images.each { image ->
                                bat "docker tag ${image.local}:latest %DOCKERHUB_NAMESPACE%/${image.remote}:build-%BUILD_NUMBER%"
                                bat "docker push %DOCKERHUB_NAMESPACE%/${image.remote}:build-%BUILD_NUMBER%"
                            }
                        } finally {
                            bat 'docker logout'
                        }
                    }
                }
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                bat 'kubectl apply -f k8s\\01-namespace.yaml'
                bat 'kubectl apply -f k8s\\03-mysql.yaml'
                bat 'powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts\\render-k8s.ps1 -ImageNamespace "%DOCKERHUB_NAMESPACE%" -ImageTag build-%BUILD_NUMBER% -OutputDirectory "k8s-deploy-%BUILD_NUMBER%"'
                bat 'kubectl apply -f "k8s-deploy-%BUILD_NUMBER%"'
                bat 'kubectl rollout status deployment/discoveryservice -n bank --timeout=300s'
                bat 'kubectl rollout status deployment/accountservice -n bank --timeout=300s'
                bat 'kubectl rollout status deployment/customerservice -n bank --timeout=300s'
                bat 'kubectl rollout status deployment/identityservice -n bank --timeout=300s'
                bat 'kubectl rollout status deployment/swiggygateway -n bank --timeout=300s'
                bat 'kubectl rollout status deployment/bankfrontend -n bank --timeout=300s'
                bat 'kubectl rollout status statefulset/mysql -n bank --timeout=300s'
            }
        }
    }

    post {
        always {
            bat '''
@powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "$path = Join-Path $env:WORKSPACE ('k8s-deploy-' + $env:BUILD_NUMBER); if (Test-Path -LiteralPath $path) { Remove-Item -LiteralPath $path -Recurse -Force }"
'''
        }
        success {
            echo 'Build and Kubernetes deployment completed.'
        }
        failure {
            echo 'Pipeline failed. Check the stage logs before retrying.'
        }
    }
}
