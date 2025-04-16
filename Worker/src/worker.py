import pika
import base64
import cv2
import numpy as np
import json

# Conexión a RabbitMQ con autenticación
connection = pika.BlockingConnection(
    pika.ConnectionParameters(
        host='localhost',
        credentials=pika.PlainCredentials('user', 'password')  # Credenciales de RabbitMQ
    )
)
channel = connection.channel()

# Declaración de la cola de entrada (debe coincidir con la usada por Spring)
channel.queue_declare(queue='image.parts.queue')

# Declaración de la cola de salida (donde se publican partes procesadas)
channel.queue_declare(queue='image.processed.queue')


def aplicar_sobel(image_bytes):
    # Convertir bytes base64 a imagen OpenCV
    nparr = np.frombuffer(image_bytes, np.uint8)
    img = cv2.imdecode(nparr, cv2.IMREAD_GRAYSCALE)

    # Aplicar filtro de Sobel
    sobelx = cv2.Sobel(img, cv2.CV_64F, 1, 0, ksize=3)
    sobely = cv2.Sobel(img, cv2.CV_64F, 0, 1, ksize=3)
    sobel = cv2.magnitude(sobelx, sobely)
    sobel = np.uint8(np.clip(sobel, 0, 255))

    # Codificar imagen nuevamente a JPEG
    _, buffer = cv2.imencode('.jpg', sobel)
    return buffer.tobytes()


def callback(ch, method, properties, body):
    mensaje = json.loads(body)
    indice = mensaje['indice']
    parte_base64 = mensaje['parte']
    parte_bytes = base64.b64decode(parte_base64)

    procesada_bytes = aplicar_sobel(parte_bytes)
    procesada_base64 = base64.b64encode(procesada_bytes).decode('utf-8')

    resultado = {
        'indice': indice,
        'parteProcesada': procesada_base64
    }

    channel.basic_publish(
        exchange='',
        routing_key='image.processed.queue',
        body=json.dumps(resultado)
    )

    print(f'[x] Procesado chunk #{indice}')


channel.basic_consume(queue='image.parts.queue', on_message_callback=callback, auto_ack=True)
print('[*] Esperando partes de imagen. Para salir, presioná CTRL+C')
channel.start_consuming()
