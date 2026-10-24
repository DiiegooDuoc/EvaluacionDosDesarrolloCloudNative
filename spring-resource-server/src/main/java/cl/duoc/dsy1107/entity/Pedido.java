package cl.duoc.dsy1107.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "pedidos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Pedido {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String clienteEmail;

    @Column(nullable = false, length = 150)
    private String productoNombre;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(nullable = false)
    private Double montoTotal;

    @Column(nullable = false, length = 40)
    private String tipoDocumento;

    @Column(nullable = false, length = 40)
    private String estado;

    @Column(nullable = false)
    private LocalDateTime fechaCreacion;
}
