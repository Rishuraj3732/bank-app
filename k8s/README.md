# Local Kubernetes

These manifests use the `bank` namespace. Kubernetes Services provide stable
in-namespace DNS names such as `mysql`, `discoveryservice`, and
`identityservice`; Eureka clients use the Discovery Service, and the Gateway
uses Eureka to find the application services.

1. Build the JARs before building the images. Run `.\mvnw.cmd -DskipTests package`
   in each Java service directory. For the Gateway and Identity Service, use
   `mvn.cmd -DskipTests package` if their Maven wrapper files are incomplete.
2. The Kubernetes manifests pull versioned images from the public Docker Hub
   repositories under `rishuraj1`. Jenkins tags each image with its build
   number and deploys that exact tag. Create these repositories in Docker Hub
   and make them public so the local cluster can pull without a registry Secret:
   `discoveryservice`, `accountservice`, `customer-service`, `identity-service`,
   `swiggy-gateway`, and `bank-frontend`.

   ```powershell
   docker login
   ```

   For a manual deployment, build and push each image with the same tag, then
   render that tag into the Kubernetes manifests:

   ```powershell
   $tag = 'build-manual'
   docker build -t "rishuraj1/discoveryservice:$tag" .\DiscoveryService\DiscoveryService
   docker build -t "rishuraj1/accountservice:$tag" .\AccountService\AccountService
   docker build -t "rishuraj1/customer-service:$tag" .\CustomerService\CustomerService
   docker build -t "rishuraj1/identity-service:$tag" '.\identity-service (2) (1)\identity-service\identity-service'
   docker build -t "rishuraj1/swiggy-gateway:$tag" '.\swiggy-gateway (2)\swiggy-gateway\swiggy-gateway'
   docker build -t "rishuraj1/bank-frontend:$tag" --build-arg VITE_AUTH_API_URL=/ --build-arg VITE_BANK_API_URL=/ .\BankService\BankService
   docker push "rishuraj1/discoveryservice:$tag"
   docker push "rishuraj1/accountservice:$tag"
   docker push "rishuraj1/customer-service:$tag"
   docker push "rishuraj1/identity-service:$tag"
   docker push "rishuraj1/swiggy-gateway:$tag"
   docker push "rishuraj1/bank-frontend:$tag"
   bash ./scripts/render-k8s.sh rishuraj1 "$tag" k8s-deploy-manual
   ```

   The command above pushes each generated versioned image to Docker Hub. Remove
   `k8s-deploy-manual` after applying the manifests.
3. Keep a root `.env` with `MYSQL_ROOT_PASSWORD` for local Compose and the
   existing Jenkins learning setup. Jenkins creates the `bank-database` Secret
   only if it is missing, and preserves any existing Secret to avoid
   desynchronizing credentials from the MySQL PVC. The example password below
   is public and for tutorial use only.

   ```dotenv
   MYSQL_ROOT_PASSWORD=tutorial-only-password
   ```

4. For Jenkins-based AKS deployment, follow [DEPLOY_AKS.md](../DEPLOY_AKS.md).
   The Linux pipeline reads `.env` only when it needs to create the Secret and
   does not print its value. Restrict access to `.env`; do not commit real
   production credentials.

   For a manual deployment instead, create the namespace and Secret once:

   ```powershell
   kubectl apply -f .\k8s\01-namespace.yaml
   kubectl create secret generic bank-database -n bank --from-literal=mysql-root-password='tutorial-only-password'
   ```

   Then apply the manifests and wait for workloads:

   ```powershell
   kubectl apply -f .\k8s\01-namespace.yaml
   kubectl apply -f .\k8s\03-mysql.yaml
   kubectl apply -f .\k8s-deploy-manual
   kubectl get pods,services -n bank
   kubectl rollout status deployment/discoveryservice -n bank
   kubectl rollout status deployment/accountservice -n bank
   kubectl rollout status deployment/customerservice -n bank
   kubectl rollout status deployment/identityservice -n bank
   kubectl rollout status deployment/swiggygateway -n bank
   kubectl rollout status deployment/bankfrontend -n bank
   ```

5. After the rollout, check the assigned NodePorts:

   ```powershell
   kubectl get services -n bank
   ```

   The frontend and Gateway Services use NodePorts `30080` and `30765`.
   Docker Desktop's local Kubernetes kind cluster may not expose NodePorts
   directly to Windows, so forward the frontend Service:

   ```powershell
   kubectl port-forward -n bank service/bankfrontend 30080:80
   ```

   Keep the frontend command running and open `http://localhost:30080`. The
   frontend's Nginx container proxies `/api/` and `/auth/` requests to the
   in-cluster Gateway Service, so a separate Gateway port-forward is not
   required.

The customer and savings directories use these authenticated collection
endpoints:

- `GET /api/v1/customers` returns all customer records.
- `GET /api/v1/savings` returns all savings accounts.
- `GET /api/v1/customers/{customerId}`, `GET /api/v1/customers/profile`, and
  `GET /api/v1/savings/profile` remain available for individual profile data.

Account, Customer, Identity, Gateway, and frontend run two replicas. Eureka is
kept at one replica because this project configures a standalone registry;
high availability for the registry would require configuring Eureka peers.
MySQL also runs one replica and stores data in a PVC.

The Java containers have memory limits and bounded JVM heaps to fit a local
cluster. Startup probes allow Spring services and MySQL time to initialize
before Kubernetes starts liveness checks.
