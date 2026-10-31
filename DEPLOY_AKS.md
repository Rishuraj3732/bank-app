# Jenkins deployment to AKS

The root `Jenkinsfile` builds and tests the existing services on a Linux Jenkins
agent, publishes six versioned Docker Hub images, and deploys them to the
`bank` namespace in AKS. It polls SCM every two minutes; it does not require a
public Jenkins webhook.

## One-time setup

- Configure a Linux Jenkins agent with Java, Maven, Docker/build support, Azure
  CLI, `kubectl`, and Python 3. Install the Jenkins Git, Pipeline, Credentials
  Binding, and JUnit plugins. The pipeline builds sequentially to limit memory
  use.
- Configure Jenkins credential ID `dockerhub-credentials` as **Username with
  password**, using a Docker Hub username and access token. Create the six
  public repositories listed below if they do not already exist.
- Authenticate Azure CLI as the Jenkins build user, using its assigned managed
  identity or a securely provisioned Azure identity. Select the subscription
  containing the AKS cluster. For a managed identity, the one-time setup can
  use:

  ```sh
  az login --identity
  az account set --subscription "<AKS subscription>"
  az aks get-credentials --resource-group rgroup --name therishuraj --overwrite-existing
  kubectl config get-contexts
  kubectl config use-context therishuraj
  kubectl get namespaces
  ```

  The pipeline refreshes AKS credentials with Azure CLI on each deployment.
  Do not put a personal kubeconfig or admin credential in the repository.
  Installing tools or logging into Azure as another user does not grant Jenkins
  access. The Jenkins identity must be allowed to retrieve AKS credentials and
  apply the `bank` Namespace and its resources, including reading the existing
  `bank-database` Secret.
- Keep the root `.env` file and `MYSQL_ROOT_PASSWORD` entry required by the
  existing learning setup. On deployment, the pipeline creates the
  `bank-database` Secret with key `mysql-root-password` only when it is absent;
  it preserves an existing Secret so it stays aligned with the MySQL PVC. The
  pipeline does not print the password. Keep `.env` access restricted and do
  not add a real production password to source control.
- Create a Jenkins **Pipeline** job configured as **Pipeline script from SCM**:
  use this Git repository, branch `main`, and script path `Jenkinsfile`. The
  pipeline declares `H/2 * * * *` SCM polling. A push is detected on the next
  successful poll while Jenkins and Git access are available; this is not an
  instantaneous webhook.

The `Publish versioned images` and `Deploy to AKS` stages run only for `main`.
The full Maven `test` phase runs for all five Java projects before packaging.
Test, package, image-build, image-push, or deployment failures stop subsequent
stages. Jenkins also publishes available Maven Surefire JUnit reports.
Account, Customer, and Identity context-load tests use test-scoped H2 so the
unit-test phase does not depend on a running MySQL service; application runtime
database configuration remains unchanged.

## Images and frontend API routing

Every `main` build publishes the following images with the same tag
`build-${BUILD_NUMBER}`:

- `rishuraj1/discoveryservice`
- `rishuraj1/accountservice`
- `rishuraj1/customer-service`
- `rishuraj1/identity-service`
- `rishuraj1/swiggy-gateway`
- `rishuraj1/bank-frontend`

The frontend is built with relative `/api` and `/auth` URLs. Its Nginx
configuration forwards those paths to the in-cluster `swiggygateway` Service;
the browser therefore does not call `localhost` or need a separately exposed
Gateway endpoint. The existing frontend NodePort remains `30080`; no additional
LoadBalancer Service is created.

## Verify and roll back

After a successful build, use a reachable AKS node address and the existing
NodePort/network rules to open the frontend at port `30080`. Do not assume a
public endpoint exists unless the AKS network exposes one. Check workloads with:

```sh
kubectl get pods -n bank
kubectl get deployments,services,statefulsets -n bank
kubectl get events -n bank --sort-by=.metadata.creationTimestamp
kubectl rollout status deployment/accountservice -n bank --timeout=300s
```

The pipeline waits for MySQL's StatefulSet, each application Deployment, and
frontend pod readiness. A timeout, failed rollout, missing `.env` password, or
unavailable AKS context fails the build.

To undo a deployment, roll back the affected application deployments to their
previous ReplicaSets, then verify their rollouts:

```sh
kubectl rollout undo deployment/bankfrontend -n bank
kubectl rollout undo deployment/swiggygateway -n bank
kubectl rollout undo deployment/identityservice -n bank
kubectl rollout undo deployment/customerservice -n bank
kubectl rollout undo deployment/accountservice -n bank
kubectl rollout undo deployment/discoveryservice -n bank
kubectl get pods -n bank
```

MySQL and its PVC are intentionally not rolled back.

## Scope and limits

This is a learning deployment, not a production platform. It does not configure
Jenkins, provision Azure/Docker Hub credentials or the database Secret, add a
webhook, change service logic or database schema, add ingress/public IP
resources, or establish that a deployment succeeded. Verify Jenkins-agent
tooling, credentials, AKS authentication, network access, and Azure capacity in
the target environment before relying on polling.
