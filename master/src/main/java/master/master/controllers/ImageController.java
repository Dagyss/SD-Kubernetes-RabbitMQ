package master.master.controllers;

import lombok.RequiredArgsConstructor;
import master.master.configurations.ImageClientConfig;
import master.master.dtos.StatusResponse;
import master.master.feignClients.ImageClient;
import master.master.services.ImageFecadeService;
import org.apache.catalina.connector.Request;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/task")
public class ImageController {

    private final ImageFecadeService imageFacadeService;
    private final ImageClient imageClient;

    @PostMapping("/processAndPublish")
    public ResponseEntity<String> processAndPublish(@RequestParam("image") MultipartFile image, @RequestParam("partes") int partes) throws IOException {
        String id = String.format("%s_%s", UUID.randomUUID(), image.getOriginalFilename());
        imageFacadeService.procesarYPublicar(image, partes, id);
        return ResponseEntity.ok("Imagen procesada y publicada en la cola.");
    }

    @GetMapping("/images/{idImagen}")
    public ResponseEntity<ByteArrayResource> getImage(@PathVariable String idImagen) {
        byte[] data = imageClient.obtenerImagen(idImagen);

        String ext = idImagen.contains(".")
                ? idImagen.substring(idImagen.lastIndexOf('.') + 1).toLowerCase()
                : "";
        MediaType mediaType = switch (ext) {
            case "png"  -> MediaType.IMAGE_PNG;
            case "gif"  -> MediaType.IMAGE_GIF;
            case "jpg", "jpeg" -> MediaType.IMAGE_JPEG;
            default    -> MediaType.APPLICATION_OCTET_STREAM;
        };

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + idImagen + "\"")
                .contentLength(data.length)
                .contentType(mediaType)
                .body(new ByteArrayResource(data));
    }

    @GetMapping("/status/{idImagen}")
    public ResponseEntity<StatusResponse> getStatus(@PathVariable String idImagen) {
        StatusResponse status = imageClient.obtenerEstadoImagen(idImagen);
        return ResponseEntity.ok(status);
    }
}
