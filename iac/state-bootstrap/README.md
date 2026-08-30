# Terraform state bootstrap

The main Terraform configuration uses the S3 bucket created here for remote,
versioned state and native S3 locking. Create it once using an AWS identity that
is allowed to manage S3 buckets in account `333159324079`:

```bash
terraform -chdir=iac/state-bootstrap init
terraform -chdir=iac/state-bootstrap apply
```

Do not run `terraform destroy` in this directory. The state bucket has
`prevent_destroy` enabled because it contains the infrastructure state and the
sensitive collector API key.
