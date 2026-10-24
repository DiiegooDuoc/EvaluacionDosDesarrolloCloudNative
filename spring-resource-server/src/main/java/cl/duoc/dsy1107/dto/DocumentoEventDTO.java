package cl.duoc.dsy1107.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentoEventDTO {
    private String messageId;
    private Long pedidoId;

    @NotBlank(message = "El tipo de documento es obligatorio")
    @Pattern(regexp = "^(BOLETA|FACTURA|GUIA_DESPACHO)$", message = "El tipo de documento debe ser BOLETA, FACTURA o GUIA_DESPACHO")
    private String tipoDocumento;

    @NotBlank(message = "El RUT o identificador del receptor no puede estar vacío")
    private String rutReceptor;

    @NotBlank(message = "El detalle del producto o servicio es obligatorio")
    private String detalleGlosa;

    @NotNull(message = "El monto total del documento es obligatorio")
    @Positive(message = "El monto total del documento debe ser mayor que cero")
    private Double montoTotal;

    private boolean simularErrorRecuperable;
    private boolean simularErrorNoRecuperable;
    private LocalDateTime timestamp;
}
