package master.master.listeners;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import master.master.dtos.ParteProcesadaDTO;
import master.master.models.ImageMetadata;
import master.master.services.ImageProcessingService;
import master.master.services.MetadataPersistenceService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class ImagenProcesadaListener {

    private final Map<String, List<ParteProcesadaDTO>> buffer = new ConcurrentHashMap<>();

    private final MetadataPersistenceService metadataPersistenceService;
    private final ImageProcessingService imageProcessingService;

    @RabbitListener(queues = "image.processed.queue")
    public void recibirParteProcesada(String mensajeJson) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        ParteProcesadaDTO dto = mapper.readValue(mensajeJson, ParteProcesadaDTO.class);

        String imagenId = dto.getId();

        ImageMetadata imageMetadata = metadataPersistenceService.getMetadata(imagenId);

        buffer.putIfAbsent(imagenId, new ArrayList<>());
        buffer.get(imagenId).add(dto);

        System.out.println("Recibida parte #" + dto.getIndice());

        ImageMetadata imageMetadataUpdate = ImageMetadata.builder()
                .id(imagenId)
                .nombreImagen(imageMetadata.nombreImagen())
                .partes(imageMetadata.partes())
                .partesProcesadas(imageMetadata.partesProcesadas() + 1).build();

        metadataPersistenceService.updateMetadata(imagenId, imageMetadataUpdate);

        if (metadataPersistenceService.isComplete(imagenId)) { // tiene que venir de redis
            System.out.println("Se recibieron todas las partes, uniendo imagen...");

            // Ordenar por índice
            List<ParteProcesadaDTO> partes = buffer.remove(imagenId).stream()
                    .sorted(Comparator.comparingInt(ParteProcesadaDTO::getIndice))
                    .toList();

            List<byte[]> partesBytes = new ArrayList<>();
            for (ParteProcesadaDTO parte : partes) {
                partesBytes.add(Base64.getDecoder().decode(parte.getParteProcesada()));
            }

            String extension = imageMetadata.contentType().split("/")[1];

            byte[] imagenUnida = imageProcessingService.unirImagenes(partesBytes, extension);

            Files.write(Path.of(String.format("/app/output/%s.%s",imagenId, extension)), imagenUnida);

            System.out.println("Imagen reconstruida y guardada");
            metadataPersistenceService.deleteMetadata(imagenId);
        }
    }
}