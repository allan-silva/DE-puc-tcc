output "lake-datasets" {
  value = google_bigquery_dataset.lake-dataset[*].id
}
