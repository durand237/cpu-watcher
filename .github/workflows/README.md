# GitHub Actions delivery pipeline

The workflows are deliberately separated so a frontend, backend, collector, or
Terraform change does not run every project check.

| Workflow | Pull request and `main` trigger | Release tag | Deployment target |
| --- | --- | --- | --- |
| `frontend.yml` | `frontend/**` | `frontend-v*` | S3 and CloudFront |
| `backend.yml` | `backend/**`, `compose.yaml`, `nginx/**` | `backend-v*` | ECR |
| `collector.yml` | `collector/**` | `collector-v*` | Verified JAR artifact only |
| `terraform.yml` | `iac/**` | `iac-v*` | AWS infrastructure |

All pull-request and `main` jobs test, build, format, or validate only. A
deployment requires an immutable, component-specific Git tag. Examples:

```bash
git tag -a backend-v1.0.0 -m "Release backend v1.0.0"
git push origin backend-v1.0.0

git tag -a frontend-v1.0.0 -m "Release frontend v1.0.0"
git push origin frontend-v1.0.0

git tag -a iac-v1.0.0 -m "Release infrastructure v1.0.0"
git push origin iac-v1.0.0
```

Protect these tag patterns and the `production` GitHub environment before using
them for releases. The `production` environment should require an approval.

Each release tag must use semantic versioning: `component-vMAJOR.MINOR.PATCH`,
with an optional prerelease suffix such as `backend-v1.2.0-rc.1`. The workflows
reject other tag formats. They pass the version into the backend and collector
JAR builds, inject it into the frontend bundle, and tag AWS resources with the
infrastructure release version. A Git tag marks an already committed revision;
it never replaces `git commit`.

## OIDC safety gate

No AWS mutation occurs until AWS OIDC is configured and these repository or
environment variables are set:

| Variable | Used by |
| --- | --- |
| `AWS_DEPLOY_ROLE_ARN` | All AWS deployment jobs |
| `AWS_DEFAULT_REGION` | All AWS deployment jobs |
| `AWS_ACCOUNT_ID` | All AWS deployment jobs; prevents use of the wrong account |
| `ECR_REPOSITORY` | Backend release, e.g. `cpu-watcher-dev-backend` |
| `FRONTEND_BUCKET_NAME` | Frontend release |
| `CLOUDFRONT_DISTRIBUTION_ID` | Frontend release |
| `TERRAFORM_DEPLOY_ENABLED` | Terraform release only; set to exactly `true` after remote state is configured |

The collector key must be stored as the protected GitHub Actions secret
`TF_VAR_COLLECTOR_API_KEY`; the Terraform workflow maps it to
`TF_VAR_collector_api_key`. Never commit it or add it as a plain repository
variable. Add this secret only after OIDC and remote state are ready.

The Terraform `apply` job is intentionally skipped until
`TERRAFORM_DEPLOY_ENABLED=true`. Do not enable it while Terraform uses the current
local state backend: a GitHub-hosted runner is ephemeral and cannot safely retain
that state. First configure a protected, remote state backend with locking.

The workflows use GitHub OIDC (`id-token: write`) and do not accept permanent AWS
access keys. The AWS role trust policy must restrict access to this repository and
the protected `production` environment or approved release ref.
