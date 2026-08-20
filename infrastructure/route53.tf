resource "aws_route53_zone" "main" {
  name = var.domain_name
}

  resource "aws_route53_record" "main" {
    zone_id = aws_route53_zone.main.zone_id
    name    = var.domain_name
    type    = "A"
    ttl     = 300
    records = [
      "185.199.108.153",
      "185.199.109.153",
      "185.199.110.153",
      "185.199.111.153",
    ]
}

output "route53_nameservers" {
  value       = aws_route53_zone.main.name_servers
  description = "Copy these 4 nameservers into Namecheap DNS settings"
}

resource "aws_route53_record" "api" {
    zone_id = aws_route53_zone.main.zone_id
    name    = "api.${var.domain_name}"
    type    = "A"

    alias {
      name                   = aws_lb.main.dns_name
      zone_id                = aws_lb.main.zone_id
      evaluate_target_health = true
    }
}

  resource "aws_route53_record" "www" {
    zone_id = aws_route53_zone.main.zone_id
    name    = "www.${var.domain_name}"
    type    = "CNAME"
    ttl     = 300
    records = ["merranb.github.io"]
}
