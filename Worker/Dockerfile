FROM python:3.10-slim

WORKDIR /app

# Instalar dependencias necesarias
COPY requirements.txt .

RUN pip install --no-cache-dir -r requirements.txt

# Copiar el código
COPY ./src/worker.py .

# Exponer el puerto del Flask
EXPOSE 5000

# Correr la app
CMD ["python", "worker.py"]