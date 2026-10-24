package cl.duoc.dsy1107.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RabbitResourceResponseDTO {
    private String resourceType;
    private String resourceName;
    private String operation;
    private String status;
    private String message;
    private Map<String, Object> details;
    private Instant timestamp;
}
