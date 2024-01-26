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

variable "lake-datasets" {
  type = list(object(
    {
      id            = string
      friendly_name = string
      description   = string
    }
  ))

  default = [{
    id            = "sus"
    friendly_name = "SUS"
    description   = "Informações comuns entre os sistemas do SUS"
    },
    {
      id            = "informacoes_ambulatoriais"
      friendly_name = "Informacoes ambulatoriais"
      description   = "SIA/SUS - Sistemas de Informações Ambulatoriais do SUS"
    },
    {
      id            = "informacoes_hospitalares"
      friendly_name = "Informacoes hospitalares"
      description   = "SIH/SUS - Sistemas de Informações Hospitalares do SUS"
  }]
}
