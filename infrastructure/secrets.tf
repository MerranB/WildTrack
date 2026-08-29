 resource "aws_secretsmanager_secret" "db_user" {
    name = "${var.app_name}/db_user"
  }

  resource "aws_secretsmanager_secret_version" "db_user" {
    secret_id     = aws_secretsmanager_secret.db_user.id
    secret_string = var.db_username
  }

  resource "aws_secretsmanager_secret" "db_pass" {
    name = "${var.app_name}/db_pass"
  }

  resource "aws_secretsmanager_secret_version" "db_pass" {
    secret_id     = aws_secretsmanager_secret.db_pass.id
    secret_string = var.db_password
  }

  resource "aws_secretsmanager_secret" "anthropic_key" {
    name = "${var.app_name}/anthropic_key"
  }

  resource "aws_secretsmanager_secret_version" "anthropic_key" {
    secret_id     = aws_secretsmanager_secret.anthropic_key.id
    secret_string = var.anthropic_api_key
  }

  resource "aws_secretsmanager_secret" "movebank_username" {
    name = "${var.app_name}/movebank_username"
  }

  resource "aws_secretsmanager_secret_version" "movebank_username" {
    secret_id     = aws_secretsmanager_secret.movebank_username.id
    secret_string = var.movebank_username
  }

  resource "aws_secretsmanager_secret" "movebank_password" {
    name = "${var.app_name}/movebank_password"
  }

  resource "aws_secretsmanager_secret_version" "movebank_password" {
    secret_id     = aws_secretsmanager_secret.movebank_password.id
    secret_string = var.movebank_password
  }

  resource "aws_secretsmanager_secret" "email_username" {
    name = "${var.app_name}/email_username"
  }

  resource "aws_secretsmanager_secret_version" "email_username" {
    secret_id     = aws_secretsmanager_secret.email_username.id
    secret_string = var.email_username
  }

  resource "aws_secretsmanager_secret" "email_password" {
    name = "${var.app_name}/email_password"
  }

  resource "aws_secretsmanager_secret_version" "email_password" {
    secret_id     = aws_secretsmanager_secret.email_password.id
    secret_string = var.email_password
  }

  resource "aws_secretsmanager_secret" "api_key" {
    name = "${var.app_name}/api_key"
  }

  resource "aws_secretsmanager_secret_version" "api_key" {
    secret_id     = aws_secretsmanager_secret.api_key.id
    secret_string = var.api_key
  }

 resource "aws_secretsmanager_secret" "wildtrack_admin_username" {
      name = "${var.app_name}/wildtrack_admin_username"
    }

    resource "aws_secretsmanager_secret_version" "wildtrack_admin_username" {
      secret_id     = aws_secretsmanager_secret.wildtrack_admin_username.id
      secret_string = var.wildtrack_admin_username
    }

    resource "aws_secretsmanager_secret" "wildtrack_admin_password" {
      name = "${var.app_name}/wildtrack_admin_password"
    }

    resource "aws_secretsmanager_secret_version" "wildtrack_admin_password" {
      secret_id     = aws_secretsmanager_secret.wildtrack_admin_password.id
      secret_string = var.wildtrack_admin_password
    }

    resource "aws_secretsmanager_secret" "wildtrack_jwt_secret" {
      name = "${var.app_name}/wildtrack_jwt_secret"
    }

    resource "aws_secretsmanager_secret_version" "wildtrack_jwt_secret" {
      secret_id     = aws_secretsmanager_secret.wildtrack_jwt_secret.id
      secret_string = var.wildtrack_jwt_secret
    }