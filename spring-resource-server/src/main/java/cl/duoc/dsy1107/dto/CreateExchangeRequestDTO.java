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
public class CreateExchangeRequestDTO {

    @NotBlank(message = "El nombre del exchange no puede estar vacío")
    @Size(min = 3, max = 120, message = "El nombre del exchange debe tener entre 3 y 120 caracteres")
    @Pattern(
        regexp = "^[a-zA-Z0-9._-]+$",
        message = "El nombre del exchange solo puede contener letras, números, puntos, guiones y guiones bajos"
    )
    private String name;

    @NotBlank(message = "El tipo de exchange es obligatorio (direct, topic, fanout o headers)")
    @Pattern(
        regexp = "^(direct|topic|fanout|headers)$",
        message = "El tipo de exchange es inválido. Valores permitidos: direct, topic, fanout, headers"
    )
    private String type;

    private boolean durable = true;
    private boolean autoDelete = false;
}
