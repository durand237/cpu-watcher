terraform {
  backend "s3" {
    bucket       = "cpu-watcher-terraform-state-333159324079"
    key          = "cpu-watcher/dev/terraform.tfstate"
    region       = "eu-central-1"
    encrypt      = true
    use_lockfile = true
  }
}
