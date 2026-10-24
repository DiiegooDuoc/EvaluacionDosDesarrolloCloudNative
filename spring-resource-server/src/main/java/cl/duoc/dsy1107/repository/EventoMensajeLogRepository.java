package cl.duoc.dsy1107.repository;

import cl.duoc.dsy1107.entity.EventoMensajeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventoMensajeLogRepository extends JpaRepository<EventoMensajeLog, Long> {
    List<EventoMensajeLog> findTop50ByOrderByFechaEventoDesc();
    List<EventoMensajeLog> findByEstadoOrderByFechaEventoDesc(String estado);
}
