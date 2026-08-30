# Infrastructure as Code (Terraform)

The [`iac/`](../iac/) directory defines the first AWS deployment for CPU Watcher.
Run Terraform from that directory; Terraform automatically combines every `.tf`
file in it into one configuration. There is no single Terraform equivalent to an
Nginx `nginx.conf` include file.

```text
Internet -> CloudFront -> private S3 bucket (frontend files)
Internet -> EC2 public IP :80/:443 -> future Nginx and Docker Compose stack
EC2 host -> host-native collector -> http://127.0.0.1:8080
EC2 instance role -> ECR and the collector API-key SSM parameter
```

## What Terraform creates

- A VPC, public subnet, internet gateway, route table, and application security group.
- An Amazon Linux 2023 EC2 instance with an encrypted gp3 root volume.
- An IAM role and instance profile for the EC2 host.
- An ECR repository for future backend images.
- A private S3 bucket and CloudFront distribution for the frontend.
- A SecureString Systems Manager Parameter Store value for the collector API key.
- A host-native collector service on the EC2 instance.

The configuration does not yet deploy the backend, database, Nginx, or frontend
application containers. The collector will retry requests until a backend is
available on the same host at `127.0.0.1:8080`.

## File layout

| File | Purpose |
| --- | --- |
| `providers.tf` | AWS provider and shared resource tags. |
| `variables.tf` | Deployment inputs and validation. |
| `network.tf` | Network and security-group resources. |
| `identity.tf` | EC2 role, ECR access, Session Manager access, and collector parameter access. |
| `compute.tf` | ECR repository, collector API-key parameter, and EC2 instance. |
| `frontend.tf` | Private S3 frontend bucket and CloudFront distribution. |
| `user-data.sh.tftpl` | Linux first-boot script that installs and starts the collector. |
| `outputs.tf` | Public IP, CloudFront hostname, ECR URL, and Session Manager command. |

## Collector deployment

The collector deliberately runs directly on the Amazon Linux host. This allows
OSHI to inspect the host's processes; a normal container would only see its own
process namespace.

At the EC2 instance's first boot, `user-data.sh.tftpl`:

1. Installs Docker, Git, and Java 17.
2. Clones `collector_repository_url` at `collector_repository_ref`.
3. Builds the collector JAR.
4. Creates the `cpu-watcher-collector` systemd service.
5. Retrieves `collector_api_key` from Parameter Store at startup.

The EC2 role can read only this collector API-key parameter. It does not use a
developer's AWS credentials.

Terraform also applies a `Release` tag to every managed AWS resource. Local
commands use `unreleased` by default; an `iac-vMAJOR.MINOR.PATCH` workflow run
uses the version derived from that immutable Git tag.

After applying Terraform, connect through Session Manager and inspect the service:

```bash
sudo systemctl status cpu-watcher-collector
sudo journalctl -u cpu-watcher-collector -f
```

Changing the user-data template replaces the EC2 instance because
`user_data_replace_on_change` is enabled. Do not make this change casually once
the instance stores data that you need.

## Required configuration

Copy the example file before the first deployment:

```bash
cd iac
cp terraform.tfvars.example terraform.tfvars
```

Set a strong, non-empty API key in `terraform.tfvars`:

```hcl
collector_api_key = "replace-with-a-long-random-secret"
```

`terraform.tfvars` and Terraform state files are ignored by Git and must never
be committed. Although Terraform marks `collector_api_key` as sensitive, its
value is retained in Terraform state because Terraform creates the SecureString
parameter. Protect the state accordingly.

## Local deployment

Terraform 1.16.0 or newer is required. Authenticate with an AWS SSO profile or
other approved local AWS credentials, then run:

```bash
cd iac
terraform init
terraform fmt -recursive
terraform validate
terraform plan
terraform apply
```

Review the plan before applying it. `terraform destroy` will remove the EC2
instance; its root EBS volume is also deleted, including Docker volumes stored
there.

## CI deployment and OIDC

Before a CI workflow is allowed to run `terraform apply`:

1. Move state from the local backend to a protected remote backend with locking.
2. Create an AWS OIDC identity provider for the CI platform.
3. Create a dedicated deployment role with least-privilege Terraform permissions.
4. Restrict the role trust policy to this repository and protected deployment branch.
5. Protect the deployment branch, CI environment, workflow file, and secret variables.

The deployment role is separate from the EC2 instance role in `identity.tf`.
OIDC uses short-lived credentials, so permanent AWS access keys should not be
stored in the repository or CI configuration.

See the [GitHub Actions pipeline guide](../.github/workflows/README.md) for the
path-based triggers, release-tag format, and variables that keep deployment jobs
disabled until the OIDC and remote-state setup is complete.

## Operating-system support

This configuration is written for Amazon Linux 2023. Its user-data is Bash and
the collector runs as a Linux `systemd` service. Deploying to Windows Server
would require a separate AMI, PowerShell user data, and a Windows service or
scheduled-task implementation; it is not supported by the current files.
