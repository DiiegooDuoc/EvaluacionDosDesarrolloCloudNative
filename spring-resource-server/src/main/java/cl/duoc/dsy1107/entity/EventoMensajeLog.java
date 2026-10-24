package cl.duoc.dsy1107.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "eventos_mensajeria_log")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventoMensajeLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String messageId;

    @Column(nullable = false, length = 50)
    private String dominio;

    @Column(nullable = false, length = 120)
    private String exchange;

    @Column(nullable = false, length = 120)
    private String routingKey;

    @Column(nullable = false, length = 120)
    private String colaDestino;

    @Column(nullable = false, length = 40)
    private String estado;

    @Column(length = 1000)
    private String detalle;

    @Column(nullable = false)
    private LocalDateTime fechaEvento;
}
