package cl.duoc.dsy1107.controller;

import cl.duoc.dsy1107.dto.CrearPedidoRequestDTO;
import cl.duoc.dsy1107.dto.DocumentoEventDTO;
import cl.duoc.dsy1107.dto.NotificacionEventDTO;
import cl.duoc.dsy1107.dto.TicketEventDTO;
import cl.duoc.dsy1107.entity.EventoMensajeLog;
import cl.duoc.dsy1107.entity.Pedido;
import cl.duoc.dsy1107.messaging.producer.EventMessagePublisher;
import cl.duoc.dsy1107.service.PedidoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class PedidoMensajeriaController {

    private final PedidoService pedidoService;
    private final EventMessagePublisher eventPublisher;

    public PedidoMensajeriaController(PedidoService pedidoService, EventMessagePublisher eventPublisher) {
        this.pedidoService = pedidoService;
        this.eventPublisher = eventPublisher;
    }

    @GetMapping("/pedidos")
    public List<Pedido> listarPedidos() {
        return pedidoService.listarPedidos();
    }

    @PostMapping("/pedidos")
    public ResponseEntity<Map<String, Object>> crearPedido(@Valid @RequestBody CrearPedidoRequestDTO request) {
        Map<String, Object> result = pedidoService.crearPedidoYDispararEventos(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping("/mensajeria/notificaciones")
    public ResponseEntity<NotificacionEventDTO> enviarNotificacion(@Valid @RequestBody NotificacionEventDTO dto) {
        return ResponseEntity.accepted().body(eventPublisher.publicarNotificacion(dto));
    }

    @PostMapping("/mensajeria/tickets")
    public ResponseEntity<TicketEventDTO> enviarTicket(@Valid @RequestBody TicketEventDTO dto) {
        return ResponseEntity.accepted().body(eventPublisher.publicarTicket(dto));
    }

    @PostMapping("/mensajeria/documentos")
    public ResponseEntity<DocumentoEventDTO> enviarDocumento(@Valid @RequestBody DocumentoEventDTO dto) {
        return ResponseEntity.accepted().body(eventPublisher.publicarDocumento(dto));
    }

    @GetMapping("/mensajeria/eventos")
    public List<EventoMensajeLog> listarEventosMensajeria() {
        return pedidoService.listarHistorialEventos();
    }
}
