from flask import Flask, request
from PIL import Image
import time

app = Flask(name)

@app.route("/process", methods=["POST"])
def process():
    data = request.get_json()
    chunk_path = data["chunk_path"]

    print(f"Procesando {chunk_path}...")
    time.sleep(1)  # Simular procesamiento
    return f"Procesado {chunk_path}"

if name == "main":
    app.run(host="0.0.0.0", port=5000)