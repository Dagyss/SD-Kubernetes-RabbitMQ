terraform {
  required_version = ">= 0.13"
  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 4.0"
    }
  }
}

# Variables
variable "project_id" {
  description = "ID del proyecto de GCP"
  type        = string
}

variable "region" {
  description = "Región de GCP"
  type        = string
  default     = "us-central1"
}

variable "zone" {
  description = "Zona de GCP para clúster zonal"
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
  description = "Número de nodos mínimo para pools estáticos"
  type        = number
  default     = 1
}

variable "boot_disk_size_gb" {
  description = "Tamaño del disco de arranque en GB"
  type        = number
  default     = 20
}

variable "disk_type" {
  description = "Tipo de disco de arranque"
  type        = string
  default     = "pd-standard"
}

variable "worker_min_nodes" {
  description = "Mínimo de nodos para el pool dinámico de workers"
  type        = number
  default     = 0
}

variable "worker_max_nodes" {
  description = "Máximo de nodos para el pool dinámico de workers"
  type        = number
  default     = 5
}

variable "bucket_name" {
  description = "Nombre del bucket de GCS para fragmentos de imagen"
  type        = string
}

variable "network" {
  description = "VPC donde desplegar clúster y VMs"
  type        = string
  default     = "default"
}

variable "subnetwork" {
  description = "Subred dentro de la VPC"
  type        = string
  default     = "default"
}

variable "infra_node_tag" {
  description = "Etiqueta aplicada a nodos infra en GKE"
  type        = string
  default     = "gke-infra-pool"
}

# Provider
provider "google" {
  credentials = file(var.credentials_file)
  project     = var.project_id
  region      = var.region
  zone        = var.zone
}

# VPC and Subnet
resource "google_compute_network" "vpc" {
  name                    = var.network
  auto_create_subnetworks = false
}

resource "google_compute_subnetwork" "subnet" {
  name          = var.subnetwork
  ip_cidr_range = "10.0.0.0/16"
  region        = var.region
  network       = google_compute_network.vpc.id

  secondary_ip_range {
    range_name    = "gke-cluster-secondary-range"
    ip_cidr_range = "10.1.0.0/20"
  }

  secondary_ip_range {
    range_name    = "gke-services-secondary-range"
    ip_cidr_range = "10.2.0.0/20"
  }
}

# GKE Cluster
resource "google_container_cluster" "primary" {
  name                     = var.cluster_name
  location                 = var.zone
  remove_default_node_pool = true
  networking_mode          = "VPC_NATIVE"
  network                  = google_compute_network.vpc.name
  subnetwork               = google_compute_subnetwork.subnet.name
  initial_node_count       = 1

  ip_allocation_policy {
    cluster_secondary_range_name  = "gke-cluster-secondary-range"
    services_secondary_range_name = "gke-services-secondary-range"
  }
  
  node_config {
    machine_type = var.machine_type
    disk_size_gb = var.boot_disk_size_gb
    disk_type    = var.disk_type
    oauth_scopes = ["https://www.googleapis.com/auth/cloud-platform"]
  }
}

# Node pool infra
resource "google_container_node_pool" "infra" {
  name     = "${var.cluster_name}-infra"
  cluster  = google_container_cluster.primary.name
  location = var.zone
  node_config {
    machine_type = var.machine_type
    disk_size_gb  = var.boot_disk_size_gb
    disk_type     = var.disk_type
    labels        = { role = "infra" }
    tags          = [var.infra_node_tag]
  }
  autoscaling {
    min_node_count = var.node_count
    max_node_count = var.node_count * 2
  }
}

# Node pool apps
resource "google_container_node_pool" "apps" {
  name     = "${var.cluster_name}-apps"
  cluster  = google_container_cluster.primary.name
  location = var.zone
  node_config {
    machine_type = var.machine_type
    disk_size_gb  = var.boot_disk_size_gb
    disk_type     = var.disk_type
    labels        = { role = "app" }
  }
  autoscaling {
    min_node_count = var.node_count
    max_node_count = var.node_count * 2
  }
}

# Sobel Worker VMs
resource "google_service_account" "sobel_worker" {
  project    = var.project_id
  account_id = "sobel-worker-sa"
}


resource "google_compute_instance_template" "sobel_worker" {
  name_prefix = "sobel-worker-"
  tags        = ["sobel-worker"]

  service_account {
    email  = google_service_account.sobel_worker.email
    scopes = ["https://www.googleapis.com/auth/cloud-platform"]
  }

  disk {
    boot         = true
    auto_delete  = true
    source_image = "projects/debian-cloud/global/images/family/debian-12"
    disk_size_gb = var.boot_disk_size_gb
    disk_type    = var.disk_type
  }

  machine_type = var.machine_type
  metadata_startup_script = <<-EOF
    #!/bin/bash
    sudo apt-get update && sudo apt-get install -y docker.io netcat-traditional
    until nc -z 10.0.0.7 5672; do
      sleep 5
    done
    sudo docker run --rm   -e RABBITMQ_HOST=10.0.0.7   -e RABBITMQ_USER=user   -e RABBITMQ_PASS=password   -e RABBITMQ_PORT=5672   dagyss/worker:latest
  EOF
  network_interface {
    network    = google_compute_network.vpc.id
    subnetwork = google_compute_subnetwork.subnet.id
    access_config {}
  }
}

resource "google_compute_region_instance_group_manager" "sobel_workers" {
  name               = "sobel-workers-rigm"
  region             = var.region
  base_instance_name = "sobel-worker"
  version {
    instance_template = google_compute_instance_template.sobel_worker.self_link
  }
  target_size        = var.worker_min_nodes
}

resource "google_compute_region_autoscaler" "sobel_workers_autoscaler" {
  name   = "sobel-workers-autoscaler"
  region = var.region

  target = google_compute_region_instance_group_manager.sobel_workers.id
  autoscaling_policy {
    min_replicas = var.worker_min_nodes
    max_replicas = var.worker_max_nodes
    cpu_utilization {
      target = 0.6
    }
  }
}

# GCS bucket data source
data "google_storage_bucket" "image_bucket" {
  name = var.bucket_name
}

resource "google_compute_firewall" "allow-ssh" {
  name    = "allow-ssh"
  network = google_compute_network.vpc.name

  allow {
    protocol = "tcp"
    ports    = ["22"]
  }

  source_ranges = ["0.0.0.0/0"]
}

resource "google_compute_firewall" "allow-http" {
  name    = "allow-http"
  network = google_compute_network.vpc.name

  allow {
    protocol = "tcp"
    ports    = ["80"]
  }

  source_ranges = ["0.0.0.0/0"]
}

resource "google_compute_firewall" "allow-https" {
  name    = "allow-https"
  network = google_compute_network.vpc.name

  allow {
    protocol = "tcp"
    ports    = ["443"]
  }

  source_ranges = ["0.0.0.0/0"]
}

resource "tls_private_key" "ssh_key" {
  algorithm = "RSA"
  rsa_bits  = 4096
}
resource "local_file" "ssh_private_key_pem" {
  content         = tls_private_key.ssh_key.private_key_pem
  filename        = ".ssh/google_compute_engine"
  file_permission = "0600"
}


resource "google_compute_firewall" "allow-rabbitmq1" {
  name    = "allow-rabbitmq1"
  network = google_compute_network.vpc.name

  allow {
    protocol = "tcp"
    ports    = ["5672", "15672"]
  }

  source_ranges = ["0.0.0.0/0"]
}
resource "google_compute_firewall" "allow-redis" {
  name    = "allow-redis"
  network = google_compute_network.vpc.name


  allow {
    protocol = "tcp"
    ports    = ["6379", "8001"]
  }

  source_ranges = ["0.0.0.0/0"]
}

resource "google_storage_bucket_iam_binding" "reconstructor_storage_admin" {
  bucket = data.google_storage_bucket.image_bucket.name
  role   = "roles/storage.objectAdmin"
  members = [
    "serviceAccount:reconstructor-sa@gentle-oxygen-457917-c0.iam.gserviceaccount.com",
  ]
}