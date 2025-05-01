terraform {
  required_version = ">= 0.13"
  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 4.0"
    }
  }
}

variable "project_id" {
  description = "ID del proyecto de GCP"
  type        = string
}

variable "region" {
  description = "Región de GCP (Iowa)"
  type        = string
  default     = "us-central1"
}

variable "zone" {
  description = "Zona de GCP para cluster zonal"
  type        = string
  default     = "us-central1-a"
}

variable "credentials_file" {
  description = "Ruta al archivo de credenciales (terraform-sa-key.json)"
  type        = string
}

variable "cluster_name" {
  description = "Nombre del clúster GKE"
  type        = string
  default     = "gke-cluster"
}

variable "machine_type" {
  description = "Tipo de máquina para los nodos"
  type        = string
  default     = "e2-medium"
}

variable "node_count" {
  description = "Número de nodos en el pool principal"
  type        = number
  default     = 1
}

variable "boot_disk_size_gb" {
  description = "Tamaño del disco de arranque en GB"
  type        = number
  default     = 20
}

variable "disk_type" {
  description = "Tipo de disco de arranque (pd-standard|pd-balanced|pd-ssd)"
  type        = string
  default     = "pd-standard"
}

# Variables para el pool elástico de workers
variable "worker_min_nodes" {
  description = "Mínimo de nodos para el pool de workers"
  type        = number
  default     = 0
}
variable "worker_max_nodes" {
  description = "Máximo de nodos para el pool de workers"
  type        = number
  default     = 5
}

provider "google" {
  credentials = file(var.credentials_file)
  project     = var.project_id
  region      = var.region
  zone        = var.zone
}

# Clúster zonal: usa únicamente la zona especificada
resource "google_container_cluster" "primary" {
  name                     = var.cluster_name
  location                 = var.zone
  remove_default_node_pool = true
  initial_node_count       = 1

  node_config {
    machine_type = var.machine_type
    oauth_scopes = ["https://www.googleapis.com/auth/cloud-platform"]
    disk_size_gb = var.boot_disk_size_gb
    disk_type    = var.disk_type
  }
}

# Pool de nodos “base” (estático) con autoscaling habilitado
resource "google_container_node_pool" "primary_nodes" {
  name     = "${var.cluster_name}-pool"
  cluster  = google_container_cluster.primary.name
  location = var.zone

  autoscaling {
    min_node_count = var.node_count
    max_node_count = var.node_count * 2
  }

  node_config {
    machine_type = var.machine_type
    oauth_scopes = ["https://www.googleapis.com/auth/cloud-platform"]
    disk_size_gb = var.boot_disk_size_gb
    disk_type    = var.disk_type
  }
}

# Pool de nodos “workers” (elástico, sólo cuando haya Sobel-jobs)
resource "google_container_node_pool" "worker_pool" {
  name                  = "${var.cluster_name}-workers"
  cluster               = google_container_cluster.primary.name
  location              = var.zone
  initial_node_count    = var.worker_min_nodes

  autoscaling {
    min_node_count = var.worker_min_nodes
    max_node_count = var.worker_max_nodes
  }

  node_config {
    machine_type = var.machine_type
    oauth_scopes = ["https://www.googleapis.com/auth/cloud-platform"]

    labels = { role = "worker" }
    taint {
      key    = "role"
      value  = "worker"
      effect = "NO_SCHEDULE"
    }

    disk_size_gb = var.boot_disk_size_gb
    disk_type    = var.disk_type
  }
}