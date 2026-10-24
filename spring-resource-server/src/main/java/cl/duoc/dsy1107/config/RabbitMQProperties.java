package cl.duoc.dsy1107.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.rabbitmq")
public class RabbitMQProperties {

    private int maxRetries = 2;
    private Exchanges exchanges = new Exchanges();
    private Queues queues = new Queues();

    @Data
    public static class Exchanges {
        private String direct;
        private String topic;
        private String deadLetter;
    }

    @Data
    public static class Queues {
        private QueueDefinition notificaciones = new QueueDefinition();
        private QueueDefinition tickets = new QueueDefinition();
        private DocumentosQueueDefinition documentos = new DocumentosQueueDefinition();
    }

    @Data
    public static class QueueDefinition {
        private String name;
        private String dlq;
        private String routingKey;
        private String dlqRoutingKey;
    }

    @Data
    public static class DocumentosQueueDefinition {
        private String name;
        private String dlq;
        private String routingKey;
        private String publishRoutingKeyPrefix;
        private String dlqRoutingKey;
    }
}
