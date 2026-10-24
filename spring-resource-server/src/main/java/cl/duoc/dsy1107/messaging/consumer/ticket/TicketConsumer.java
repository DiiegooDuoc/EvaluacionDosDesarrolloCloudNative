package cl.duoc.dsy1107.messaging.consumer.ticket;

import cl.duoc.dsy1107.config.RabbitMQProperties;
import cl.duoc.dsy1107.dto.TicketEventDTO;
import cl.duoc.dsy1107.entity.EventoMensajeLog;
import cl.duoc.dsy1107.messaging.exception.NonRecoverableMessagingException;
import cl.duoc.dsy1107.messaging.exception.RecoverableMessagingException;
import cl.duoc.dsy1107.repository.EventoMensajeLogRepository;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class TicketConsumer {

    private final RabbitMQProperties properties;
    private final EventoMensajeLogRepository logRepository;
    private final Map<String, Integer> retryCounter = new ConcurrentHashMap<>();

    public TicketConsumer(RabbitMQProperties properties, EventoMensajeLogRepository logRepository) {
        this.properties = properties;
        this.logRepository = logRepository;
    }

    @RabbitListener(
            queues = "${app.rabbitmq.queues.tickets.name}",
            containerFactory = "manualAckListenerContainerFactory"
    )
    public void procesarTicket(
            TicketEventDTO payload,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {

        String messageId = payload.getMessageId() != null ? payload.getMessageId() : "SIN-ID";
        String queueName = properties.getQueues().getTickets().getName();

        log.info("[CONSUMER][TICKET] Procesando ticket ID={} para cliente='{}' prioridad='{}' (deliveryTag={})",
                messageId, payload.getCliente(), payload.getPrioridad(), deliveryTag);

        try {
            validarTicket(payload);

            log.info("[CONSUMER][TICKET] Ticket generado exitosamente en área '{}' con prioridad '{}'",
                    payload.getAreaResponsable(), payload.getPrioridad());

            retryCounter.remove(messageId);
            channel.basicAck(deliveryTag, false);

            log.info("[CONSUMER][TICKET] ACK confirmado exitosamente para ticket ID={}", messageId);
            guardarLog(messageId, queueName, "PROCESADO_ACK",
                    "Ticket generado y confirmado (basicAck) en área " + payload.getAreaResponsable());

        } catch (RecoverableMessagingException ex) {
            // Reintentar con requeue=true hasta maxRetries; luego enviar a DLQ
            int intentos = retryCounter.merge(messageId, 1, Integer::sum);
            if (intentos <= properties.getMaxRetries()) {
                log.warn("[CONSUMER][TICKET] Error recuperable en ticket ID={} (intento {}/{}): {}. Reencolando con basicNack(requeue=true)",
                        messageId, intentos, properties.getMaxRetries(), ex.getMessage());
                guardarLog(messageId, queueName, "REINTENTO_NACK",
                        "Reintento " + intentos + "/" + properties.getMaxRetries() + ": " + ex.getMessage());
                channel.basicNack(deliveryTag, false, true);
            } else {
                retryCounter.remove(messageId);
                log.error("[CONSUMER][TICKET] Reintentos agotados ({}) para ticket ID={}. Enviando a DLQ (basicNack requeue=false)",
                        properties.getMaxRetries(), messageId);
                guardarLog(messageId, queueName, "RECHAZADO_NACK_DLQ",
                        "Reintentos agotados en generación de ticket. Enviado a DLQ: " + ex.getMessage());
                channel.basicNack(deliveryTag, false, false);
            }

        } catch (NonRecoverableMessagingException ex) {
            // Error no recuperable: enviar directo a DLQ (requeue=false)
            retryCounter.remove(messageId);
            log.error("[CONSUMER][TICKET] Error NO recuperable en ticket ID={}: {}. Enviando a DLQ con basicNack(requeue=false)",
                    messageId, ex.getMessage());
            guardarLog(messageId, queueName, "RECHAZADO_NACK_DLQ",
                    "Error no recuperable en ticket (NACK -> DLQ): " + ex.getMessage());
            channel.basicNack(deliveryTag, false, false);

        } catch (Exception ex) {
            retryCounter.remove(messageId);
            log.error("[CONSUMER][TICKET] Error crítico en ticket ID={}: {}. Derivando a DLQ",
                    messageId, ex.getMessage(), ex);
            guardarLog(messageId, queueName, "ERROR_DLQ", "Fallo inesperado en ticket: " + ex.getMessage());
            channel.basicNack(deliveryTag, false, false);
        }
    }

    private void validarTicket(TicketEventDTO payload) {
        if (payload.isSimularErrorNoRecuperable()
                || (payload.getDescripcion() != null && payload.getDescripcion().toUpperCase().contains("FAIL_DLQ"))) {
            throw new NonRecoverableMessagingException(
                    "Datos del ticket inválidos o área de soporte inexistente para asignación");
        }
        if (payload.isSimularErrorRecuperable()) {
            throw new RecoverableMessagingException(
                    "Sistema de mesa de ayuda temporalmente saturado (reintento requerido)");
        }
    }

    private void guardarLog(String messageId, String cola, String estado, String detalle) {
        try {
            logRepository.save(EventoMensajeLog.builder()
                    .messageId(messageId)
                    .dominio("TICKET")
                    .exchange(properties.getExchanges().getDirect())
                    .routingKey(properties.getQueues().getTickets().getRoutingKey())
                    .colaDestino(cola)
                    .estado(estado)
                    .detalle(detalle)
                    .fechaEvento(LocalDateTime.now())
                    .build());
        } catch (Exception e) {
            log.warn("No se pudo registrar auditoría de ticket {}: {}", messageId, e.getMessage());
        }
    }
}
