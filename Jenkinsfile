pipeline {
    agent any

    environment {
        DOCKERHUB_NAMESPACE = 'rishuraj1'
        AKS_NAMESPACE = 'bank'
        AKS_CONTEXT = 'therishuraj'
    }

    options {
        timestamps()
    }

    triggers {
        pollSCM('H/2 * * * *')
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Verify build tools') {
            steps {
                sh '''
set -eu
for tool in java mvn docker kubectl python3; do
    if ! command -v "$tool" >/dev/null 2>&1; then
        echo "ERROR: Required tool '$tool' is not available on the Jenkins agent." >&2
        exit 1
    fi
done
java -version
mvn -version
docker --version
kubectl version --client
docker info
'''
            }
        }

        stage('Run all Maven test suites') {
            steps {
                script {
                    def javaProjects = [
                        'DiscoveryService/DiscoveryService/pom.xml',
                        'AccountService/AccountService/pom.xml',
                        'CustomerService/CustomerService/pom.xml',
                        'identity-service (2) (1)/identity-service/identity-service/pom.xml',
                        'swiggy-gateway (2)/swiggy-gateway/swiggy-gateway/pom.xml'
                    ]
                    javaProjects.each { project ->
                        echo "Running full Maven test suite: ${project}"
                        sh "mvn -B -f '${project}' test"
                    }
                }
            }
        }

        stage('Package Java services') {
            steps {
                script {
                    def javaProjects = [
                        'DiscoveryService/DiscoveryService/pom.xml',
                        'AccountService/AccountService/pom.xml',
                        'CustomerService/CustomerService/pom.xml',
                        'identity-service (2) (1)/identity-service/identity-service/pom.xml',
                        'swiggy-gateway (2)/swiggy-gateway/swiggy-gateway/pom.xml'
                    ]
                    javaProjects.each { project ->
                        echo "Packaging Java service: ${project}"
                        sh "mvn -B -f '${project}' -DskipTests package"
                    }
                }
            }
        }

        stage('Build six images') {
            steps {
                sh '''
set -eu
tag="build-${BUILD_NUMBER}"
echo "Building all six Docker images with tag $tag"
docker build -t "$DOCKERHUB_NAMESPACE/discoveryservice:$tag" ./DiscoveryService/DiscoveryService
docker build -t "$DOCKERHUB_NAMESPACE/accountservice:$tag" ./AccountService/AccountService
docker build -t "$DOCKERHUB_NAMESPACE/customer-service:$tag" ./CustomerService/CustomerService
docker build -t "$DOCKERHUB_NAMESPACE/identity-service:$tag" "./identity-service (2) (1)/identity-service/identity-service"
docker build -t "$DOCKERHUB_NAMESPACE/swiggy-gateway:$tag" "./swiggy-gateway (2)/swiggy-gateway/swiggy-gateway"
docker build \
    --build-arg VITE_AUTH_API_URL=/ \
    --build-arg VITE_BANK_API_URL=/ \
    -t "$DOCKERHUB_NAMESPACE/bank-frontend:$tag" \
    ./BankService/BankService
'''
            }
        }

        stage('Publish versioned images') {
            when {
                expression {
                    env.BRANCH_NAME == 'main' || env.GIT_BRANCH in ['main', 'origin/main', '*/main', 'refs/remotes/origin/main']
                }
            }
            steps {
                script {
                    def images = [
                        'discoveryservice',
                        'accountservice',
                        'customer-service',
                        'identity-service',
                        'swiggy-gateway',
                        'bank-frontend'
                    ]
                    withCredentials([usernamePassword(
                        credentialsId: 'dockerhub-credentials',
                        usernameVariable: 'DOCKERHUB_USERNAME',
                        passwordVariable: 'DOCKERHUB_TOKEN'
                    )]) {
                        try {
                            echo "Logging in to Docker Hub and publishing build-${env.BUILD_NUMBER} images"
                            sh '''
set +x
printf '%s' "$DOCKERHUB_TOKEN" | docker login --username "$DOCKERHUB_USERNAME" --password-stdin
'''
                            images.each { image ->
                                sh "docker push ${env.DOCKERHUB_NAMESPACE}/${image}:build-${env.BUILD_NUMBER}"
                            }
                        } finally {
                            sh 'docker logout'
                        }
                    }
                }
            }
        }

        stage('Deploy to AKS') {
            when {
                expression {
                    env.BRANCH_NAME == 'main' || env.GIT_BRANCH in ['main', 'origin/main', '*/main', 'refs/remotes/origin/main']
                }
            }
            steps {
                sh '''
set -eu
set +x
contexts=$(kubectl config get-contexts -o name)
if ! printf '%s\n' "$contexts" | grep -Fxq "$AKS_CONTEXT"; then
    echo "ERROR: Kubernetes context '$AKS_CONTEXT' is missing from the Jenkins agent kubeconfig. Configure Jenkins access to AKS before deploying." >&2
    exit 1
fi
kubectl config use-context "$AKS_CONTEXT"
if ! kubectl get --raw=/version >/dev/null; then
    echo "ERROR: Jenkins cannot reach AKS using context '$AKS_CONTEXT'. Check the Jenkins kubeconfig credentials and cluster network access." >&2
    exit 1
fi

kubectl apply -f k8s/01-namespace.yaml

if [ ! -f .env ]; then
    echo "ERROR: Required root .env file is missing." >&2
    exit 1
fi
if ! grep -Eq '^[[:space:]]*MYSQL_ROOT_PASSWORD[[:space:]]*=' .env; then
    echo "ERROR: MYSQL_ROOT_PASSWORD is missing from root .env." >&2
    exit 1
fi

if kubectl get secret bank-database -n "$AKS_NAMESPACE" -o name >/dev/null 2>&1; then
    echo "Using the existing bank-database Secret to preserve the MySQL PVC credentials."
else
    echo "Creating the bank-database Secret from root .env."
    secret_manifest=$(mktemp)
    trap 'rm -f -- "$secret_manifest"' EXIT
    python3 - "$secret_manifest" <<'PY'
import base64
import json
from pathlib import Path
import sys

password = None
for line in Path(".env").read_text(encoding="utf-8").splitlines():
    stripped = line.strip()
    if stripped.partition("=")[0].strip() == "MYSQL_ROOT_PASSWORD":
        password = stripped.split("=", 1)[1].strip()
        if len(password) >= 2 and password[0] == password[-1] and password[0] in ("'", '"'):
            password = password[1:-1]
        break

if not password:
    raise SystemExit("ERROR: MYSQL_ROOT_PASSWORD is missing or empty in root .env.")

secret = {
    "apiVersion": "v1",
    "kind": "Secret",
    "metadata": {"name": "bank-database", "namespace": "bank"},
    "type": "Opaque",
    "data": {"mysql-root-password": base64.b64encode(password.encode()).decode()},
}
Path(sys.argv[1]).write_text(json.dumps(secret), encoding="utf-8")
PY
    kubectl apply -f "$secret_manifest"
    rm -f -- "$secret_manifest"
    trap - EXIT
fi

echo "Applying MySQL and waiting for its StatefulSet rollout."
kubectl apply -f k8s/03-mysql.yaml
kubectl rollout status statefulset/mysql -n "$AKS_NAMESPACE" --timeout=300s

output_directory="k8s-deploy-${BUILD_NUMBER}"
echo "Rendering versioned Kubernetes manifests into $output_directory."
bash scripts/render-k8s.sh "$DOCKERHUB_NAMESPACE" "build-${BUILD_NUMBER}" "$output_directory"

echo "Applying discovery service and waiting for readiness."
kubectl apply -f "$output_directory/04-discovery.yaml"
kubectl rollout status deployment/discoveryservice -n "$AKS_NAMESPACE" --timeout=300s

echo "Applying application services and waiting for rollouts."
kubectl apply -f "$output_directory/05-services.yaml"
kubectl rollout status deployment/accountservice -n "$AKS_NAMESPACE" --timeout=300s
kubectl rollout status deployment/customerservice -n "$AKS_NAMESPACE" --timeout=300s
kubectl rollout status deployment/identityservice -n "$AKS_NAMESPACE" --timeout=300s
kubectl rollout status deployment/swiggygateway -n "$AKS_NAMESPACE" --timeout=300s

echo "Applying frontend and waiting for deployment and pod readiness."
kubectl apply -f "$output_directory/06-frontend.yaml"
kubectl rollout status deployment/bankfrontend -n "$AKS_NAMESPACE" --timeout=300s
kubectl wait --for=condition=Ready pod -l app=bankfrontend -n "$AKS_NAMESPACE" --timeout=300s
kubectl get deployments,statefulsets,pods -n "$AKS_NAMESPACE"
'''
            }
        }
    }

    post {
        always {
            junit testResults: '**/target/surefire-reports/*.xml', allowEmptyResults: true
            sh '''
set -eu
case "$BUILD_NUMBER" in
    ''|*[!0-9]*)
        echo "ERROR: Refusing to clean an invalid generated-directory name." >&2
        exit 1
        ;;
esac
output_directory="k8s-deploy-${BUILD_NUMBER}"
if [ -d "$output_directory" ]; then
    rm -rf -- "$output_directory"
fi
'''
        }
        success {
            echo 'Build and AKS deployment completed.'
        }
        failure {
            echo 'Pipeline failed. Check the stage logs before retrying.'
        }
    }
}
