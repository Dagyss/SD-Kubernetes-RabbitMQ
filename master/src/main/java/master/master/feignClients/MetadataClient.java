package master.master.feignClients;

import master.master.models.ImageMetadata;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "metadata-service", path = "/api/metadata")
public interface MetadataClient {

    @PostMapping("/guardar")
    void guardarMetaData(@RequestBody ImageMetadata metadata);

}
