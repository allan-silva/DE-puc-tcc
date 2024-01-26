output "lake-bucket" {
  value = google_storage_bucket.lake-bucket[*].url
}
