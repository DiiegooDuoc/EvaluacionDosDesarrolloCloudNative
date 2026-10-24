package cl.duoc.dsy1107.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateQueueRequestDTO {

    @NotBlank(message = "El nombre de la cola no puede estar vacío")
    @Size(min = 3, max = 120, message = "El nombre de la cola debe tener entre 3 y 120 caracteres")
    @Pattern(
        regexp = "^[a-zA-Z0-9._-]+$",
        message = "El nombre de la cola solo puede contener letras, números, puntos, guiones y guiones bajos (sin espacios)"
    )
    private String name;

    private boolean durable = true;
    private boolean exclusive = false;
    private boolean autoDelete = false;

    @Pattern(
        regexp = "^$|^[a-zA-Z0-9._-]+$",
        message = "El nombre del Dead Letter Exchange solo puede contener caracteres alfanuméricos, puntos o guiones"
    )
    private String deadLetterExchange;

    @Pattern(
        regexp = "^$|^[a-zA-Z0-9._*#-]+$",
        message = "El Dead Letter Routing Key contiene caracteres inválidos"
    )
    private String deadLetterRoutingKey;

    @Min(value = 1000, message = "El TTL mínimo permitido para mensajes es de 1000 ms (1 segundo)")
    @Max(value = 86400000, message = "El TTL máximo permitido es de 86400000 ms (24 horas)")
    private Integer messageTtlMs;
}
