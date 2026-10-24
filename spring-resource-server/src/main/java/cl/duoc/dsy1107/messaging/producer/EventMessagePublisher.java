package cl.duoc.dsy1107.messaging.producer;

import cl.duoc.dsy1107.dto.DocumentoEventDTO;
import cl.duoc.dsy1107.dto.NotificacionEventDTO;
import cl.duoc.dsy1107.dto.TicketEventDTO;

public interface EventMessagePublisher {
    NotificacionEventDTO publicarNotificacion(NotificacionEventDTO event);
    TicketEventDTO publicarTicket(TicketEventDTO event);
    DocumentoEventDTO publicarDocumento(DocumentoEventDTO event);
}
