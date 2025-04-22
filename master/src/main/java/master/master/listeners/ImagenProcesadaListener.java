package master.master.listeners;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import master.master.dtos.ParteProcesadaDTO;
import master.master.models.ImageMetadata;
import master.master.services.ImageProcessingService;
import master.master.services.MetadataPersistenceService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImagenProcesadaListener {

    private final Map<String, List<ParteProcesadaDTO>> buffer = new ConcurrentHashMap<>();
    private final MetadataPersistenceService metadataPersistenceService;
    private final ImageProcessingService imageProcessingService;
    private final ObjectMapper mapper;  // inyectado por Spring

    @RabbitListener(queues = "image.processed.queue")
    public void recibirParteProcesada(String mensajeJson) {
        try {
            // 1) Deserializar DTO
            ParteProcesadaDTO dto = mapper.readValue(mensajeJson, ParteProcesadaDTO.class);
            if (dto == null || dto.getId() == null) {
                log.warn("Mensaje inválido o sin ID: {}", mensajeJson);
                return;
            }
            String imagenId = dto.getId();

            // 2) Recuperar metadata (puede venir de Redis u otro store)
            ImageMetadata imageMetadata = metadataPersistenceService.getMetadata(imagenId);
            if (imageMetadata == null) {
                log.warn("No existe metadata para imagenId={}", imagenId);
                return;
            }

            // 3) Guardar en buffer thread-safe
            buffer.computeIfAbsent(imagenId, k -> new ArrayList<>()).add(dto);
            log.info("Recibida parte #{} para imagenId={}", dto.getIndice(), imagenId);

            // 4) Actualizar contador de partes procesadas
            ImageMetadata updated = ImageMetadata.builder()
                    .id(imagenId)
                    .nombreImagen(imageMetadata.nombreImagen())
                    .contentType(imageMetadata.contentType())
                    .partes(imageMetadata.partes())
                    .partesProcesadas(imageMetadata.partesProcesadas() + 1)
                    .build();
            metadataPersistenceService.updateMetadata(imagenId, updated);

            // 5) Si ya están todas, unimos
            if (metadataPersistenceService.isComplete(imagenId)) {
                log.info("Todas las partes recibidas para id={}, uniendo imagen...", imagenId);

                List<ParteProcesadaDTO> partesDto = buffer.remove(imagenId);
                if (partesDto == null || partesDto.isEmpty()) {
                    log.error("Buffer vacío al tratar de unir imagen id={}", imagenId);
                    return;
                }

                // Ordenar y decodificar
                List<byte[]> partesBytes = partesDto.stream()
                        .sorted(Comparator.comparingInt(ParteProcesadaDTO::getIndice))
                        .map(p -> Base64.getDecoder().decode(p.getParteProcesada()))
                        .collect(Collectors.toList());

                // Extraer extensión (por ej. "png", "jpg")
                String extension = imageMetadata.contentType().split("/")[1];

                // Reconstruir imagen
                byte[] merged = imageProcessingService.unirImagenes(partesBytes, extension);

                // Asegurar carpeta de salida
                Path outPath = Path.of("/app/output", imagenId + "." + extension);
                Files.createDirectories(outPath.getParent());
                Files.write(outPath, merged);

                log.info("Imagen reconstruida y guardada en {}", outPath);
                metadataPersistenceService.deleteMetadata(imagenId);
            }

        } catch (IOException e) {
            log.error("Error I/O al procesar mensaje: {}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("Excepción inesperada en listener", e);
        }
    }
}