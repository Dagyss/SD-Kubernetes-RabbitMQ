package master.master.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ParteProcesadaDTO {
    private int indice;
    private String parteProcesada; // base64
}