package master.master.services.impl;

import lombok.RequiredArgsConstructor;
import master.master.models.ImageMetadata;
import master.master.services.MetadataPersistenceService;
import master.master.services.RedisService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MetadataPersistenceServiceImpl implements MetadataPersistenceService {

    private final RedisService redisService;
    @Override
    public void persistMetadata(ImageMetadata metadata) {
        redisService.guardarMetaData(metadata.id(), metadata);
    }

    @Override
    public ImageMetadata getMetadata(String id) {
        return redisService.obtenerMetaData(id);
    }

    @Override
    public void deleteMetadata(String id) {
        redisService.eliminarMetaData(id);
    }

    @Override
    public void updateMetadata(String clave, ImageMetadata metadata) {
        redisService.actualizarMetaData(clave, metadata);
    }

    @Override
    public boolean exists(String id){
        return redisService.obtenerMetaData(id) != null;
    }

    @Override
    public boolean isComplete(String id) {
        ImageMetadata metadata = redisService.obtenerMetaData(id);
        return metadata.partes() == metadata.partesProcesadas();
    }
}
