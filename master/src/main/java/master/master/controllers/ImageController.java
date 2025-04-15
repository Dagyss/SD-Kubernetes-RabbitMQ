package master.master.controllers;

import lombok.RequiredArgsConstructor;
import master.master.services.TaskService;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
@RestController
@RequestMapping("/task")
public class ImageController {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @PostMapping("/processImage")
    public ResponseEntity<String> processImage(@RequestBody byte[] imageBytes) {
        int N = 4; // Número de fragmentos verticales
        List<byte[]> imageFragments = divideImage(imageBytes, N);
        Map<UUID, byte[]> fragmentMap = new ConcurrentHashMap<>();

        for (byte[] fragment : imageFragments) {
            UUID correlationId = UUID.randomUUID();
            fragmentMap.put(correlationId, fragment);

            Message message = MessageBuilder.withBody(fragment)
                    .setCorrelationId(correlationId.toString())
                    .setMessageId(correlationId.toString())
                    .build();

            rabbitTemplate.convertAndSend("sobel-tasks", message);
        }

        return ResponseEntity.ok("Image processing started");
    }

    private List<byte[]> divideImage(byte[] imageBytes, int numFragments) {
        List<byte[]> fragments = new ArrayList<>();

        try {
            BufferedImage original = ImageIO.read(new ByteArrayInputStream(imageBytes));
            int width = original.getWidth();
            int height = original.getHeight();
            int fragmentHeight = height / numFragments;

            for (int i = 0; i < numFragments; i++) {
                int y = i * fragmentHeight;
                int h = (i == numFragments - 1) ? (height - y) : fragmentHeight;

                BufferedImage subImage = original.getSubimage(0, y, width, h);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(subImage, "jpg", baos); // o "png" si preferís
                baos.flush();

                fragments.add(baos.toByteArray());
                baos.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return fragments;
    }

}
