package master.master.controllers;

import lombok.RequiredArgsConstructor;
import master.master.services.ImageFecadeService;
import master.master.services.ImageProcessingService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/task")
public class ImageController {

    private final ImageFecadeService imageFacadeService;

    @PostMapping("/processAndPublish")
    public ResponseEntity<String> processAndPublish(@RequestParam("image") MultipartFile image, @RequestParam("partes") int partes) throws IOException {
        imageFacadeService.procesarYPublicar(image, partes);
        return ResponseEntity.ok("Imagen procesada y publicada en la cola.");
    }

    private HttpHeaders getHttpHeaders(MultipartFile imageFile, int contentLength) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf(imageFile.getContentType()));
        headers.setContentLength(contentLength);
        headers.setContentDisposition(ContentDisposition.inline().filename("processed_" + imageFile.getOriginalFilename()).build());
        return headers;
    }
}
