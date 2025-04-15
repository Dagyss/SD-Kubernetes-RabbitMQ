import os
import pika
import uuid
import numpy as np
import cv2
import time
import socket

# Leer configuración desde variables de entorno
rabbitmq_host = os.getenv('RABBITMQ_HOST', 'rabbitmq')
rabbitmq_user = os.getenv('RABBITMQ_USER', 'guest')
rabbitmq_pass = os.getenv('RABBITMQ_PASS', 'guest')

# Configurar credenciales
credentials = pika.PlainCredentials(rabbitmq_user, rabbitmq_pass)
parameters = pika.ConnectionParameters(host=rabbitmq_host, credentials=credentials)

# Intentar conexión con reintentos
connection = None
for attempt in range(10):
    try:
        print(f"Intentando conectar a RabbitMQ ({rabbitmq_host})... intento {attempt + 1}")
        connection = pika.BlockingConnection(parameters)
        print("✅ Conexión exitosa a RabbitMQ.")
        break
    except (pika.exceptions.AMQPConnectionError, socket.gaierror) as e:
        print(f"❌ Fallo al conectar: {e}")
        time.sleep(5)

if connection is None:
    print("❌ No se pudo establecer conexión con RabbitMQ. Terminando proceso.")
    exit(1)

channel = connection.channel()
channel.queue_declare(queue='sobel-tasks', durable=True)
channel.queue_declare(queue='sobel-results', durable=True)

def on_request(ch, method, properties, body):
    correlation_id = properties.correlation_id

    # Convertir el fragmento de bytes a una imagen
    nparr = np.frombuffer(body, np.uint8)
    img = cv2.imdecode(nparr, cv2.IMREAD_GRAYSCALE)

    # Aplicar filtro Sobel
    sobel_img = cv2.Sobel(img, cv2.CV_64F, 1, 1, ksize=3)

    # Convertir imagen a bytes
    _, buffer = cv2.imencode('.jpg', sobel_img)
    processed_image = buffer.tobytes()

    ch.basic_publish(
        exchange='',
        routing_key='sobel-results',
        properties=pika.BasicProperties(
            reply_to=properties.reply_to,
            correlation_id=correlation_id
        ),
        body=processed_image
    )

    ch.basic_ack(delivery_tag=method.delivery_tag)

channel.basic_qos(prefetch_count=1)
channel.basic_consume(queue='sobel-tasks', on_message_callback=on_request)

print("🐍 Worker listo. Esperando solicitudes...")
channel.start_consuming()
