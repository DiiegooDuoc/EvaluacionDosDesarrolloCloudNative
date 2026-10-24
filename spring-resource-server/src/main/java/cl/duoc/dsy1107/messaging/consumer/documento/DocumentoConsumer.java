package cl.duoc.dsy1107.messaging.consumer.documento;

import cl.duoc.dsy1107.config.RabbitMQProperties;
import cl.duoc.dsy1107.dto.DocumentoEventDTO;
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
public class DocumentoConsumer {

    private final RabbitMQProperties properties;
    private final EventoMensajeLogRepository logRepository;
    private final Map<String, Integer> retryCounter = new ConcurrentHashMap<>();

    public DocumentoConsumer(RabbitMQProperties properties, EventoMensajeLogRepository logRepository) {
        this.properties = properties;
        this.logRepository = logRepository;
    }

    @RabbitListener(
            queues = "${app.rabbitmq.queues.documentos.name}",
            containerFactory = "manualAckListenerContainerFactory"
    )
    public void procesarDocumento(
            DocumentoEventDTO payload,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
            @Header(value = AmqpHeaders.RECEIVED_ROUTING_KEY, required = false) String receivedRoutingKey) throws IOException {

        String messageId = payload.getMessageId() != null ? payload.getMessageId() : "SIN-ID";
        String queueName = properties.getQueues().getDocumentos().getName();
        String routingKey = receivedRoutingKey != null ? receivedRoutingKey : properties.getQueues().getDocumentos().getRoutingKey();

        log.info("[CONSUMER][DOCUMENTO] Recibido documento ID={} tipo='{}' routingKey='{}' monto=${} (deliveryTag={})",
                messageId, payload.getTipoDocumento(), routingKey, payload.getMontoTotal(), deliveryTag);

        try {
            validarDocumento(payload);

            log.info("[CONSUMER][DOCUMENTO] Documento tributario {} emitido exitosamente para RUT {} por ${}",
                    payload.getTipoDocumento(), payload.getRutReceptor(), payload.getMontoTotal());

            retryCounter.remove(messageId);
            channel.basicAck(deliveryTag, false);

            log.info("[CONSUMER][DOCUMENTO] ACK confirmado exitosamente para documento ID={}", messageId);
            guardarLog(messageId, routingKey, queueName, "PROCESADO_ACK",
                    "Documento " + payload.getTipoDocumento() + " emitido y confirmado (basicAck) para RUT " + payload.getRutReceptor());

        } catch (RecoverableMessagingException ex) {
            // Reintentar con requeue=true hasta maxRetries; luego enviar a DLQ
            int intentos = retryCounter.merge(messageId, 1, Integer::sum);
            if (intentos <= properties.getMaxRetries()) {
                log.warn("[CONSUMER][DOCUMENTO] Error recuperable en documento ID={} (intento {}/{}): {}. Aplicando basicNack(requeue=true)",
                        messageId, intentos, properties.getMaxRetries(), ex.getMessage());
                guardarLog(messageId, routingKey, queueName, "REINTENTO_NACK",
                        "Reintento " + intentos + "/" + properties.getMaxRetries() + ": " + ex.getMessage());
                channel.basicNack(deliveryTag, false, true);
            } else {
                retryCounter.remove(messageId);
                log.error("[CONSUMER][DOCUMENTO] Reintentos agotados ({}) para documento ID={}. Enviando a DLQ (basicNack requeue=false)",
                        properties.getMaxRetries(), messageId);
                guardarLog(messageId, routingKey, queueName, "RECHAZADO_NACK_DLQ",
                        "Reintentos agotados en emisión de documento. Enviado a DLQ: " + ex.getMessage());
                channel.basicNack(deliveryTag, false, false);
            }

        } catch (NonRecoverableMessagingException ex) {
            // Error no recuperable: enviar directo a DLQ (requeue=false)
            retryCounter.remove(messageId);
            log.error("[CONSUMER][DOCUMENTO] Error NO recuperable en documento ID={}: {}. Enviando a DLQ con basicNack(requeue=false)",
                    messageId, ex.getMessage());
            guardarLog(messageId, routingKey, queueName, "RECHAZADO_NACK_DLQ",
                    "Error no recuperable en documento (NACK -> DLQ): " + ex.getMessage());
            channel.basicNack(deliveryTag, false, false);

        } catch (Exception ex) {
            retryCounter.remove(messageId);
            log.error("[CONSUMER][DOCUMENTO] Error inesperado en documento ID={}: {}. Derivando a DLQ",
                    messageId, ex.getMessage(), ex);
            guardarLog(messageId, routingKey, queueName, "ERROR_DLQ", "Fallo inesperado en documento: " + ex.getMessage());
            channel.basicNack(deliveryTag, false, false);
        }
    }

    private void validarDocumento(DocumentoEventDTO payload) {
        if (payload.isSimularErrorNoRecuperable()
                || payload.getMontoTotal() == null
                || payload.getMontoTotal() <= 0) {
            throw new NonRecoverableMessagingException(
                    "Rechazo de firma electrónica SII: datos tributarios inválidos o timbre rechazado para RUT " + payload.getRutReceptor());
        }
        if (payload.isSimularErrorRecuperable()) {
            throw new RecoverableMessagingException(
                    "Servicio de foliación electrónica temporalmente no disponible");
        }
    }

    private void guardarLog(String messageId, String routingKey, String cola, String estado, String detalle) {
        try {
            logRepository.save(EventoMensajeLog.builder()
                    .messageId(messageId)
                    .dominio("DOCUMENTO")
                    .exchange(properties.getExchanges().getTopic())
                    .routingKey(routingKey)
                    .colaDestino(cola)
                    .estado(estado)
                    .detalle(detalle)
                    .fechaEvento(LocalDateTime.now())
                    .build());
        } catch (Exception e) {
            log.warn("No se pudo registrar auditoría de documento {}: {}", messageId, e.getMessage());
        }
    }
}
