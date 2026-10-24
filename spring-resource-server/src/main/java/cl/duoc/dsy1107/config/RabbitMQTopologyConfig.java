package cl.duoc.dsy1107.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQTopologyConfig {

    private final RabbitMQProperties properties;

    public RabbitMQTopologyConfig(RabbitMQProperties properties) {
        this.properties = properties;
    }

    // Exchanges principales y Dead Letter Exchange (DLX)
    @Bean
    public DirectExchange pedidosDirectExchange() {
        return new DirectExchange(properties.getExchanges().getDirect(), true, false);
    }

    @Bean
    public TopicExchange pedidosTopicExchange() {
        return new TopicExchange(properties.getExchanges().getTopic(), true, false);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(properties.getExchanges().getDeadLetter(), true, false);
    }

    // Caso 1: Cola y DLQ de Notificaciones (Direct)
    @Bean
    public Queue notificacionesQueue() {
        return QueueBuilder.durable(properties.getQueues().getNotificaciones().getName())
                .withArgument("x-dead-letter-exchange", properties.getExchanges().getDeadLetter())
                .withArgument("x-dead-letter-routing-key", properties.getQueues().getNotificaciones().getDlqRoutingKey())
                .build();
    }

    @Bean
    public Queue notificacionesDlq() {
        return QueueBuilder.durable(properties.getQueues().getNotificaciones().getDlq()).build();
    }

    @Bean
    public Binding notificacionesBinding(Queue notificacionesQueue, DirectExchange pedidosDirectExchange) {
        return BindingBuilder.bind(notificacionesQueue)
                .to(pedidosDirectExchange)
                .with(properties.getQueues().getNotificaciones().getRoutingKey());
    }

    @Bean
    public Binding notificacionesDlqBinding(Queue notificacionesDlq, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(notificacionesDlq)
                .to(deadLetterExchange)
                .with(properties.getQueues().getNotificaciones().getDlqRoutingKey());
    }

    // Caso 2: Cola y DLQ de Tickets (Direct)
    @Bean
    public Queue ticketsQueue() {
        return QueueBuilder.durable(properties.getQueues().getTickets().getName())
                .withArgument("x-dead-letter-exchange", properties.getExchanges().getDeadLetter())
                .withArgument("x-dead-letter-routing-key", properties.getQueues().getTickets().getDlqRoutingKey())
                .build();
    }

    @Bean
    public Queue ticketsDlq() {
        return QueueBuilder.durable(properties.getQueues().getTickets().getDlq()).build();
    }

    @Bean
    public Binding ticketsBinding(Queue ticketsQueue, DirectExchange pedidosDirectExchange) {
        return BindingBuilder.bind(ticketsQueue)
                .to(pedidosDirectExchange)
                .with(properties.getQueues().getTickets().getRoutingKey());
    }

    @Bean
    public Binding ticketsDlqBinding(Queue ticketsDlq, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(ticketsDlq)
                .to(deadLetterExchange)
                .with(properties.getQueues().getTickets().getDlqRoutingKey());
    }

    // Caso 3: Cola y DLQ de Documentos (Topic)
    @Bean
    public Queue documentosQueue() {
        return QueueBuilder.durable(properties.getQueues().getDocumentos().getName())
                .withArgument("x-dead-letter-exchange", properties.getExchanges().getDeadLetter())
                .withArgument("x-dead-letter-routing-key", properties.getQueues().getDocumentos().getDlqRoutingKey())
                .build();
    }

    @Bean
    public Queue documentosDlq() {
        return QueueBuilder.durable(properties.getQueues().getDocumentos().getDlq()).build();
    }

    @Bean
    public Binding documentosBinding(Queue documentosQueue, TopicExchange pedidosTopicExchange) {
        return BindingBuilder.bind(documentosQueue)
                .to(pedidosTopicExchange)
                .with(properties.getQueues().getDocumentos().getRoutingKey());
    }

    @Bean
    public Binding documentosDlqBinding(Queue documentosDlq, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(documentosDlq)
                .to(deadLetterExchange)
                .with(properties.getQueues().getDocumentos().getDlqRoutingKey());
    }
}
