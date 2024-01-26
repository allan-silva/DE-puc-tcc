variable "project" {
  default = "puc-tcc-412315"
}

variable "credentials_file" {
  default = "/home/allan/secdrop/puc-tcc-412315-9e63f609ce1f.json"
}

variable "region" {
  default = "us-central1"
}

variable "zone" {
  default = "us-central1-c"
}

variable "required_api_services" {
  type = list(string)
  default = [
    "dataplex.googleapis.com",
    "datacatalog.googleapis.com",
    "dataproc.googleapis.com"
  ]
}

variable "lake-buckets" {
  type = list(string)
  default = [
    "sus-raw",
    "sus-curated",
    "informacoes-ambulatoriais-raw",
    "informacoes-ambulatoriais-curated",
    "informacoes-hospitalares-raw",
    "informacoes-hospitalares-curated",
    "painel-oncologia-raw",
    "painel-oncologia-curated",
  ]
}
