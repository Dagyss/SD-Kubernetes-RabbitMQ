import os
import pika
import base64
import cv2
import numpy as np
import json
import time
from google.cloud import storage

# Leer configuración desde variables de entorno
RABBITMQ_HOST = os.environ.get('RABBITMQ_HOST', 'localhost')
RABBITMQ_PORT = int(os.environ.get('RABBITMQ_PORT', 5672))
RABBITMQ_USER = os.environ.get('RABBITMQ_USER', 'user')
RABBITMQ_PASS = os.environ.get('RABBITMQ_PASS', 'password')
GCS_BUCKET_NAME = os.environ.get('GCS_BUCKET_NAME', 'bucket-imagenes-sobel')  

time.sleep(60)
# Conexión a RabbitMQ con autenticación
credentials = pika.PlainCredentials(RABBITMQ_USER, RABBITMQ_PASS)
connection = pika.BlockingConnection(
    pika.ConnectionParameters(
        host=RABBITMQ_HOST,
        port=RABBITMQ_PORT,
        credentials=credentials
    )
)
channel = connection.channel()
channel.basic_qos(prefetch_count=1)
channel.queue_declare(queue='image.parts.queue')
channel.queue_declare(queue='image.processed.queue', durable=True)

if not GCS_BUCKET_NAME:
    raise RuntimeError("Environment variable GCS_BUCKET_NAME is required")
storage_client = storage.Client()
bucket = storage_client.bucket(GCS_BUCKET_NAME)


def aplicar_sobel(image_bytes):
    nparr = np.frombuffer(image_bytes, np.uint8)
    img = cv2.imdecode(nparr, cv2.IMREAD_GRAYSCALE)
    sobelx = cv2.Sobel(img, cv2.CV_64F, 1, 0, ksize=3)
    sobely = cv2.Sobel(img, cv2.CV_64F, 0, 1, ksize=3)
    sobel = cv2.magnitude(sobelx, sobely)
    sobel = np.uint8(np.clip(sobel, 0, 255))
    _, buffer = cv2.imencode('.jpg', sobel)
    return buffer.tobytes()


def upload_to_gcs(image_id, index, image_bytes):
    blob_name = f"{image_id}_{index}.jpg"
    blob = bucket.blob(blob_name)
    blob.upload_from_string(image_bytes, content_type='image/jpeg')
    print(f"[x] Subido chunk procesado #{index} a bucket://{GCS_BUCKET_NAME}/{blob_name}")


def callback(ch, method, properties, body):
    try:
        mensaje = json.loads(body)
        imageId = mensaje['id']
        indice = mensaje['indice']
        parte_bytes = base64.b64decode(mensaje['parte'])
        procesada_bytes = aplicar_sobel(parte_bytes)
        upload_to_gcs(imageId, indice, procesada_bytes)
        resultado = {
            'id': imageId,
            'indice': indice,
            'status': 'PROCESADO'
        }
        channel.basic_publish(
            exchange='',
            routing_key='image.processed.queue',
            body=json.dumps(resultado)
        )
        print(f'[x] Procesado chunk #{indice} de imagen con id: {imageId}')
        
        ch.basic_ack(delivery_tag=method.delivery_tag)

    except Exception as e:
        print(f"[!] Error procesando chunk: {e}")
        

channel.basic_consume(
    queue='image.parts.queue',
    on_message_callback=callback,
    auto_ack=False
)
print('[*] Esperando partes de imagen. Para salir, presioná CTRL+C')
channel.start_consuming()
