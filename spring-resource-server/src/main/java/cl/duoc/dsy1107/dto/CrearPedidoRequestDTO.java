package cl.duoc.dsy1107.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CrearPedidoRequestDTO {

    @NotBlank(message = "El correo del cliente es obligatorio")
    @Email(message = "El correo del cliente debe tener un formato válido")
    private String clienteEmail;

    @NotNull(message = "Debe seleccionar un producto válido")
    private Long productoId;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad mínima es 1")
    private Integer cantidad;

    @NotBlank(message = "El tipo de documento tributario es obligatorio")
    @Pattern(regexp = "^(BOLETA|FACTURA|GUIA_DESPACHO)$", message = "El tipo de documento debe ser BOLETA, FACTURA o GUIA_DESPACHO")
    private String tipoDocumento;

    private boolean simularErrorDlq;
}
