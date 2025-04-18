package master.master.services.impl;

import lombok.RequiredArgsConstructor;
import master.master.models.ImageMetadata;
import master.master.services.RedisService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RedisServiceImpl implements RedisService {

    private final RedisTemplate<String, ImageMetadata> redisTemplate;

    @Override
    public void guardarMetaData(String clave, ImageMetadata valor) {
        redisTemplate.opsForValue().set(clave, valor);
    }

    @Override
    public ImageMetadata obtenerMetaData(String clave) {
        if (redisTemplate.hasKey(clave)) {
            return redisTemplate.opsForValue().get(clave);
        }
        return null;
    }

    @Override
    public void eliminarMetaData(String clave) {
        if (redisTemplate.hasKey(clave)) {
            redisTemplate.delete(clave);
        }
    }

    @Override
    public void actualizarMetaData(String clave, ImageMetadata valor) {
        redisTemplate.opsForValue().set(clave, valor);
    }
}
