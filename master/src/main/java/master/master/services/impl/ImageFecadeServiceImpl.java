package master.master.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import master.master.services.ImageFecadeService;
import master.master.services.ImageProcessingService;
import master.master.services.RabbitPublisherService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ImageFecadeServiceImpl implements ImageFecadeService {

    private final ImageProcessingService imageProcessingService;
    private final RabbitPublisherService rabbitPublisherService;

    @Override
    public void procesarYPublicar(MultipartFile imageFile, int partes) throws IOException {
        if (imageFile == null || imageFile.isEmpty()) {
            log.warn("Imagen vacía o nula. No se procesará.");
            return;
        }

        log.info("Iniciando procesamiento y publicación de imagen en {} partes", partes);

        List<byte[]> chunks = imageProcessingService.dividirImagen(imageFile, partes);
        rabbitPublisherService.publicarPartes(chunks);

        log.info("Se publicaron {} partes de la imagen en la cola RabbitMQ.", chunks.size());
    }
}
