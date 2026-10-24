package cl.duoc.dsy1107.messaging.consumer.dlq;

import cl.duoc.dsy1107.config.RabbitMQProperties;
import cl.duoc.dsy1107.dto.DocumentoEventDTO;
import cl.duoc.dsy1107.dto.NotificacionEventDTO;
import cl.duoc.dsy1107.dto.TicketEventDTO;
import cl.duoc.dsy1107.entity.EventoMensajeLog;
import cl.duoc.dsy1107.repository.EventoMensajeLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Slf4j
@Component
public class DeadLetterQueueConsumer {

    private final RabbitMQProperties properties;
    private final EventoMensajeLogRepository logRepository;

    public DeadLetterQueueConsumer(RabbitMQProperties properties, EventoMensajeLogRepository logRepository) {
        this.properties = properties;
        this.logRepository = logRepository;
    }

    @RabbitListener(
            queues = "${app.rabbitmq.queues.notificaciones.dlq}",
            autoStartup = "${app.rabbitmq.consume-dlq:false}"
    )
    public void auditarNotificacionDlq(NotificacionEventDTO payload, Message rawMessage) {
        String body = new String(rawMessage.getBody(), StandardCharsets.UTF_8);
        log.error("[DLQ-MONITOR][NOTIFICACION] Mensaje no entregado recibido en DLQ '{}': ID={}, payload={}",
                properties.getQueues().getNotificaciones().getDlq(), payload.getMessageId(), body);
        guardarAuditoriaDlq(payload.getMessageId(), "NOTIFICACION",
                properties.getQueues().getNotificaciones().getDlq(),
                properties.getQueues().getNotificaciones().getDlqRoutingKey(),
                "Mensaje almacenado en DLQ de Notificaciones para destinatario: " + payload.getDestinatario());
    }

    @RabbitListener(
            queues = "${app.rabbitmq.queues.tickets.dlq}",
            autoStartup = "${app.rabbitmq.consume-dlq:false}"
    )
    public void auditarTicketDlq(TicketEventDTO payload, Message rawMessage) {
        String body = new String(rawMessage.getBody(), StandardCharsets.UTF_8);
        log.error("[DLQ-MONITOR][TICKET] Mensaje no entregado recibido en DLQ '{}': ID={}, payload={}",
                properties.getQueues().getTickets().getDlq(), payload.getMessageId(), body);
        guardarAuditoriaDlq(payload.getMessageId(), "TICKET",
                properties.getQueues().getTickets().getDlq(),
                properties.getQueues().getTickets().getDlqRoutingKey(),
                "Mensaje almacenado en DLQ de Tickets para cliente: " + payload.getCliente());
    }

    @RabbitListener(
            queues = "${app.rabbitmq.queues.documentos.dlq}",
            autoStartup = "${app.rabbitmq.consume-dlq:false}"
    )
    public void auditarDocumentoDlq(DocumentoEventDTO payload, Message rawMessage) {
        String body = new String(rawMessage.getBody(), StandardCharsets.UTF_8);
        log.error("[DLQ-MONITOR][DOCUMENTO] Mensaje no entregado recibido en DLQ '{}': ID={}, payload={}",
                properties.getQueues().getDocumentos().getDlq(), payload.getMessageId(), body);
        guardarAuditoriaDlq(payload.getMessageId(), "DOCUMENTO",
                properties.getQueues().getDocumentos().getDlq(),
                properties.getQueues().getDocumentos().getDlqRoutingKey(),
                "Mensaje almacenado en DLQ de Documentos para RUT: " + payload.getRutReceptor());
    }

    private void guardarAuditoriaDlq(String messageId, String dominio, String dlqName, String dlqRk, String detalle) {
        try {
            logRepository.save(EventoMensajeLog.builder()
                    .messageId(messageId != null ? messageId : "DLQ-MSG")
                    .dominio(dominio)
                    .exchange(properties.getExchanges().getDeadLetter())
                    .routingKey(dlqRk)
                    .colaDestino(dlqName)
                    .estado("EN_COLA_DLQ")
                    .detalle(detalle)
                    .fechaEvento(LocalDateTime.now())
                    .build());
        } catch (Exception e) {
            log.warn("Error guardando auditoria DLQ: {}", e.getMessage());
        }
    }
}
