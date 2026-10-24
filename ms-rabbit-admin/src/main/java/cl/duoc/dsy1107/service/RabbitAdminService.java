package cl.duoc.dsy1107.service;

import cl.duoc.dsy1107.config.RabbitMQProperties;
import cl.duoc.dsy1107.dto.CreateBindingRequestDTO;
import cl.duoc.dsy1107.dto.CreateExchangeRequestDTO;
import cl.duoc.dsy1107.dto.CreateQueueRequestDTO;
import cl.duoc.dsy1107.dto.RabbitResourceResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.CustomExchange;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.HeadersExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class RabbitAdminService {

    private final AmqpAdmin amqpAdmin;
    private final RabbitMQProperties properties;

    private final Set<String> registeredQueues = ConcurrentHashMap.newKeySet();
    private final Map<String, String> registeredExchanges = new ConcurrentHashMap<>();
    private final List<Map<String, String>> registeredBindings = Collections.synchronizedList(new ArrayList<>());

    public RabbitAdminService(AmqpAdmin amqpAdmin, RabbitMQProperties properties) {
        this.amqpAdmin = amqpAdmin;
        this.properties = properties;
        inicializarCatalogoBase();
    }

    private void inicializarCatalogoBase() {
        registeredQueues.add(properties.getQueues().getNotificaciones().getName());
        registeredQueues.add(properties.getQueues().getNotificaciones().getDlq());
        registeredQueues.add(properties.getQueues().getTickets().getName());
        registeredQueues.add(properties.getQueues().getTickets().getDlq());
        registeredQueues.add(properties.getQueues().getDocumentos().getName());
        registeredQueues.add(properties.getQueues().getDocumentos().getDlq());

        registeredExchanges.put(properties.getExchanges().getDirect(), "direct");
        registeredExchanges.put(properties.getExchanges().getTopic(), "topic");
        registeredExchanges.put(properties.getExchanges().getDeadLetter(), "direct (DLX)");

        agregarBindingAlCatalogo(properties.getExchanges().getDirect(),
                properties.getQueues().getNotificaciones().getName(),
                properties.getQueues().getNotificaciones().getRoutingKey());
        agregarBindingAlCatalogo(properties.getExchanges().getDirect(),
                properties.getQueues().getTickets().getName(),
                properties.getQueues().getTickets().getRoutingKey());
        agregarBindingAlCatalogo(properties.getExchanges().getTopic(),
                properties.getQueues().getDocumentos().getName(),
                properties.getQueues().getDocumentos().getRoutingKey());
        agregarBindingAlCatalogo(properties.getExchanges().getDeadLetter(),
                properties.getQueues().getNotificaciones().getDlq(),
                properties.getQueues().getNotificaciones().getDlqRoutingKey());
        agregarBindingAlCatalogo(properties.getExchanges().getDeadLetter(),
                properties.getQueues().getTickets().getDlq(),
                properties.getQueues().getTickets().getDlqRoutingKey());
        agregarBindingAlCatalogo(properties.getExchanges().getDeadLetter(),
                properties.getQueues().getDocumentos().getDlq(),
                properties.getQueues().getDocumentos().getDlqRoutingKey());
    }

    private void agregarBindingAlCatalogo(String exchange, String queue, String routingKey) {
        Map<String, String> item = new LinkedHashMap<>();
        item.put("exchangeName", exchange);
        item.put("queueName", queue);
        item.put("routingKey", routingKey);
        registeredBindings.add(item);
    }

    public Map<String, Object> obtenerResumenTopologia() {
        List<Map<String, Object>> colasInfo = new ArrayList<>();
        for (String queueName : registeredQueues) {
            Map<String, Object> qData = new LinkedHashMap<>();
            qData.put("name", queueName);
            qData.put("isDlq", queueName.endsWith(".dlq"));
            try {
                Properties props = amqpAdmin.getQueueProperties(queueName);
                if (props != null) {
                    qData.put("existsInBroker", true);
                    qData.put("messageCount", props.get(RabbitAdmin.QUEUE_MESSAGE_COUNT));
                    qData.put("consumerCount", props.get(RabbitAdmin.QUEUE_CONSUMER_COUNT));
                } else {
                    qData.put("existsInBroker", false);
                    qData.put("messageCount", 0);
                    qData.put("consumerCount", 0);
                }
            } catch (Exception e) {
                qData.put("existsInBroker", false);
                qData.put("messageCount", 0);
                qData.put("consumerCount", 0);
            }
            colasInfo.add(qData);
        }

        List<Map<String, String>> exchangesInfo = new ArrayList<>();
        registeredExchanges.forEach((name, type) -> {
            Map<String, String> ex = new LinkedHashMap<>();
            ex.put("name", name);
            ex.put("type", type);
            exchangesInfo.add(ex);
        });

        Map<String, Object> resumen = new LinkedHashMap<>();
        resumen.put("queues", colasInfo);
        resumen.put("exchanges", exchangesInfo);
        resumen.put("bindings", new ArrayList<>(registeredBindings));
        return resumen;
    }

    public RabbitResourceResponseDTO createQueue(CreateQueueRequestDTO dto) {
        String queueName = dto.getName().trim();

        QueueBuilder builder = dto.isDurable()
                ? QueueBuilder.durable(queueName)
                : QueueBuilder.nonDurable(queueName);

        if (dto.isExclusive()) {
            builder.exclusive();
        }
        if (dto.isAutoDelete()) {
            builder.autoDelete();
        }

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("durable", dto.isDurable());
        details.put("exclusive", dto.isExclusive());
        details.put("autoDelete", dto.isAutoDelete());

        if (dto.getDeadLetterExchange() != null && !dto.getDeadLetterExchange().isBlank()) {
            builder.withArgument("x-dead-letter-exchange", dto.getDeadLetterExchange().trim());
            details.put("x-dead-letter-exchange", dto.getDeadLetterExchange().trim());
        }
        if (dto.getDeadLetterRoutingKey() != null && !dto.getDeadLetterRoutingKey().isBlank()) {
            builder.withArgument("x-dead-letter-routing-key", dto.getDeadLetterRoutingKey().trim());
            details.put("x-dead-letter-routing-key", dto.getDeadLetterRoutingKey().trim());
        }
        if (dto.getMessageTtlMs() != null) {
            builder.withArgument("x-message-ttl", dto.getMessageTtlMs());
            details.put("x-message-ttl", dto.getMessageTtlMs());
        }

        Queue queue = builder.build();
        String declaredName = amqpAdmin.declareQueue(queue);
        registeredQueues.add(queueName);

        log.info("[RABBIT-ADMIN] Cola '{}' declarada exitosamente en RabbitMQ. Detalles: {}", declaredName, details);

        return RabbitResourceResponseDTO.builder()
                .resourceType("QUEUE")
                .resourceName(queueName)
                .operation("CREATE")
                .status("SUCCESS")
                .message("Cola '" + queueName + "' creada exitosamente en el clúster RabbitMQ")
                .details(details)
                .timestamp(Instant.now())
                .build();
    }

    public RabbitResourceResponseDTO deleteQueue(String queueName) {
        validarNombreRecurso(queueName, "cola");
        String trimmed = queueName.trim();

        boolean deleted = amqpAdmin.deleteQueue(trimmed);
        registeredQueues.remove(trimmed);
        registeredBindings.removeIf(b -> trimmed.equals(b.get("queueName")));

        log.info("[RABBIT-ADMIN] Solicitud de eliminación de cola '{}' ejecutada (deleted={})", trimmed, deleted);

        return RabbitResourceResponseDTO.builder()
                .resourceType("QUEUE")
                .resourceName(trimmed)
                .operation("DELETE")
                .status("SUCCESS")
                .message("Cola '" + trimmed + "' eliminada correctamente de RabbitMQ")
                .details(Map.of("deletedFromBroker", deleted))
                .timestamp(Instant.now())
                .build();
    }

    public RabbitResourceResponseDTO purgeQueue(String queueName) {
        validarNombreRecurso(queueName, "cola");
        String trimmed = queueName.trim();
        int purgedMessages = amqpAdmin.purgeQueue(trimmed);

        log.info("[RABBIT-ADMIN] Cola '{}' purgada ({} mensajes eliminados)", trimmed, purgedMessages);

        return RabbitResourceResponseDTO.builder()
                .resourceType("QUEUE")
                .resourceName(trimmed)
                .operation("PURGE")
                .status("SUCCESS")
                .message("Se purgaron los mensajes de la cola '" + trimmed + "'")
                .details(Map.of("purgedMessageCount", purgedMessages))
                .timestamp(Instant.now())
                .build();
    }

    public RabbitResourceResponseDTO createExchange(CreateExchangeRequestDTO dto) {
        String name = dto.getName().trim();
        String type = dto.getType().trim().toLowerCase();

        Exchange exchange = switch (type) {
            case "direct" -> new DirectExchange(name, dto.isDurable(), dto.isAutoDelete());
            case "topic" -> new TopicExchange(name, dto.isDurable(), dto.isAutoDelete());
            case "fanout" -> new FanoutExchange(name, dto.isDurable(), dto.isAutoDelete());
            case "headers" -> new HeadersExchange(name, dto.isDurable(), dto.isAutoDelete());
            default -> new CustomExchange(name, type, dto.isDurable(), dto.isAutoDelete());
        };

        amqpAdmin.declareExchange(exchange);
        registeredExchanges.put(name, type);

        log.info("[RABBIT-ADMIN] Exchange '{}' de tipo '{}' creado exitosamente", name, type);

        return RabbitResourceResponseDTO.builder()
                .resourceType("EXCHANGE")
                .resourceName(name)
                .operation("CREATE")
                .status("SUCCESS")
                .message("Exchange '" + name + "' (tipo " + type + ") creado exitosamente")
                .details(Map.of("type", type, "durable", dto.isDurable(), "autoDelete", dto.isAutoDelete()))
                .timestamp(Instant.now())
                .build();
    }

    public RabbitResourceResponseDTO deleteExchange(String exchangeName) {
        validarNombreRecurso(exchangeName, "exchange");
        String trimmed = exchangeName.trim();

        boolean deleted = amqpAdmin.deleteExchange(trimmed);
        registeredExchanges.remove(trimmed);
        registeredBindings.removeIf(b -> trimmed.equals(b.get("exchangeName")));

        log.info("[RABBIT-ADMIN] Exchange '{}' eliminado de RabbitMQ (deleted={})", trimmed, deleted);

        return RabbitResourceResponseDTO.builder()
                .resourceType("EXCHANGE")
                .resourceName(trimmed)
                .operation("DELETE")
                .status("SUCCESS")
                .message("Exchange '" + trimmed + "' eliminado correctamente")
                .details(Map.of("deletedFromBroker", deleted))
                .timestamp(Instant.now())
                .build();
    }

    public RabbitResourceResponseDTO createBinding(CreateBindingRequestDTO dto) {
        String queueName = dto.getQueueName().trim();
        String exchangeName = dto.getExchangeName().trim();
        String routingKey = dto.getRoutingKey().trim();

        Binding binding = new Binding(
                queueName,
                Binding.DestinationType.QUEUE,
                exchangeName,
                routingKey,
                Collections.emptyMap()
        );

        amqpAdmin.declareBinding(binding);
        agregarBindingAlCatalogo(exchangeName, queueName, routingKey);

        log.info("[RABBIT-ADMIN] Binding creado: Exchange '{}' -> Cola '{}' (routingKey='{}')",
                exchangeName, queueName, routingKey);

        return RabbitResourceResponseDTO.builder()
                .resourceType("BINDING")
                .resourceName(exchangeName + " -> " + queueName)
                .operation("CREATE")
                .status("SUCCESS")
                .message("Binding creado entre exchange '" + exchangeName + "' y cola '" + queueName + "'")
                .details(Map.of("exchangeName", exchangeName, "queueName", queueName, "routingKey", routingKey))
                .timestamp(Instant.now())
                .build();
    }

    public RabbitResourceResponseDTO deleteBinding(CreateBindingRequestDTO dto) {
        String queueName = dto.getQueueName().trim();
        String exchangeName = dto.getExchangeName().trim();
        String routingKey = dto.getRoutingKey().trim();

        Binding binding = new Binding(
                queueName,
                Binding.DestinationType.QUEUE,
                exchangeName,
                routingKey,
                Collections.emptyMap()
        );

        amqpAdmin.removeBinding(binding);
        registeredBindings.removeIf(b ->
                exchangeName.equals(b.get("exchangeName"))
                        && queueName.equals(b.get("queueName"))
                        && routingKey.equals(b.get("routingKey"))
        );

        log.info("[RABBIT-ADMIN] Binding eliminado: Exchange '{}' -> Cola '{}' (routingKey='{}')",
                exchangeName, queueName, routingKey);

        return RabbitResourceResponseDTO.builder()
                .resourceType("BINDING")
                .resourceName(exchangeName + " -> " + queueName)
                .operation("DELETE")
                .status("SUCCESS")
                .message("Binding eliminado correctamente")
                .details(Map.of("exchangeName", exchangeName, "queueName", queueName, "routingKey", routingKey))
                .timestamp(Instant.now())
                .build();
    }

    private void validarNombreRecurso(String name, String tipo) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("El nombre del recurso (" + tipo + ") no puede estar vacío");
        }
        if (!name.trim().matches("^[a-zA-Z0-9._-]+$")) {
            throw new IllegalArgumentException("El nombre del " + tipo + " contiene caracteres inválidos");
        }
    }
}
