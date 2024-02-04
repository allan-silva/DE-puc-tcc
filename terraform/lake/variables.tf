variable "lake_name" {

}

variable "lake_location" {

}

variable "lake_description" {

}

variable "lake_display_name" {

}

variable "lake-zones" {
  type = list(object(
    {
      name         = string
      type         = string
      description  = string
      display_name = string
    }
  ))
}

variable "assets" {
  type = map(list(object({
    name          = string,
    resource_name = string,
    type          = string
  })))
}
