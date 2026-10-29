# Local Kubernetes

These manifests use the `bank` namespace. Kubernetes Services provide stable
in-namespace DNS names such as `mysql`, `discoveryservice`, and
`identityservice`; Eureka clients use the Discovery Service, and the Gateway
uses Eureka to find the application services.

1. Build the JARs before building the images. Run `.\mvnw.cmd -DskipTests package`
   in each Java service directory. For the Gateway and Identity Service, use
   `mvn.cmd -DskipTests package` if their Maven wrapper files are incomplete.
2. From the project root, build the local images:

   ```powershell
   docker build -t discoveryservice:latest .\DiscoveryService\DiscoveryService
   docker build -t accountservice:latest .\AccountService\AccountService
   docker build -t customer-service:latest .\CustomerService\CustomerService
   docker build -t identity-service:latest '.\identity-service (2) (1)\identity-service\identity-service'
   docker build -t swiggy-gateway:latest '.\swiggy-gateway (2)\swiggy-gateway\swiggy-gateway'
   docker build -t bank-frontend:latest --build-arg VITE_AUTH_API_URL=http://localhost:8765 --build-arg VITE_BANK_API_URL=http://localhost:8765 .\BankService\BankService
   ```

   These local image names are intended for Docker Desktop Kubernetes. For
   Minikube, load each image into Minikube after building it.
3. Keep a root `.env` with `MYSQL_ROOT_PASSWORD` for this tutorial. Jenkins
   reads that value and creates the `bank-database` Secret on first deployment;
   it leaves an existing Secret unchanged so it does not desynchronize
   credentials from the persistent MySQL volume. This example password is
   public and for tutorial use only.

   ```dotenv
   MYSQL_ROOT_PASSWORD=tutorial-only-password
   ```

4. To deploy through Jenkins, commit the root `Jenkinsfile` and `.env`, then
   create a Jenkins **Pipeline** job using **Pipeline script from SCM** and
   the public GitHub repository. The pipeline builds the local images,
   provisions the Secret if it is missing, and deploys in dependency order.
   The Jenkins Windows account must have Docker Desktop, Maven, Java, and the
   active Kubernetes context available on its PATH/profile.

   For a manual deployment instead, create the namespace and Secret once:

   ```powershell
   kubectl apply -f .\k8s\01-namespace.yaml
   kubectl create secret generic bank-database -n bank --from-literal=mysql-root-password='tutorial-only-password'
   ```

   Then apply the manifests and wait for workloads:

   ```powershell
   kubectl apply -f .\k8s\
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
   directly to Windows, so forward the Services using the browser URLs:

   ```powershell
   kubectl port-forward -n bank service/bankfrontend 30080:80
   kubectl port-forward -n bank service/swiggygateway 8765:8765
   ```

   Keep both commands running and open `http://localhost:30080`. The frontend
   calls the Gateway at `http://localhost:8765`; Gateway CORS allows the
   frontend origin on port `30080`.

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
