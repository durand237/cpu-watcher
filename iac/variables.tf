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
