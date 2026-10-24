package cl.duoc.dsy1107.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketEventDTO {
    private String messageId;
    private Long pedidoId;

    @NotBlank(message = "El cliente asociado al ticket no puede estar vacío")
    private String cliente;

    @NotBlank(message = "El área responsable del ticket es obligatoria")
    private String areaResponsable;

    @NotBlank(message = "La prioridad del ticket es obligatoria")
    @Pattern(regexp = "^(ALTA|MEDIA|BAJA|CRITICA)$", message = "La prioridad debe ser ALTA, MEDIA, BAJA o CRITICA")
    private String prioridad;

    @NotBlank(message = "La descripción del ticket no puede estar vacía")
    private String descripcion;

    private boolean simularErrorRecuperable;
    private boolean simularErrorNoRecuperable;
    private LocalDateTime timestamp;
}
