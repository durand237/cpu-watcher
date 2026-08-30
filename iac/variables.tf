variable "aws_region" {
  description = "AWS region for the first deployment."
  type        = string
  default     = "eu-central-1"
}

variable "project_name" {
  description = "Short project name used in resource names and tags."
  type        = string
  default     = "cpu-watcher"
}

variable "environment" {
  description = "Deployment environment name."
  type        = string
  default     = "dev"
}

variable "release_version" {
  description = "Infrastructure release version applied as an AWS resource tag."
  type        = string
  default     = "unreleased"
}

variable "vpc_cidr" {
  description = "CIDR range for the project VPC."
  type        = string
  default     = "10.20.0.0/16"
}

variable "public_subnet_cidr" {
  description = "CIDR range for the single public subnet that hosts the EC2 instance."
  type        = string
  default     = "10.20.1.0/24"
}

variable "instance_type" {
  description = "Small burstable EC2 instance type. t3.micro is the default for this single-host deployment."
  type        = string
  default     = "t3.micro"
}

variable "root_volume_size_gib" {
  description = "Size of the encrypted root EBS volume. Docker named volumes, including PostgreSQL data, initially live here."
  type        = number
  default     = 30

  validation {
    condition     = var.root_volume_size_gib >= 20
    error_message = "Use at least 20 GiB so the operating system, Docker images, and database have room."
  }
}

variable "ssh_allowed_cidr" {
  description = "Optional CIDR allowed to use SSH. Leave null and use AWS Systems Manager Session Manager instead."
  type        = string
  default     = null
  nullable    = true
}

variable "collector_api_key" {
  description = "Non-empty API key for the host collector. Terraform stores this sensitive value in state; protect the state file."
  type        = string
  sensitive   = true

  validation {
    condition     = length(trimspace(var.collector_api_key)) > 0
    error_message = "collector_api_key must be non-empty."
  }
}

variable "collector_repository_url" {
  description = "Public HTTPS Git repository containing the collector source used during first boot."
  type        = string
  default     = "https://github.com/durand237/cpu-watcher.git"

  validation {
    condition     = can(regex("^https://github\\.com/[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+(?:\\.git)?$", var.collector_repository_url))
    error_message = "collector_repository_url must be a public HTTPS GitHub repository URL."
  }
}

variable "collector_repository_ref" {
  description = "Git branch or tag to build for the host collector."
  type        = string
  default     = "main"

  validation {
    condition     = can(regex("^[A-Za-z0-9._/-]+$", var.collector_repository_ref))
    error_message = "collector_repository_ref may contain only letters, numbers, dots, underscores, slashes, and hyphens."
  }
}
