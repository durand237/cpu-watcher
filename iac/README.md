# Initial AWS infrastructure

This directory is a deliberately small first deployment target for CPU Watcher:

```text
Internet -> CloudFront -> private S3 bucket (React build)
Internet -> EC2 public IP :80/:443 -> future Nginx/Docker Compose stack
EC2 host -> host-native collector -> loopback backend :8080
EC2 instance role -> ECR (pull backend image)
```

Terraform creates a VPC, one public subnet, an Internet gateway and route table,
an EC2 security group, an IAM instance role, an ECR repository, an Amazon Linux
2023 EC2 instance, a private S3 frontend bucket, and a CloudFront distribution.

It also deploys the collector directly on the EC2 host as a `systemd` service. On
first boot, the instance clones the configured public repository at the configured
ref, builds `collector/`, and starts `cpu-watcher-collector`. The API key is kept
in a SecureString SSM parameter and the instance role may read only that parameter.
The collector posts to `http://127.0.0.1:8080`, so it starts sending data once the
future backend deployment is running locally on the host.

It deliberately does **not** create RDS, ECS, Fargate, a NAT gateway, a domain,
a TLS certificate, a database, or any application containers. Docker is installed
on the EC2 host, but production Compose files are a later checkpoint.

## Collector configuration

Set a non-empty `collector_api_key` in `terraform.tfvars` (see
`terraform.tfvars.example`). The value is marked sensitive in Terraform output,
but it is still stored in Terraform state because Terraform creates the SSM
parameter; keep the state file protected. The default source repository is this
project's public GitHub repository and the default ref is `main`. Change
`collector_repository_url` and `collector_repository_ref` if a release repository
or immutable tag should be deployed.

After applying, use Session Manager to check the service:

```bash
sudo systemctl status cpu-watcher-collector
sudo journalctl -u cpu-watcher-collector -f
```

## EBS and the database

The EC2 instance always has an encrypted gp3 root EBS volume. In this first
configuration the Docker volume that will store PostgreSQL data lives on that
root volume. That is adequate for a low-cost development deployment, but it is
not a backup strategy: terminating the instance also deletes that root volume.

Before treating it as a live production database, add either a separately managed
and mounted EBS data volume with scheduled snapshots, or move PostgreSQL to RDS.
Do not run `terraform destroy` against an instance that holds data you need.

## Before applying to AWS

1. Configure a real AWS SSO/profile and run `aws sts get-caller-identity`.
2. Run `terraform init`, `terraform fmt -recursive`, `terraform validate`, and
   `terraform plan` from this directory.
3. Review the plan and AWS cost estimate before `terraform apply`.

The current state backend is local by design. The next infrastructure checkpoint
should create a dedicated, versioned state bucket and switch this configuration to
the locked S3 backend before GitHub Actions is permitted to apply changes.

## LocalStack

This is production-shaped AWS configuration, not a LocalStack-only configuration.
Run `terraform fmt` and `terraform validate` locally without credentials. A full
`apply` is only meaningful once the target LocalStack services and Docker runtime
match the selected plan; never carry a LocalStack endpoint into an AWS deployment.
