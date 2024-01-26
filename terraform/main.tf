module "apis" {
  for_each = toset(var.required_api_services)
  source   = "./apis-service"

  service_name = each.key
}