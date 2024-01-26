resource "google_bigquery_dataset" "lake-dataset" {
    dataset_id = var.dataset_id
    friendly_name = var.dataset_friendly_name
    description = var.dataset_description
    location = "US"
}
