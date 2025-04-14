from flask import Flask, request, send_file, Response
from PIL import Image
import numpy as np
import os
from io import BytesIO
import time

app = Flask(__name__)

# Sobel kernels
Kx = np.array([[-1, 0, 1],
               [-2, 0, 2],
               [-1, 0, 1]])

Ky = np.array([[-1, -2, -1],
               [0, 0, 0],
               [1, 2, 1]])

def apply_sobel(image_path):
    img = Image.open(image_path).convert("L")
    arr = np.array(img, dtype=np.float32)

    gx = np.zeros_like(arr)
    gy = np.zeros_like(arr)

    for i in range(1, arr.shape[0] - 1):
        for j in range(1, arr.shape[1] - 1):
            region = arr[i-1:i+2, j-1:j+2]
            gx[i, j] = np.sum(Kx * region)
            gy[i, j] = np.sum(Ky * region)

    sobel = np.hypot(gx, gy)
    sobel = (sobel / sobel.max()) * 255.0
    sobel = sobel.astype(np.uint8)

    result = Image.fromarray(sobel)
    return result

@app.route("/process", methods=["POST"])
def process():
    if 'image' not in request.files:
        return "No image part in the request", 400
    
    file = request.files['image']
    
    if file.filename == '':
        return "No selected file", 400

    temp_image_path = "temp_image.png"
    file.save(temp_image_path)
    
    try:
        start_time = time.time()

        processed_image = apply_sobel(temp_image_path)

        end_time = time.time()

        elapsed_time = end_time - start_time

        output_buffer = BytesIO()
        processed_image.save(output_buffer, format="PNG")
        output_buffer.seek(0)

        response = send_file(
            output_buffer,
            mimetype='image/png',
            as_attachment=True,
            download_name="processed_image.png"
        )
        response.headers["X-Processing-Time"] = f"{elapsed_time:.4f} seconds"
        return response
    finally:
        if os.path.exists(temp_image_path):
            os.remove(temp_image_path)

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000)