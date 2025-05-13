# SD-Kubernetes-RabbitMQ

## Proyecto: Procesamiento de Imágenes Distribuido con Sobel
---

## 📦 Releases

A continuación encontrarás nuestras releases principales. ¡Haz clic en cada una para explorar el código y la documentación detallada!

* **Hit #1: Operador de Sobel**

  * **Tag**: [`hit-1`](https://github.com/Dagyss/SD-Kubernetes-RabbitMQ/releases/tag/hit1)
  * **Descripción**: Implementa un servicio maestro que divide la imagen en *N* segmentos y los envía en paralelo a *workers* vía RabbitMQ. Cada worker aplica el filtro de Sobel concurrentemente, y el maestro unifica los resultados en la imagen final.

* **Hit #2: Offloading en la nube**

  * **Tag**: [`hit-2`](https://github.com/Dagyss/SD-Kubernetes-RabbitMQ/releases/tag/hit2)
  * **Descripción**: Extiende la arquitectura con despliegue híbrido y escalable usando Terraform y Kubernetes. Permite crear y destruir *workers* en la nube (GCP/AWS/Azure) según la demanda, manteniendo una base elástica.

* **Hit #3: Sobel contenerizado asincrónico y escalable**

  * **Tag**:  [`hit-3`](https://github.com/Dagyss/SD-Kubernetes-RabbitMQ/releases/tag/hit3)

  * **Descripción breve: Despliega una infraestructura Kubernetes en GKE con Terraform, donde un cluster principal coordina servicios críticos (RabbitMQ, Redis) y aplicaciones (API REST, reconstructor) en nodegroups, mientras que máquinas virtuales externas escalan los workers asíncronos.

---

##  Cómo empezar con cada release

### Hit #1: Operador de Sobel

```bash
# 1. Descargar el source code del hit 1

# 2. Compilar sin tests
mvn clean package -DskipTests

# 3. Levantar contenedores Docker
cd ..
docker-compose up --scale worker=<número_de_workers>

# 4. Enviar imagen para procesar
#    POST http://localhost:8080/task/processAndPublish
#    Parámetro: partes
#    Body: form-data -> image (archivo)
```

### Hit #2: Offloading en la nube

```bash
# 1. Preparar bucket en GCP y credenciales
#    Crear bucket único y actualizar variable en k8s-manifests/worker-deployment.yaml, k8s-manifests/reconstructor-deployment.yaml y terraform/terraform.tfvars

# 2. Terraform
cd terraform
cp path/to/terraform-sa-keys.json .
terraform init
terraform plan -out plan.tfplan
terraform apply "plan.tfplan"

# 3. Configurar kubectl y desplegar manifiestos
gcloud container clusters get-credentials gke-cluster
kubectl apply -f ../k8s-manifests/

# 4. Consumir API REST
#    POST /processAndPublish -> image, partes
#    GET /task/status/{id}
#    GET /task/images/{id}
```

### Hit #3: Sobel contenerizado asincrónico y escalable

```bash
   # 1.Push al repositorio
   #    Desencadena los GitHub Actions de los pipelines configurados.
   # 2.Verificación de despliegue
   #    Comprobar el estado y logs en la sección Actions de GitHub.
   # 3.Prueba de la API REST
   #     Obtener la IP del servicio maestro:

        kubectl get svc master -n apps -o jsonpath='{.status.loadBalancer.ingress[0].ip}'

   #     Ejecutar:
   #         POST /processAndPublish (form-data: image y partes).
   #         GET /task/status/{id}.
   #         GET /task/images/{id}.



```

---

