# Enable required APIs
module "apis" {
  for_each = toset(var.required_api_services)
  source   = "./apis-service"

  service_name = each.key
}

# Create cloud storage buckets

module "informacoes-ambulatoriais-raw" {
  source = "./buckets"

  bucket_name   = "informacoes-ambulatoriais-raw"
  bucket_region = var.region
}

module "informacoes-hospitalares-raw" {
  source = "./buckets"

  bucket_name   = "informacoes-hospitalares-raw"
  bucket_region = var.region
}

module "sus-raw" {
  source = "./buckets"

  bucket_name   = "sus-raw"
  bucket_region = var.region
}

module "informacoes-ambulatoriais-curated" {
  source = "./buckets"

  bucket_name   = "informacoes-ambulatoriais-curated"
  bucket_region = var.region
}

module "informacoes-hospitalares-curated" {
  source = "./buckets"

  bucket_name   = "informacoes-hospitalares-curated"
  bucket_region = var.region
}

module "sus-curated" {
  source = "./buckets"

  bucket_name   = "sus-curated"
  bucket_region = var.region
}

module "datasus-spark-jobs" {
  source = "./buckets"

  bucket_name   = "datasus-spark-jobs"
  bucket_region = var.region
}

# Create BigQuery datasets

module "ingestion_info-dataset" {
  source = "./big-query"

  dataset_id            = "ingestion_info"
  dataset_friendly_name = "Files processing ingestion information."
  dataset_description   = "Files processing ingestion information."
}

resource "google_bigquery_table" "discovered_files_table" {
  dataset_id          = module.ingestion_info-dataset.lake-dataset-dataset_id
  deletion_protection = false
  table_id            = "discovered_files"
  schema              = <<EOF
[
  {
    "name": "file_uri",
    "type": "STRING",
    "mode": "REQUIRED"
  },
  {
    "name": "source",
    "type": "STRING",
    "mode": "REQUIRED"
  },
  {
    "name": "discovery_date",
    "type": "DATE",
    "mode": "REQUIRED"
  }
]
  EOF
}

resource "google_bigquery_table" "curated_files_table" {
  dataset_id          = module.ingestion_info-dataset.lake-dataset-dataset_id
  deletion_protection = false
  table_id            = "curated_files"
  schema              = <<EOF
[
  {
    "name": "source_file_uri",
    "type": "STRING",
    "mode": "REQUIRED"
  },
  {
    "name": "parquet_file_uri",
    "type": "STRING"
  },
  {
    "name": "source",
    "type": "STRING",
    "mode": "REQUIRED"
  },
  {
    "name": "converted_date",
    "type": "DATE"
  },
  {
    "name": "success",
    "type": "BOOL",
    "mode": "REQUIRED"
  },
  {
    "name": "error_message",
    "type": "STRING"
  }
]
  EOF
}

resource "google_bigquery_table" "warehouse_datasets_table" {
  dataset_id          = module.ingestion_info-dataset.lake-dataset-dataset_id
  deletion_protection = false
  table_id            = "warehouse_datasets"
  schema              = <<EOF
[
  {
    "name": "parquet_file_uri",
    "type": "STRING",
    "mode": "REQUIRED"
  },
  {
    "name": "source",
    "type": "STRING",
    "mode": "REQUIRED"
  },
  {
    "name": "success",
    "type": "BOOL",
    "mode": "REQUIRED"
  },
  {
    "name": "dataset",
    "type": "STRING"
  },
  {
    "name": "table",
    "type": "STRING"
  },
  {
    "name": "entries_count",
    "type": "INT64"
  },
  {
    "name": "load_date",
    "type": "DATE"
  },
  {
    "name": "error_message",
    "type": "STRING"
  }
]
  EOF
}

module "sus-dataset" {
  source = "./big-query"

  dataset_id            = "sus"
  dataset_friendly_name = "SUS"
  dataset_description   = "Informações comuns entre os sistemas do SUS"
}

module "informacoes_ambulatoriais-dataset" {
  source = "./big-query"

  dataset_id            = "informacoes_ambulatoriais"
  dataset_friendly_name = "Informacoes ambulatoriais"
  dataset_description   = "SIA/SUS - Sistemas de Informações Ambulatoriais do SUS"
}

module "informacoes_hospitalares-dataset" {
  source = "./big-query"

  dataset_id            = "informacoes_hospitalares"
  dataset_friendly_name = "Informacoes hospitalares"
  dataset_description   = "SIH/SUS - Sistemas de Informações Hospitalares do SUS"
}

# Create data lake
# module "sus-lake" {
#   source            = "./lake"
#   lake_location     = var.region
#   lake_name         = "sus-lake"
#   lake_description  = "Sistema Único de Saúde - Data Lake"
#   lake_display_name = "Sistema Único de Saúde - Data Lake"
#   lake-zones = [
#     {
#       name         = "informacoes-ambulatoriais-raw"
#       description  = "Raw Data - SIA/SUS - Sistemas de Informações Ambulatoriais do SUS"
#       type         = "RAW"
#       display_name = "SIA/SUS - Sistemas de Informações Ambulatoriais do SUS"
#     },
#     {
#       name         = "informacoes-hospitalares-raw"
#       description  = "Raw Data - SIH/SUS - Sistemas de Informações Hospitalares do SUS"
#       type         = "RAW"
#       display_name = "Raw Data - SIH/SUS - Sistemas de Informações Hospitalares do SUS"
#     },
#     {
#       name         = "sus-raw"
#       description  = "Raw Data - Informações comuns entre os sistemas do SUS"
#       type         = "RAW"
#       display_name = "Raw Data - Informações comuns entre os sistemas do SUS"
#     },
#     {
#       name         = "informacoes-ambulatoriais-curated"
#       description  = "Curated Data - SIA/SUS - Sistemas de Informações Ambulatoriais do SUS"
#       type         = "CURATED"
#       display_name = "Curated Data - SIA/SUS - Sistemas de Informações Ambulatoriais do SUS"
#     },
#     {
#       name         = "informacoes-hospitalares-curated"
#       description  = "Curated Data - SIH/SUS - Sistemas de Informações Hospitalares do SUS"
#       type         = "CURATED"
#       display_name = "Curated Data - SIH/SUS - Sistemas de Informações Hospitalares do SUS"
#     },
#     {
#       name         = "sus-curated"
#       description  = "Curated Data - Informações comuns entre os sistemas do SUS"
#       type         = "CURATED"
#       display_name = "Curated Data - Informações comuns entre os sistemas do SUS"
#     }
#   ]
#   assets = {
#     "informacoes-ambulatoriais-raw" = [{
#       name          = "informacoes-ambulatoriais-raw"
#       resource_name = "projects/${var.project}/buckets/${module.informacoes-ambulatoriais-raw.lake-bucket-id}"
#       type          = "STORAGE_BUCKET"
#     }]

#     "informacoes-hospitalares-raw" = [{
#       name          = "informacoes-hospitalares-raw"
#       resource_name = "projects/${var.project}/buckets/${module.informacoes-hospitalares-raw.lake-bucket-id}"
#       type          = "STORAGE_BUCKET"
#     }]

#     "sus-raw" = [{
#       name          = "sus-raw"
#       resource_name = "projects/${var.project}/buckets/${module.sus-raw.lake-bucket-id}"
#       type          = "STORAGE_BUCKET"
#     }]

#     "informacoes-ambulatoriais-curated" = [{
#       name          = "informacoes-ambulatoriais-curated"
#       resource_name = "projects/${var.project}/buckets/${module.informacoes-ambulatoriais-curated.lake-bucket-id}"
#       type          = "STORAGE_BUCKET"
#       },
#       {
#         name          = "informacoes-ambulatoriais-dataset"
#         resource_name = module.informacoes_ambulatoriais-dataset.lake-dataset-id
#         type          = "BIGQUERY_DATASET"
#     }]

#     "informacoes-hospitalares-curated" = [{
#       name          = "informacoes-hospitalares-curated"
#       resource_name = "projects/${var.project}/buckets/${module.informacoes-hospitalares-curated.lake-bucket-id}"
#       type          = "STORAGE_BUCKET"
#       },
#       {
#         name          = "informacoes-hospitalares-dataset"
#         resource_name = module.informacoes_hospitalares-dataset.lake-dataset-id
#         type          = "BIGQUERY_DATASET"
#     }]

#     "sus-curated" = [{
#       name          = "sus-curated"
#       resource_name = "projects/${var.project}/buckets/${module.sus-curated.lake-bucket-id}"
#       type          = "STORAGE_BUCKET"
#       },
#       {
#         name          = "sus-dataset"
#         resource_name = module.sus-dataset.lake-dataset-id
#         type          = "BIGQUERY_DATASET"
#     }]
#   }
#   depends_on = [
#     module.apis,
#     module.informacoes-ambulatoriais-raw,
#     module.informacoes-hospitalares-raw, module.sus-raw,
#     module.informacoes-ambulatoriais-curated,
#     module.informacoes-hospitalares-curated, module.sus-curated,
#     module.informacoes_ambulatoriais-dataset,
#     module.informacoes_hospitalares-dataset,
#     module.sus-dataset
#   ]
# }

# Dataproc workflow templates
module "dataproc-workflow-SIA" {
  source         = "./dataproc-workflow"
  name           = "informacoes-ambulatoriais"
  raw_bucket     = module.informacoes-ambulatoriais-raw.lake-bucket-url
  curated_bucket = module.informacoes-ambulatoriais-curated.lake-bucket-url
  source_system  = "SIA"
  job_bucket     = module.datasus-spark-jobs.lake-bucket-url
  location       = var.region
  depends_on     = [module.informacoes-ambulatoriais-raw, module.datasus-spark-jobs]
}

module "dataproc-workflow-SIH" {
  source         = "./dataproc-workflow"
  name           = "informacoes-hospitalares"
  raw_bucket     = module.informacoes-hospitalares-raw.lake-bucket-url
  curated_bucket = module.informacoes-hospitalares-curated.lake-bucket-url
  source_system  = "SIH"
  job_bucket     = module.datasus-spark-jobs.lake-bucket-url
  location       = var.region
  depends_on     = [module.informacoes-hospitalares-raw, module.datasus-spark-jobs]
}

module "dataproc-workflow-SUS" {
  source         = "./dataproc-workflow"
  name           = "sus"
  raw_bucket     = module.sus-raw.lake-bucket-url
  curated_bucket = module.sus-curated.lake-bucket-url
  source_system  = "SUS"
  job_bucket     = module.datasus-spark-jobs.lake-bucket-url
  location       = var.region
  depends_on     = [module.sus-raw, module.datasus-spark-jobs]
}
