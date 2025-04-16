package master.master.controllers;

import lombok.RequiredArgsConstructor;
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

    private final ImageProcessingService imageProcessingService;

    @PostMapping("/processImage")
    public ResponseEntity<byte[]> processImage(@RequestParam("image") MultipartFile imageFile) throws IOException {
        int partes = 4;

        List<byte[]> partesImagen = imageProcessingService.dividirImagen(imageFile, partes);

        String format = imageFile.getContentType().split("/")[1];
        byte[] imagenFinal = imageProcessingService.unirImagenes(partesImagen, format);

        return new ResponseEntity<>(imagenFinal, getHttpHeaders(imageFile, imagenFinal.length), HttpStatus.OK);
    }

    private HttpHeaders getHttpHeaders(MultipartFile imageFile, int contentLength) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf(imageFile.getContentType()));
        headers.setContentLength(contentLength);
        headers.setContentDisposition(ContentDisposition.inline().filename("processed_" + imageFile.getOriginalFilename()).build());
        return headers;
    }
}
