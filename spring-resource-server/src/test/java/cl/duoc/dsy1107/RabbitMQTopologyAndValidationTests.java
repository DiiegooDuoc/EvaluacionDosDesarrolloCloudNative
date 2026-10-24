package cl.duoc.dsy1107;

import cl.duoc.dsy1107.config.RabbitMQProperties;
import cl.duoc.dsy1107.config.RabbitMQTopologyConfig;
import cl.duoc.dsy1107.dto.CreateExchangeRequestDTO;
import cl.duoc.dsy1107.dto.CreateQueueRequestDTO;
import cl.duoc.dsy1107.dto.NotificacionEventDTO;
import cl.duoc.dsy1107.messaging.consumer.notificacion.NotificacionConsumer;
import cl.duoc.dsy1107.repository.EventoMensajeLogRepository;
import com.rabbitmq.client.Channel;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

class RabbitMQTopologyAndValidationTests {

    private RabbitMQProperties properties;
    private RabbitMQTopologyConfig topologyConfig;
    private Validator validator;

    @BeforeEach
    void setUp() {
        properties = new RabbitMQProperties();
        properties.setMaxRetries(2);
        properties.getExchanges().setDirect("pedidos.direct.exchange");
        properties.getExchanges().setTopic("pedidos.topic.exchange");
        properties.getExchanges().setDeadLetter("pedidos.dlx.exchange");

        properties.getQueues().getNotificaciones().setName("pedidos.notificaciones.queue");
        properties.getQueues().getNotificaciones().setDlq("pedidos.notificaciones.dlq");
        properties.getQueues().getNotificaciones().setRoutingKey("pedidos.notificacion.rk");
        properties.getQueues().getNotificaciones().setDlqRoutingKey("pedidos.notificacion.dlq.rk");

        properties.getQueues().getTickets().setName("pedidos.tickets.queue");
        properties.getQueues().getTickets().setDlq("pedidos.tickets.dlq");
        properties.getQueues().getTickets().setRoutingKey("pedidos.ticket.rk");
        properties.getQueues().getTickets().setDlqRoutingKey("pedidos.ticket.dlq.rk");

        properties.getQueues().getDocumentos().setName("pedidos.documentos.queue");
        properties.getQueues().getDocumentos().setDlq("pedidos.documentos.dlq");
        properties.getQueues().getDocumentos().setRoutingKey("pedidos.documento.#");
        properties.getQueues().getDocumentos().setPublishRoutingKeyPrefix("pedidos.documento.");
        properties.getQueues().getDocumentos().setDlqRoutingKey("pedidos.documento.dlq.rk");

        topologyConfig = new RabbitMQTopologyConfig(properties);

        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void debeDeclararTresColasConSusRespectivasDlqYExchanges() {
        DirectExchange direct = topologyConfig.pedidosDirectExchange();
        TopicExchange topic = topologyConfig.pedidosTopicExchange();
        DirectExchange dlx = topologyConfig.deadLetterExchange();

        assertEquals("pedidos.direct.exchange", direct.getName());
        assertEquals("pedidos.topic.exchange", topic.getName());
        assertEquals("pedidos.dlx.exchange", dlx.getName());

        Queue notifQueue = topologyConfig.notificacionesQueue();
        Queue ticketsQueue = topologyConfig.ticketsQueue();
        Queue docsQueue = topologyConfig.documentosQueue();

        assertEquals("pedidos.dlx.exchange", notifQueue.getArguments().get("x-dead-letter-exchange"));
        assertEquals("pedidos.dlx.exchange", ticketsQueue.getArguments().get("x-dead-letter-exchange"));
        assertEquals("pedidos.dlx.exchange", docsQueue.getArguments().get("x-dead-letter-exchange"));
    }

    @Test
    void debeRechazarCreacionDeColaConNombreVacioOInvalido() {
        CreateQueueRequestDTO emptyDto = new CreateQueueRequestDTO("", true, false, false, null, null, null);
        assertFalse(validator.validate(emptyDto).isEmpty(), "No debe permitir nombres de cola vacíos");

        CreateQueueRequestDTO invalidCharsDto = new CreateQueueRequestDTO("cola con espacios!", true, false, false, null, null, null);
        assertFalse(validator.validate(invalidCharsDto).isEmpty(), "No debe permitir espacios ni caracteres especiales inválidos");

        CreateQueueRequestDTO validDto = new CreateQueueRequestDTO("pedidos.auditoria.queue", true, false, false, "pedidos.dlx.exchange", "auditoria.dlq.rk", 60000);
        assertTrue(validator.validate(validDto).isEmpty(), "Una cola bien formada debe pasar la validación");
    }

    @Test
    void debeRechazarExchangeConTipoInvalido() {
        CreateExchangeRequestDTO invalidType = new CreateExchangeRequestDTO("mi.exchange", "invalido", true, false);
        assertFalse(validator.validate(invalidType).isEmpty());

        CreateExchangeRequestDTO validTopic = new CreateExchangeRequestDTO("mi.topic.exchange", "topic", true, false);
        assertTrue(validator.validate(validTopic).isEmpty());
    }

    @Test
    void consumidorDebeConfirmarAckEnExitoYNackHaciaDlqEnErrorNoRecuperable() throws Exception {
        EventoMensajeLogRepository mockRepo = Mockito.mock(EventoMensajeLogRepository.class);
        Channel mockChannel = Mockito.mock(Channel.class);
        NotificacionConsumer consumer = new NotificacionConsumer(properties, mockRepo);

        NotificacionEventDTO okMsg = NotificacionEventDTO.builder()
                .messageId("NOTIF-001")
                .destinatario("cliente@duocuc.cl")
                .asunto("Pedido confirmado")
                .mensaje("Tu pedido va en camino")
                .simularErrorNoRecuperable(false)
                .build();

        consumer.procesarNotificacion(okMsg, mockChannel, 10L);
        verify(mockChannel).basicAck(10L, false);

        NotificacionEventDTO dlqMsg = NotificacionEventDTO.builder()
                .messageId("NOTIF-ERR-002")
                .destinatario("cliente@duocuc.cl")
                .asunto("Pedido fallido")
                .mensaje("Forzar DLQ")
                .simularErrorNoRecuperable(true)
                .build();

        consumer.procesarNotificacion(dlqMsg, mockChannel, 20L);
        verify(mockChannel).basicNack(20L, false, false);
    }
}
