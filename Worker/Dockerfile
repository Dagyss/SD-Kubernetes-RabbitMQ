# Dockerfile para Python Worker
FROM python:3.9-slim

# Instalar dependencias del sistema necesarias para OpenCV
RUN apt-get update && apt-get install -y \
    libgl1-mesa-glx \
    libglib2.0-0 \
    && rm -rf /var/lib/apt/lists/*

# Establecer directorio de trabajo
WORKDIR /app

# Copiar e instalar dependencias Python
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copiar el código fuente del worker
COPY ./src/worker.py .

# Puerto expuesto (opcional, no necesario si solo consume de RabbitMQ)
EXPOSE 5672

# Comando para ejecutar el worker
CMD ["python", "worker.py"]
