package cl.duoc.dsy1107.controller;

import cl.duoc.dsy1107.dto.CreateBindingRequestDTO;
import cl.duoc.dsy1107.dto.CreateExchangeRequestDTO;
import cl.duoc.dsy1107.dto.CreateQueueRequestDTO;
import cl.duoc.dsy1107.dto.RabbitResourceResponseDTO;
import cl.duoc.dsy1107.service.RabbitAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class RabbitAdminController {

    private final RabbitAdminService rabbitAdminService;

    public RabbitAdminController(RabbitAdminService rabbitAdminService) {
        this.rabbitAdminService = rabbitAdminService;
    }

    @GetMapping({"/queues", "/api/admin/rabbit/topology", "/api/admin/rabbit/queues"})
    public ResponseEntity<Map<String, Object>> obtenerTopologia() {
        return ResponseEntity.ok(rabbitAdminService.obtenerResumenTopologia());
    }

    @PostMapping({"/queues", "/api/admin/rabbit/queues"})
    public ResponseEntity<RabbitResourceResponseDTO> crearCola(
            @Valid @RequestBody CreateQueueRequestDTO request) {
        RabbitResourceResponseDTO response = rabbitAdminService.createQueue(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping({"/queues/{name}", "/api/admin/rabbit/queues/{name}"})
    public ResponseEntity<RabbitResourceResponseDTO> eliminarCola(@PathVariable("name") String name) {
        RabbitResourceResponseDTO response = rabbitAdminService.deleteQueue(name);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping({"/queues/{name}/messages", "/api/admin/rabbit/queues/{name}/messages"})
    public ResponseEntity<RabbitResourceResponseDTO> purgarCola(@PathVariable("name") String name) {
        RabbitResourceResponseDTO response = rabbitAdminService.purgeQueue(name);
        return ResponseEntity.ok(response);
    }

    @PostMapping({"/exchanges", "/api/admin/rabbit/exchanges"})
    public ResponseEntity<RabbitResourceResponseDTO> crearExchange(
            @Valid @RequestBody CreateExchangeRequestDTO request) {
        RabbitResourceResponseDTO response = rabbitAdminService.createExchange(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping({"/exchanges/{name}", "/api/admin/rabbit/exchanges/{name}"})
    public ResponseEntity<RabbitResourceResponseDTO> eliminarExchange(@PathVariable("name") String name) {
        RabbitResourceResponseDTO response = rabbitAdminService.deleteExchange(name);
        return ResponseEntity.ok(response);
    }

    @PostMapping({"/bindings", "/api/admin/rabbit/bindings"})
    public ResponseEntity<RabbitResourceResponseDTO> crearBinding(
            @Valid @RequestBody CreateBindingRequestDTO request) {
        RabbitResourceResponseDTO response = rabbitAdminService.createBinding(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping({"/bindings", "/api/admin/rabbit/bindings"})
    public ResponseEntity<RabbitResourceResponseDTO> eliminarBinding(
            @Valid @RequestBody CreateBindingRequestDTO request) {
        RabbitResourceResponseDTO response = rabbitAdminService.deleteBinding(request);
        return ResponseEntity.ok(response);
    }
}
