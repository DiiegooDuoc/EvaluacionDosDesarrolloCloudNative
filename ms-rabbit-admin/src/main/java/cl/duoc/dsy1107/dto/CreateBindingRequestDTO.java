package cl.duoc.dsy1107.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateBindingRequestDTO {

    @NotBlank(message = "El nombre de la cola de destino no puede estar vacío")
    @Size(min = 3, max = 120, message = "El nombre de la cola debe tener entre 3 y 120 caracteres")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "El nombre de la cola tiene un formato inválido")
    private String queueName;

    @NotBlank(message = "El nombre del exchange de origen no puede estar vacío")
    @Size(min = 3, max = 120, message = "El nombre del exchange debe tener entre 3 y 120 caracteres")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "El nombre del exchange tiene un formato inválido")
    private String exchangeName;

    @NotBlank(message = "El routing key no puede estar vacío")
    @Size(min = 1, max = 120, message = "El routing key debe tener entre 1 y 120 caracteres")
    @Pattern(
        regexp = "^[a-zA-Z0-9._*#-]+$",
        message = "El routing key solo puede contener letras, números, puntos, guiones y comodines (* o #)"
    )
    private String routingKey;
}
