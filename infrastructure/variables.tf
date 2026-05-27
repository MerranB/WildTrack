 variable "aws_region" {
    description = "AWS region to deploy into"
    type        = string
    default     = "us-east-1"
  }

  variable "app_name" {
    description = "Application name used for naming resources"
    type        = string
    default     = "wildtrack"
  }

  variable "environment" {
    description = "Deployment environment"
    type        = string
    default     = "prod"
  }

  variable "db_name" {
    description = "Database name"
    type        = string
    default     = "wildtrack"
  }

  variable "db_username" {
    description = "Database master username"
    type        = string
    default     = "wildtrack_admin"
  }

  variable "db_password" {
    description = "Database master password"
    type        = string
    sensitive   = true
  }

  variable "ecr_image_uri" {
    description = "ECR image URI for the application"
    type        = string
  }

 variable "email_host" {
    description = "SMTP email host"
    type        = string
  }

  variable "email_port" {
    description = "SMTP email port"
    type        = number
    default     = 587
  }

variable "anthropic_api_key" {
    description = "Anthropic API key"
    type        = string
    sensitive   = true
  }

  variable "movebank_username" {
    description = "Movebank username"
    type        = string
    sensitive   = true
  }

  variable "movebank_password" {
    description = "Movebank password"
    type        = string
    sensitive   = true
  }

  variable "email_username" {
    description = "SMTP email username"
    type        = string
    sensitive   = true
  }

  variable "email_password" {
    description = "SMTP email password"
    type        = string
    sensitive   = true
  }

  variable "api_key" {
    description = "WildTrack API key"
    type        = string
    sensitive   = true
  }

  variable "domain_name" {
    description = "Registered domain name"
    type        = string
  }
