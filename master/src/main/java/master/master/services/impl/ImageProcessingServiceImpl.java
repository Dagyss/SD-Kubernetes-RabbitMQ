package master.master.services.impl;

import master.master.services.ImageProcessingService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ImageProcessingServiceImpl implements ImageProcessingService {

    @Override
    public List<byte[]> dividirImagen(MultipartFile image, int partes) throws IOException {
        if (partes <= 0) {
            throw new IllegalArgumentException("El número de partes debe ser mayor o igual a 1");
        }

        // Leer imagen original
        BufferedImage original = ImageIO.read(new ByteArrayInputStream(image.getBytes()));
        int width  = original.getWidth();
        int height = original.getHeight();
        int type   = original.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : original.getType();

        // Cálculo de filas y columnas usando ceil
        int rows = (int) Math.ceil(Math.sqrt(partes));
        int cols = (int) Math.ceil((double) partes / rows);

        // Tamaño base de cada fragmento
        int baseW = width  / cols;
        int baseH = height / rows;

        // Determinar formato (extensión) de la imagen
        String format = Optional.ofNullable(image.getOriginalFilename())
                .filter(fn -> fn.contains("."))
                .map(fn -> fn.substring(fn.lastIndexOf('.') + 1))
                .orElse("png");

        List<byte[]> partesImagenes = new ArrayList<>(partes);
        int count = 0;

        // Dividir la imagen
        for (int ry = 0; ry < rows && count < partes; ry++) {
            for (int cx = 0; cx < cols && count < partes; cx++) {
                // Ajuste en bordes
                int w = (cx == cols - 1) ? width - cx * baseW : baseW;
                int h = (ry == rows - 1) ? height - ry * baseH : baseH;

                BufferedImage subImage = new BufferedImage(w, h, type);
                Graphics2D g = subImage.createGraphics();
                g.drawImage(original,
                        0, 0, w, h,
                        cx * baseW, ry * baseH,
                        cx * baseW + w, ry * baseH + h,
                        null);
                g.dispose();

                try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                    ImageIO.write(subImage, format, baos);
                    partesImagenes.add(baos.toByteArray());
                }
                count++;
            }
        }

        return partesImagenes;
    }

    @Override
    public byte[] unirImagenes(List<byte[]> partes, String format) throws IOException {
        if (partes == null || partes.isEmpty()) {
            throw new IllegalArgumentException("La lista de partes no puede estar vacía");
        }

        // Reconstruir BufferedImages de los bytes
        List<BufferedImage> imagenesLista = new ArrayList<>(partes.size());
        for (byte[] parte : partes) {
            imagenesLista.add(ImageIO.read(new ByteArrayInputStream(parte)));
        }

        int n    = imagenesLista.size();
        int rows = (int) Math.ceil(Math.sqrt(n));
        int cols = (int) Math.ceil((double) n / rows);

        // Calcular anchos/altos máximos por columna/fila
        int[] colWidths = new int[cols];
        int[] rowHeights = new int[rows];
        for (int i = 0; i < n; i++) {
            BufferedImage img = imagenesLista.get(i);
            int r = i / cols;
            int c = i % cols;
            colWidths[c] = Math.max(colWidths[c], img.getWidth());
            rowHeights[r] = Math.max(rowHeights[r], img.getHeight());
        }

        // Tamaño total del lienzo
        int totalW = 0;
        for (int w : colWidths) totalW += w;
        int totalH = 0;
        for (int h : rowHeights) totalH += h;

        // Crear imagen final
        BufferedImage imagenFinal = new BufferedImage(totalW, totalH, imagenesLista.get(0).getType());
        Graphics2D g = imagenFinal.createGraphics();

        // Dibujar cada fragmento en su posición
        int yOff = 0;
        int index = 0;
        for (int ry = 0; ry < rows; ry++) {
            int xOff = 0;
            for (int cx = 0; cx < cols && index < n; cx++) {
                BufferedImage img = imagenesLista.get(index++);
                g.drawImage(img, xOff, yOff, null);
                xOff += colWidths[cx];
            }
            yOff += rowHeights[ry];
        }
        g.dispose();

        // Escribir imagen final a bytes
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(imagenFinal, format, baos);
            return baos.toByteArray();
        }
    }
}
