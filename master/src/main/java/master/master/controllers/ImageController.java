package master.master.controllers;

import lombok.RequiredArgsConstructor;
import master.master.services.ImageFecadeService;
import org.apache.catalina.connector.Request;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/task")
public class ImageController {

    private final ImageFecadeService imageFacadeService;

    @PostMapping("/processAndPublish")
    public ResponseEntity<String> processAndPublish(@RequestParam("image") MultipartFile image, @RequestParam("partes") int partes) throws IOException {
        String id = String.format("%s_%s", UUID.randomUUID(), image.getOriginalFilename());
        imageFacadeService.procesarYPublicar(image, partes, id);
        return ResponseEntity.ok("Imagen procesada y publicada en la cola.");
    }
}
