output "application_public_ip" {
  description = "Public IP of the single Docker host. It is reachable on ports 80 and 443 only."
  value       = aws_instance.application.public_ip
}

output "ecr_repository_url" {
  description = "Repository where GitHub Actions will later push immutable backend images."
  value       = aws_ecr_repository.application.repository_url
}

output "frontend_bucket_name" {
  description = "Private S3 bucket to receive the built React frontend."
  value       = aws_s3_bucket.frontend.bucket
}

output "cloudfront_domain_name" {
  description = "HTTPS CloudFront address for the frontend after index.html is uploaded."
  value       = aws_cloudfront_distribution.frontend.domain_name
}

output "application_instance_id" {
  description = "EC2 instance ID targeted by the backend deployment workflow through SSM."
  value       = aws_instance.application.id
}

output "cloudfront_distribution_id" {
  description = "Distribution ID used by the frontend release workflow to invalidate cached files."
  value       = aws_cloudfront_distribution.frontend.id
}

output "session_manager_command" {
  description = "AWS CLI command for shell access without opening SSH."
  value       = "aws ssm start-session --target ${aws_instance.application.id} --region ${var.aws_region}"
}
