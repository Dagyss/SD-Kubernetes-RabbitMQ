package master.master.services;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface ImageProcessingService {

    List<byte[]> dividirImagen(MultipartFile image, int partes) throws IOException;

    byte[] unirImagenes(List<byte[]> imagenes, String format) throws IOException;

}
