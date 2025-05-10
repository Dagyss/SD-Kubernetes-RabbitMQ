package reconstructor.reconstructorService.services;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface ImageProcessingService {

    byte[] unirImagenes(List<byte[]> imagenes, String format) throws IOException;

}
