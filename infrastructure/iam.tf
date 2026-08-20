  resource "aws_iam_role" "ecs_task_execution" {
    name = "${var.app_name}-ecs-task-execution-role"

    assume_role_policy = jsonencode({
      Version = "2012-10-17"
      Statement = [
        {
          Action = "sts:AssumeRole"
          Effect = "Allow"
          Principal = {
            Service = "ecs-tasks.amazonaws.com"
          }
        }
      ]
    })
  }

  resource "aws_iam_role_policy_attachment" "ecs_task_execution" {
    role       = aws_iam_role.ecs_task_execution.name
    policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
  }

  resource "aws_iam_role" "ecs_task" {
    name = "${var.app_name}-ecs-task-role"

    assume_role_policy = jsonencode({
      Version = "2012-10-17"
      Statement = [
        {
          Action = "sts:AssumeRole"
          Effect = "Allow"
          Principal = {
            Service = "ecs-tasks.amazonaws.com"
          }
        }
      ]
    })
  }

  resource "aws_iam_role_policy" "ecs_task_secrets" {
    name = "${var.app_name}-ecs-task-secrets-policy"
    role = aws_iam_role.ecs_task.id

    policy = jsonencode({
      Version = "2012-10-17"
      Statement = [
        {
          Effect = "Allow"
          Action = [
            "secretsmanager:GetSecretValue"
          ]
          Resource = "*"
        }
      ]
    })
  }
 resource "aws_iam_role_policy" "ecs_task_execution_secrets" {
    name = "${var.app_name}-ecs-task-execution-secrets-policy"
    role = aws_iam_role.ecs_task_execution.id

    policy = jsonencode({
      Version = "2012-10-17"
      Statement = [
        {
          Effect   = "Allow"
          Action   = ["secretsmanager:GetSecretValue"]
          Resource = "*"
        }
      ]
    })
  }
  resource "aws_iam_role_policy" "ecs_task_ssm" {
    name = "${var.app_name}-ecs-task-ssm-policy"
    role = aws_iam_role.ecs_task.id

    policy = jsonencode({
      Version = "2012-10-17"
      Statement = [
        {
          Effect = "Allow"
          Action = [
            "ssmmessages:CreateControlChannel",
            "ssmmessages:CreateDataChannel",
            "ssmmessages:OpenControlChannel",
            "ssmmessages:OpenDataChannel"
          ]
          Resource = "*"
        }
      ]
    })
  } 