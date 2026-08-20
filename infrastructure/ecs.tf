 resource "aws_ecs_cluster" "main" {
    name = "${var.app_name}-cluster"

    tags = {
      Name = "${var.app_name}-cluster"
    }
  }

  resource "aws_cloudwatch_log_group" "app" {
    name              = "/ecs/${var.app_name}"
    retention_in_days = 7
  }

  resource "aws_ecs_task_definition" "app" {
    family                   = var.app_name
    network_mode             = "awsvpc"
    requires_compatibilities = ["FARGATE"]
    cpu                      = "512"
    memory                   = "1024"
    execution_role_arn       = aws_iam_role.ecs_task_execution.arn
    task_role_arn            = aws_iam_role.ecs_task.arn

    container_definitions = jsonencode([
      {
        name      = var.app_name
        image     = var.ecr_image_uri
        essential = true

        portMappings = [
          {
            containerPort = 8080
            protocol      = "tcp"
          }
        ]

        environment = [
          { name = "SPRING_PROFILES_ACTIVE", value = "prod" },
          { name = "DB_URL",                 value = "jdbc:postgresql://${aws_db_instance.main.address}:5432/${var.db_name}" },
          { name = "EMAIL_HOST",             value = var.email_host },
          { name = "EMAIL_PORT",             value = tostring(var.email_port) }
        ]

        secrets = [
          { name = "DB_USER",            valueFrom = aws_secretsmanager_secret.db_user.arn },
          { name = "DB_PASS",            valueFrom = aws_secretsmanager_secret.db_pass.arn },
          { name = "ANTHROPIC_API_KEY",  valueFrom = aws_secretsmanager_secret.anthropic_key.arn },
          { name = "MOVEBANK_USERNAME",  valueFrom = aws_secretsmanager_secret.movebank_username.arn },
          { name = "MOVEBANK_PASSWORD",  valueFrom = aws_secretsmanager_secret.movebank_password.arn },
          { name = "EMAIL_USERNAME",     valueFrom = aws_secretsmanager_secret.email_username.arn },
          { name = "EMAIL_PASSWORD",     valueFrom = aws_secretsmanager_secret.email_password.arn },
          { name = "KEY",                valueFrom = aws_secretsmanager_secret.api_key.arn }
        ]

        logConfiguration = {
          logDriver = "awslogs"
          options = {
            "awslogs-group"         = aws_cloudwatch_log_group.app.name
            "awslogs-region"        = var.aws_region
            "awslogs-stream-prefix" = "ecs"
          }
        }
      }
    ])
  }

  resource "aws_ecs_service" "app" {
    name            = "${var.app_name}-service"
    cluster         = aws_ecs_cluster.main.id
    task_definition = aws_ecs_task_definition.app.arn
    desired_count   = 1
    launch_type     = "FARGATE"
    enable_execute_command = true

    network_configuration {
      subnets          = [aws_subnet.private_1.id, aws_subnet.private_2.id]
      security_groups  = [aws_security_group.ecs.id]
      assign_public_ip = false
    }

    load_balancer {
      target_group_arn = aws_lb_target_group.app.arn
      container_name   = var.app_name
      container_port   = 8080
    }

    depends_on = [aws_lb_listener.http]
  }