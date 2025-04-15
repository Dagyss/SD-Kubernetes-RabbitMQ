package master.master.services;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface TaskService {

    List<MultipartFile> partirImagen(MultipartFile file);
    MultipartFile unirImagenes(List<MultipartFile> imagenes);

}
