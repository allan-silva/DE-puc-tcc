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

module "big-query" {
  for_each = {for i, value in var.lake-datasets: i => value}

  source = "./big-query"

  dataset_id            = each.value.id
  dataset_friendly_name = each.value.friendly_name
  dataset_description   = each.value.description
}