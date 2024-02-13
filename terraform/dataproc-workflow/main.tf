resource "google_dataproc_workflow_template" "datasus-workflow-template" {
  name        = var.name
  location    = var.location
  dag_timeout = "18000s"
  placement {
    managed_cluster {
      cluster_name = var.name

      config {
        gce_cluster_config {
          network         = "default"
          zone            = "us-central1-c"
          service_account = "dataproc@puc-tcc-412315.iam.gserviceaccount.com"
        }

        master_config {
          machine_type  = "e2-standard-2"
          num_instances = 1
          disk_config {
            boot_disk_size_gb = 50
            boot_disk_type    = "pd-standard"
          }
        }

        worker_config {
          machine_type  = "e2-standard-2"
          num_instances = var.worker_instances
          disk_config {
            boot_disk_size_gb = 50
            boot_disk_type    = "pd-standard"
          }
        }

        software_config {
          image_version = "2.2-debian12"
        }
      }
    }
  }

  jobs {
    step_id = "file-discovery"
    spark_job {
      args          = [var.raw_bucket, var.source_system]
      main_class    = "br.dev.contrib.gov.sus.opendata.jobs.DatasusFileDiscoveryMetadataJob"
      jar_file_uris = ["${var.job_bucket}/datasussparkjobs_2.12-0.1.0-SNAPSHOT.jar"]
    }
  }

  jobs {
    step_id               = "file-conversion"
    prerequisite_step_ids = ["file-discovery"]
    spark_job {
      args          = [var.source_system, var.curated_bucket, var.worker_instances]
      main_class    = "br.dev.contrib.gov.sus.opendata.jobs.FileConversionJob"
      jar_file_uris = ["${var.job_bucket}/datasussparkjobs_2.12-0.1.0-SNAPSHOT.jar"]
      properties = {
        "spark.jars.packages" : "br.dev.contrib.gov.sus.opendata:libdatasus-parquet-dbf:1.0.7"
      }
    }
  }

  jobs {
    step_id               = "parquet-load"
    prerequisite_step_ids = ["file-conversion"]
    spark_job {
      args          = [var.source_system]
      main_class    = "br.dev.contrib.gov.sus.opendata.jobs.ParquetLoadJob"
      jar_file_uris = ["${var.job_bucket}/datasussparkjobs_2.12-0.1.0-SNAPSHOT.jar"]
    }
  }
}
