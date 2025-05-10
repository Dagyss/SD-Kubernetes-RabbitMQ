package master.master.feignClients;

import master.master.models.ImageMetadata;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "${metadata.service.name:reconstructorService}",
        path = "/api/metadata"
)
public interface MetadataClient {

    @PostMapping(
            value    = "/guardar",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    void guardarMetaData(@RequestBody ImageMetadata metadata);
}
