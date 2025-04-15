package master.master.services;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ImageAssembler {

    private Map<String, byte[]> fragments = new ConcurrentHashMap<>();
    private int totalFragments = 4;

    @RabbitListener(queues = "sobel-results")
    public void collectResult(Message message) {
        String correlationId = message.getMessageProperties().getCorrelationId();
        byte[] processedFragment = message.getBody();

        fragments.put(correlationId, processedFragment);

        if (fragments.size() == totalFragments) {
            // Recomponer la imagen
            byte[] fullImage = reassembleImage(fragments);
            // Retornar la imagen procesada al usuario o almacenarla

        }
    }

    private byte[] reassembleImage(Map<String, byte[]> fragments) {
        // Lógica para recomponer la imagen a partir de los fragmentos

        return new byte[0]; // Aquí se combinan los fragmentos
    }
}