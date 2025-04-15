package master.master.services.impl;

import master.master.services.TaskService;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public class TaskServiceImpl implements TaskService {
    @Override
    public List<MultipartFile> partirImagen(MultipartFile file) {

        return List.of();
    }

    @Override
    public MultipartFile unirImagenes(List<MultipartFile> imagenes) {
        return null;
    }
}
