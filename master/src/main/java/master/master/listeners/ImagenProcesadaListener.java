package master.master.listeners;

import com.fasterxml.jackson.databind.ObjectMapper;
import master.master.dtos.ParteProcesadaDTO;
import master.master.services.ImageProcessingService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ImagenProcesadaListener {

    private final Map<String, List<ParteProcesadaDTO>> buffer = new ConcurrentHashMap<>();
    private final int TOTAL_PARTES = 4; // O lo que uses dinámicamente

    @Autowired
    private ImageProcessingService imageProcessingService;

    @RabbitListener(queues = "image.processed.queue")
    public void recibirParteProcesada(String mensajeJson) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        ParteProcesadaDTO dto = mapper.readValue(mensajeJson, ParteProcesadaDTO.class);

        String imagenId = "default";

        buffer.putIfAbsent(imagenId, new ArrayList<>());
        buffer.get(imagenId).add(dto);

        System.out.println("Recibida parte #" + dto.getIndice());

        if (buffer.get(imagenId).size() == TOTAL_PARTES) {
            System.out.println("Se recibieron todas las partes, uniendo imagen...");

            // Ordenar por índice
            List<ParteProcesadaDTO> partes = buffer.remove(imagenId).stream()
                    .sorted(Comparator.comparingInt(ParteProcesadaDTO::getIndice))
                    .toList();

            List<byte[]> partesBytes = new ArrayList<>();
            for (ParteProcesadaDTO parte : partes) {
                partesBytes.add(Base64.getDecoder().decode(parte.getParteProcesada()));
            }

            byte[] imagenUnida = imageProcessingService.unirImagenes(partesBytes, "jpg");

            Files.write(Path.of("imagen_unida_sobel.jpg"), imagenUnida);
            System.out.println("Imagen reconstruida y guardada");
        }
    }
}