resource "google_dataplex_lake" "lake" {
  location     = var.lake_location
  name         = var.lake_name
  description  = var.lake_description
  display_name = var.lake_display_name
}

resource "google_dataplex_zone" "lake-zone" {
  for_each = { for i, value in var.lake-zones : i => value }

  discovery_spec {
    enabled = true
  }

  lake        = google_dataplex_lake.lake.name
  location    = google_dataplex_lake.lake.location
  name        = each.value.name
  description = each.value.description
  type        = each.value.type
  resource_spec {
    location_type = "SINGLE_REGION"
  }

  depends_on = [google_dataplex_lake.lake]
}

locals {
  assets_list = flatten([for zone, assets in var.assets : [
    for asset in assets : {
      dataplex_zone = zone
      name          = asset.name
      resource_name = asset.resource_name
      type          = asset.type
    }
  ]])
}

resource "google_dataplex_asset" "lake-asset" {
  for_each = { for i, value in local.assets_list : i => value }

  lake          = google_dataplex_lake.lake.id
  name          = each.value.name
  location      = google_dataplex_lake.lake.location
  dataplex_zone = each.value.dataplex_zone
  discovery_spec {
    enabled = true
  }

  resource_spec {
    name = each.value.resource_name
    type = each.value.type
  }

  depends_on = [google_dataplex_zone.lake-zone]
}
