package master.master.services;

import master.master.models.ImageMetadata;

public interface RedisService {
    void guardarMetaData(String clave, ImageMetadata valor);
    ImageMetadata obtenerMetaData(String clave);
    void eliminarMetaData(String clave);
    void actualizarMetaData(String clave, ImageMetadata valor);
}
