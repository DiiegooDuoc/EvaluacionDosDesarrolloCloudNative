package cl.duoc.dsy1107.messaging.producer;

import cl.duoc.dsy1107.config.RabbitMQProperties;
import cl.duoc.dsy1107.dto.DocumentoEventDTO;
import cl.duoc.dsy1107.dto.NotificacionEventDTO;
import cl.duoc.dsy1107.dto.TicketEventDTO;
import cl.duoc.dsy1107.entity.EventoMensajeLog;
import cl.duoc.dsy1107.repository.EventoMensajeLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Component
public class RabbitEventMessagePublisher implements EventMessagePublisher {

    private final RabbitTemplate rabbitTemplate;
    private final RabbitMQProperties properties;
    private final EventoMensajeLogRepository logRepository;

    public RabbitEventMessagePublisher(RabbitTemplate rabbitTemplate,
                                       RabbitMQProperties properties,
                                       EventoMensajeLogRepository logRepository) {
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
        this.logRepository = logRepository;
    }

    @Override
    public NotificacionEventDTO publicarNotificacion(NotificacionEventDTO event) {
        if (event.getMessageId() == null || event.getMessageId().isBlank()) {
            event.setMessageId("NOTIF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }
        event.setTimestamp(LocalDateTime.now());

        String exchange = properties.getExchanges().getDirect();
        String routingKey = properties.getQueues().getNotificaciones().getRoutingKey();
        String queueName = properties.getQueues().getNotificaciones().getName();

        log.info("[PRODUCER][NOTIFICACION] Publicando mensaje ID={} en exchange='{}' con routingKey='{}'",
                event.getMessageId(), exchange, routingKey);

        rabbitTemplate.convertAndSend(exchange, routingKey, event);

        registrarEvento(event.getMessageId(), "NOTIFICACION", exchange, routingKey, queueName,
                "PUBLICADO", "Notificación enviada a cola para destinatario: " + event.getDestinatario());
        return event;
    }

    @Override
    public TicketEventDTO publicarTicket(TicketEventDTO event) {
        if (event.getMessageId() == null || event.getMessageId().isBlank()) {
            event.setMessageId("TCK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }
        event.setTimestamp(LocalDateTime.now());

        String exchange = properties.getExchanges().getDirect();
        String routingKey = properties.getQueues().getTickets().getRoutingKey();
        String queueName = properties.getQueues().getTickets().getName();

        log.info("[PRODUCER][TICKET] Publicando mensaje ID={} en exchange='{}' con routingKey='{}'",
                event.getMessageId(), exchange, routingKey);

        rabbitTemplate.convertAndSend(exchange, routingKey, event);

        registrarEvento(event.getMessageId(), "TICKET", exchange, routingKey, queueName,
                "PUBLICADO", "Ticket prioridad " + event.getPrioridad() + " para área " + event.getAreaResponsable());
        return event;
    }

    @Override
    public DocumentoEventDTO publicarDocumento(DocumentoEventDTO event) {
        if (event.getMessageId() == null || event.getMessageId().isBlank()) {
            event.setMessageId("DOC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }
        event.setTimestamp(LocalDateTime.now());

        String exchange = properties.getExchanges().getTopic();
        String suffix = event.getTipoDocumento() != null ? event.getTipoDocumento().toLowerCase() : "general";
        String routingKey = properties.getQueues().getDocumentos().getPublishRoutingKeyPrefix() + suffix;
        String queueName = properties.getQueues().getDocumentos().getName();

        log.info("[PRODUCER][DOCUMENTO] Publicando mensaje ID={} en topicExchange='{}' con routingKey='{}'",
                event.getMessageId(), exchange, routingKey);

        rabbitTemplate.convertAndSend(exchange, routingKey, event);

        registrarEvento(event.getMessageId(), "DOCUMENTO", exchange, routingKey, queueName,
                "PUBLICADO", "Solicitud de " + event.getTipoDocumento() + " por $" + event.getMontoTotal());
        return event;
    }

    private void registrarEvento(String messageId, String dominio, String exchange,
                                 String routingKey, String colaDestino, String estado, String detalle) {
        try {
            logRepository.save(EventoMensajeLog.builder()
                    .messageId(messageId)
                    .dominio(dominio)
                    .exchange(exchange)
                    .routingKey(routingKey)
                    .colaDestino(colaDestino)
                    .estado(estado)
                    .detalle(detalle)
                    .fechaEvento(LocalDateTime.now())
                    .build());
        } catch (Exception e) {
            log.warn("No se pudo guardar trazabilidad en BD para mensaje {}: {}", messageId, e.getMessage());
        }
    }
}
