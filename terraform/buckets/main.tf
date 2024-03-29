resource "google_storage_bucket" "lake-bucket" {
  name                     = var.bucket_name
  location                 = var.bucket_region
  force_destroy            = true
  storage_class            = "REGIONAL"
  public_access_prevention = "enforced"
}
