from flask import Flask, request
from PIL import Image
import numpy as np
import time
import os

app = Flask(__name__)

# Sobel kernels
Kx = np.array([[ -1,  0,  1],
               [ -2,  0,  2],
               [ -1,  0,  1]])

Ky = np.array([[ -1, -2, -1],
               [  0,  0,  0],
               [  1,  2,  1]])

def apply_sobel(image_path):
    img = Image.open(image_path).convert("L")  # Gris
    arr = np.array(img, dtype=np.float32)

    gx = np.zeros_like(arr)
    gy = np.zeros_like(arr)

    for i in range(1, arr.shape[0] - 1):
        for j in range(1, arr.shape[1] - 1):
            region = arr[i-1:i+2, j-1:j+2]
            gx[i, j] = np.sum(Kx * region)
            gy[i, j] = np.sum(Ky * region)

    # Magnitud del gradiente
    sobel = np.hypot(gx, gy)
    sobel = (sobel / sobel.max()) * 255.0
    sobel = sobel.astype(np.uint8)

    result = Image.fromarray(sobel)
    output_path = image_path.replace(".png", "_sobel.png")
    result.save(output_path)

    return output_path

@app.route("/process", methods=["POST"])
def process():
    data = request.get_json()
    chunk_path = data["chunk_path"]

    print(f"Procesando {chunk_path} con Sobel...")
    output = apply_sobel(chunk_path)
    return f"Chunk procesado y guardado como {os.path.basename(output)}"

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000)