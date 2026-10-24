package cl.duoc.dsy1107.messaging.consumer.notificacion;

import cl.duoc.dsy1107.config.RabbitMQProperties;
import cl.duoc.dsy1107.dto.NotificacionEventDTO;
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
public class NotificacionConsumer {

    private final RabbitMQProperties properties;
    private final EventoMensajeLogRepository logRepository;
    private final Map<String, Integer> retryCounter = new ConcurrentHashMap<>();

    public NotificacionConsumer(RabbitMQProperties properties, EventoMensajeLogRepository logRepository) {
        this.properties = properties;
        this.logRepository = logRepository;
    }

    @RabbitListener(
            queues = "${app.rabbitmq.queues.notificaciones.name}",
            containerFactory = "manualAckListenerContainerFactory"
    )
    public void procesarNotificacion(
            NotificacionEventDTO payload,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {

        String messageId = payload.getMessageId() != null ? payload.getMessageId() : "SIN-ID";
        String queueName = properties.getQueues().getNotificaciones().getName();

        log.info("[CONSUMER][NOTIFICACION] Recibido mensaje ID={} desde cola='{}' (deliveryTag={})",
                messageId, queueName, deliveryTag);

        try {
            validarReglasDeNegocio(payload);

            log.info("[CONSUMER][NOTIFICACION] Enviando {} a '{}' con asunto '{}'",
                    payload.getCanal(), payload.getDestinatario(), payload.getAsunto());

            retryCounter.remove(messageId);
            channel.basicAck(deliveryTag, false);

            log.info("[CONSUMER][NOTIFICACION] ACK confirmado exitosamente para mensaje ID={}", messageId);
            guardarLog(messageId, queueName, "PROCESADO_ACK",
                    "Notificación entregada y confirmada (basicAck) para " + payload.getDestinatario());

        } catch (RecoverableMessagingException ex) {
            // Reintentar con requeue=true hasta maxRetries; luego derivar a DLQ
            int intentos = retryCounter.merge(messageId, 1, Integer::sum);
            if (intentos <= properties.getMaxRetries()) {
                log.warn("[CONSUMER][NOTIFICACION] Error recuperable en mensaje ID={} (intento {}/{}): {}. Aplicando basicNack con requeue=true",
                        messageId, intentos, properties.getMaxRetries(), ex.getMessage());
                guardarLog(messageId, queueName, "REINTENTO_NACK",
                        "Reintento " + intentos + "/" + properties.getMaxRetries() + ": " + ex.getMessage());
                channel.basicNack(deliveryTag, false, true);
            } else {
                retryCounter.remove(messageId);
                log.error("[CONSUMER][NOTIFICACION] Maximo de reintentos agotado ({}) para mensaje ID={}. Enviando a DLQ (basicNack requeue=false)",
                        properties.getMaxRetries(), messageId);
                guardarLog(messageId, queueName, "RECHAZADO_NACK_DLQ",
                        "Reintentos agotados. Enviado a DLQ: " + ex.getMessage());
                channel.basicNack(deliveryTag, false, false);
            }

        } catch (NonRecoverableMessagingException ex) {
            // Error no recuperable: enviar directo a DLQ (requeue=false)
            retryCounter.remove(messageId);
            log.error("[CONSUMER][NOTIFICACION] Error NO recuperable en mensaje ID={}: {}. Aplicando basicNack(requeue=false) -> DLQ",
                    messageId, ex.getMessage());
            guardarLog(messageId, queueName, "RECHAZADO_NACK_DLQ",
                    "Error no recuperable (NACK -> DLQ): " + ex.getMessage());
            channel.basicNack(deliveryTag, false, false);

        } catch (Exception ex) {
            retryCounter.remove(messageId);
            log.error("[CONSUMER][NOTIFICACION] Error inesperado procesando mensaje ID={}: {}. Derivando a DLQ",
                    messageId, ex.getMessage(), ex);
            guardarLog(messageId, queueName, "ERROR_DLQ", "Fallo inesperado derivado a DLQ: " + ex.getMessage());
            channel.basicNack(deliveryTag, false, false);
        }
    }

    private void validarReglasDeNegocio(NotificacionEventDTO payload) {
        if (payload.isSimularErrorNoRecuperable()
                || (payload.getDestinatario() != null && payload.getDestinatario().contains("error-dlq"))) {
            throw new NonRecoverableMessagingException(
                    "Destinatario bloqueado o marcado con falla crítica de validación (" + payload.getDestinatario() + ")");
        }
        if (payload.isSimularErrorRecuperable()) {
            throw new RecoverableMessagingException(
                    "Timeout temporal conectando con servidor SMTP externo");
        }
    }

    private void guardarLog(String messageId, String cola, String estado, String detalle) {
        try {
            logRepository.save(EventoMensajeLog.builder()
                    .messageId(messageId)
                    .dominio("NOTIFICACION")
                    .exchange(properties.getExchanges().getDirect())
                    .routingKey(properties.getQueues().getNotificaciones().getRoutingKey())
                    .colaDestino(cola)
                    .estado(estado)
                    .detalle(detalle)
                    .fechaEvento(LocalDateTime.now())
                    .build());
        } catch (Exception e) {
            log.warn("No se pudo registrar auditoría de notificación {}: {}", messageId, e.getMessage());
        }
    }
}
