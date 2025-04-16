package master.master.services.impl;

import master.master.services.ImageProcessingService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class ImageProcessingServiceImpl implements ImageProcessingService {

    @Override
    public List<byte[]> dividirImagen(MultipartFile image, int partes) throws IOException {
        BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(image.getBytes()));

        int width = originalImage.getWidth();
        int height = originalImage.getHeight();

        int rows = (int) Math.sqrt(partes);
        int cols = (partes + rows - 1) / rows;

        int chunkWidth = width / cols;
        int chunkHeight = height / rows;

        List<byte[]> partesImagenes = new ArrayList<>();
        int count = 0;

        String format = image.getContentType().split("/")[1];

        for (int y = 0; y < rows && count < partes; y++) {
            for (int x = 0; x < cols && count < partes; x++) {
                int actualWidth = (x == cols - 1) ? width - x * chunkWidth : chunkWidth;
                int actualHeight = (y == rows - 1) ? height - y * chunkHeight : chunkHeight;

                BufferedImage subImage = originalImage.getSubimage(
                        x * chunkWidth, y * chunkHeight,
                        actualWidth, actualHeight
                );

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(subImage, format, baos);
                partesImagenes.add(baos.toByteArray());
                count++;
            }
        }

        return partesImagenes;
    }

    @Override
    public byte[] unirImagenes(List<byte[]> partes, String format) throws IOException {
        List<BufferedImage> imagenesLista = new ArrayList<>();
        for (byte[] parte : partes) {
            imagenesLista.add(ImageIO.read(new ByteArrayInputStream(parte)));
        }

        int n = partes.size();
        int rows = (int) Math.sqrt(n);
        int cols = (n + rows - 1) / rows;

        int chunkWidth = imagenesLista.get(0).getWidth();
        int chunkHeight = imagenesLista.get(0).getHeight();

        int totalWidth = chunkWidth * cols;
        int totalHeight = chunkHeight * rows;

        BufferedImage imagenFinal = new BufferedImage(totalWidth, totalHeight, BufferedImage.TYPE_INT_RGB);

        int index = 0;
        for (int y = 0; y < rows && index < imagenesLista.size(); y++) {
            for (int x = 0; x < cols && index < imagenesLista.size(); x++) {
                imagenFinal.createGraphics().drawImage(imagenesLista.get(index++), x * chunkWidth, y * chunkHeight, null);
            }
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(imagenFinal, format, baos);
        return baos.toByteArray();
    }
}
