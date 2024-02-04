resource "google_dataproc_workflow_template" "datasus-workflow-template" {
  name        = var.name
  location    = var.location
  dag_timeout = "3600s"
  placement {
    managed_cluster {
      cluster_name = "${var.name}"

      config {
        gce_cluster_config {
          network = "default"
          zone    = "us-central1-c"
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
          num_instances = 2
          disk_config {
            boot_disk_size_gb = 50
            boot_disk_type    = "pd-standard"
          }
        }

        software_config {
          image_version = "2.2-debian11"
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
      properties = {
        "spark.jars.packages" : "com.google.cloud.spark:spark-bigquery-with-dependencies_2.12:0.36.1,com.google.cloud:google-cloud-bigquery:2.37.0"
      }
    }
  }
}
