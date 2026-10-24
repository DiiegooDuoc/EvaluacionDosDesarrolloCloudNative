package cl.duoc.dsy1107.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificacionEventDTO {
    private String messageId;
    private Long pedidoId;

    @NotBlank(message = "El destinatario de la notificación no puede estar vacío")
    @Email(message = "El destinatario debe ser un correo electrónico válido")
    private String destinatario;

    @NotBlank(message = "El asunto de la notificación es obligatorio")
    @Size(min = 3, max = 150, message = "El asunto debe tener entre 3 y 150 caracteres")
    private String asunto;

    @NotBlank(message = "El cuerpo del mensaje de notificación es obligatorio")
    private String mensaje;

    @Builder.Default
    private String canal = "EMAIL";

    private boolean simularErrorRecuperable;
    private boolean simularErrorNoRecuperable;
    private LocalDateTime timestamp;
}
