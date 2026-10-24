import React, { useEffect, useState } from 'react';
import { useMsal } from '@azure/msal-react';
import { apiRequest } from '../apiClient';

interface EventoLog {
  id: number;
  messageId: string;
  dominio: string;
  exchange: string;
  routingKey: string;
  colaDestino: string;
  estado: string;
  detalle: string;
  fechaEvento: string;
}

export function EventosLogView() {
  const { instance } = useMsal();
  const [eventos, setEventos] = useState<EventoLog[]>([]);
  const [errorMsg, setErrorMsg] = useState('');

  const cargarEventos = async () => {
    try {
      setErrorMsg('');
      const data = await apiRequest<EventoLog[]>('/api/mensajeria/eventos', instance);
      setEventos(data);
    } catch (err: unknown) {
      setErrorMsg(err instanceof Error ? err.message : 'Error consultando bitácora de eventos');
    }
  };

  useEffect(() => {
    void cargarEventos();
  }, []);

  const badgeStyle = (estado: string): React.CSSProperties => {
    if (estado.includes('ACK')) {
      return { background: '#d1fae5', color: '#065f46' };
    }
    if (estado.includes('DLQ') || estado.includes('ERROR')) {
      return { background: '#fee2e2', color: '#991b1b' };
    }
    if (estado.includes('REINTENTO')) {
      return { background: '#fef3c7', color: '#92400e' };
    }
    return { background: '#dbeafe', color: '#1e40af' };
  };

  return (
    <section
      style={{
        background: '#fff',
        padding: '1.25rem',
        borderRadius: '10px',
        boxShadow: '0 1px 4px rgba(0,0,0,0.08)',
      }}
    >
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
        <div>
          <h3 style={{ margin: 0 }}>Trazabilidad de Mensajes, Confirmaciones (ACK/NACK) y Envíos a DLQ</h3>
          <p style={{ margin: '0.25rem 0 0 0', fontSize: '0.85rem', color: '#4b5563' }}>
            Registro en vivo de productores y consumidores (@RabbitListener) agrupados por dominio funcional.
          </p>
        </div>
        <button
          type="button"
          onClick={() => void cargarEventos()}
          style={{ padding: '0.45rem 0.9rem', cursor: 'pointer', fontWeight: 600 }}
        >
          Actualizar Logs
        </button>
      </div>

      {errorMsg && <p style={{ color: '#dc2626' }}>{errorMsg}</p>}

      {eventos.length === 0 ? (
        <p style={{ color: '#6b7280' }}>No hay eventos registrados aún. Envía un pedido o prueba una cola.</p>
      ) : (
        <div style={{ overflowX: 'auto' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.86rem' }}>
            <thead>
              <tr style={{ background: '#f3f4f6', textAlign: 'left' }}>
                <th style={{ padding: '0.55rem' }}>Message ID</th>
                <th style={{ padding: '0.55rem' }}>Dominio</th>
                <th style={{ padding: '0.55rem' }}>Exchange</th>
                <th style={{ padding: '0.55rem' }}>Routing Key</th>
                <th style={{ padding: '0.55rem' }}>Cola</th>
                <th style={{ padding: '0.55rem' }}>Estado / Confirmación</th>
                <th style={{ padding: '0.55rem' }}>Detalle</th>
              </tr>
            </thead>
            <tbody>
              {eventos.map((ev) => (
                <tr key={ev.id} style={{ borderBottom: '1px solid #e5e7eb' }}>
                  <td style={{ padding: '0.55rem', fontFamily: 'monospace', fontWeight: 600 }}>{ev.messageId}</td>
                  <td style={{ padding: '0.55rem', fontWeight: 600 }}>{ev.dominio}</td>
                  <td style={{ padding: '0.55rem', fontFamily: 'monospace' }}>{ev.exchange}</td>
                  <td style={{ padding: '0.55rem', fontFamily: 'monospace' }}>{ev.routingKey}</td>
                  <td style={{ padding: '0.55rem', fontFamily: 'monospace' }}>{ev.colaDestino}</td>
                  <td style={{ padding: '0.55rem' }}>
                    <span
                      style={{
                        ...badgeStyle(ev.estado),
                        padding: '0.2rem 0.5rem',
                        borderRadius: '4px',
                        fontWeight: 700,
                        fontSize: '0.78rem',
                      }}
                    >
                      {ev.estado}
                    </span>
                  </td>
                  <td style={{ padding: '0.55rem' }}>{ev.detalle}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}
