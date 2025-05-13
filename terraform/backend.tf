terraform {

    variable "bucket_backend" {
        type        = string
        description = "GCS bucket para el estado de Terraform"
    }

    variable "backend_prefix" {
        type        = string
        description = "Path/prefix dentro del bucket"
    }
  
    backend "gcs" {
        bucket      = var.bucket_backend
        prefix      = var.backend_prefix
    }
}