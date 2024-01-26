module "apis" {
  for_each = toset(var.required_api_services)
  source   = "./apis-service"

  service_name = each.key
}

module "buckets" {
  for_each = toset(var.lake-buckets)
  source   = "./buckets"

  bucket_name = each.key
}
