resource "google_project_service" "project" {
    service = var.service_name
    disable_dependent_services = true
    disable_on_destroy = true

    timeouts {
      create = var.creation_timeout
      update = var.update_timeout
      delete = "60m"
    }
}
