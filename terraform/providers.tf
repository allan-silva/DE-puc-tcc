terraform {
  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "4.51.0"
    }
  }
}

provider "google" {
    credentials = file(var.credentials_file)
    project = var.project
    zone = var.zone
    region = var.region
}
