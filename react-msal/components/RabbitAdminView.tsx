import React, { useEffect, useState } from 'react';
import { useMsal } from '@azure/msal-react';
import { apiRequest } from '../apiClient';

interface QueueInfo {
  name: string;
  isDlq: boolean;
  existsInBroker: boolean;
  messageCount: number;
  consumerCount: number;
}

interface ExchangeInfo {
  name: string;
  type: string;
}

interface BindingInfo {
  exchangeName: string;
  queueName: string;
  routingKey: string;
}

interface TopologyResponse {
  queues: QueueInfo[];
  exchanges: ExchangeInfo[];
  bindings: BindingInfo[];
}

export function RabbitAdminView() {
  const { instance } = useMsal();
  const [topology, setTopology] = useState<TopologyResponse>({ queues: [], exchanges: [], bindings: [] });
  const [statusMsg, setStatusMsg] = useState('');
  const [errorMsg, setErrorMsg] = useState('');

  // Crear Cola
  const [queueName, setQueueName] = useState('pedidos.auditoria.queue');
  const [queueDlx, setQueueDlx] = useState('pedidos.dlx.exchange');
  const [queueDlxRk, setQueueDlxRk] = useState('pedidos.notificacion.dlq.rk');

  // Crear Exchange
  const [exchangeName, setExchangeName] = useState('pedidos.custom.exchange');
  const [exchangeType, setExchangeType] = useState('direct');

  // Crear Binding
  const [bindExchange, setBindExchange] = useState('pedidos.custom.exchange');
  const [bindQueue, setBindQueue] = useState('pedidos.auditoria.queue');
  const [bindRoutingKey, setBindRoutingKey] = useState('pedidos.auditoria.rk');

  const cargarTopologia = async () => {
    try {
      const data = await apiRequest<TopologyResponse>('/queues', instance);
      setTopology(data);
    } catch (err: unknown) {
      setErrorMsg(err instanceof Error ? err.message : 'Error consultando topología RabbitMQ');
    }
  };

  useEffect(() => {
    void cargarTopologia();
  }, []);

  const handleCrearCola = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setErrorMsg('');
      setStatusMsg('');
      const res = await apiRequest<{ message: string }>('/queues', instance, {
        method: 'POST',
        body: JSON.stringify({
          name: queueName,
          durable: true,
          exclusive: false,
          autoDelete: false,
          deadLetterExchange: queueDlx,
          deadLetterRoutingKey: queueDlxRk,
        }),
      });
      setStatusMsg(res.message);
      await cargarTopologia();
    } catch (err: unknown) {
      setErrorMsg(err instanceof Error ? err.message : 'Error creando cola');
    }
  };

  const handleProbarValidacionInvalida = async () => {
    try {
      setErrorMsg('');
      setStatusMsg('');
      await apiRequest('/queues', instance, {
        method: 'POST',
        body: JSON.stringify({
          name: '',
          durable: true,
        }),
      });
    } catch (err: unknown) {
      setErrorMsg(
        `[Validación DTO @Valid Correcta - HTTP 400]: ${
          err instanceof Error ? err.message : 'Parámetros rechazados'
        }`,
      );
    }
  };

  const handleEliminarCola = async (name: string) => {
    try {
      setErrorMsg('');
      setStatusMsg('');
      const res = await apiRequest<{ message: string }>(`/queues/${encodeURIComponent(name)}`, instance, {
        method: 'DELETE',
      });
      setStatusMsg(res.message);
      await cargarTopologia();
    } catch (err: unknown) {
      setErrorMsg(err instanceof Error ? err.message : 'Error eliminando cola');
    }
  };

  const handlePurgarCola = async (name: string) => {
    try {
      setErrorMsg('');
      setStatusMsg('');
      const res = await apiRequest<{ message: string }>(`/queues/${encodeURIComponent(name)}/messages`, instance, {
        method: 'DELETE',
      });
      setStatusMsg(res.message);
      await cargarTopologia();
    } catch (err: unknown) {
      setErrorMsg(err instanceof Error ? err.message : 'Error purgando cola');
    }
  };

  const handleCrearExchange = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setErrorMsg('');
      setStatusMsg('');
      const res = await apiRequest<{ message: string }>('/exchanges', instance, {
        method: 'POST',
        body: JSON.stringify({
          name: exchangeName,
          type: exchangeType,
          durable: true,
          autoDelete: false,
        }),
      });
      setStatusMsg(res.message);
      await cargarTopologia();
    } catch (err: unknown) {
      setErrorMsg(err instanceof Error ? err.message : 'Error creando exchange');
    }
  };

  const handleEliminarExchange = async (name: string) => {
    try {
      setErrorMsg('');
      setStatusMsg('');
      const res = await apiRequest<{ message: string }>(`/exchanges/${encodeURIComponent(name)}`, instance, {
        method: 'DELETE',
      });
      setStatusMsg(res.message);
      await cargarTopologia();
    } catch (err: unknown) {
      setErrorMsg(err instanceof Error ? err.message : 'Error eliminando exchange');
    }
  };

  const handleCrearBinding = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setErrorMsg('');
      setStatusMsg('');
      const res = await apiRequest<{ message: string }>('/bindings', instance, {
        method: 'POST',
        body: JSON.stringify({
          queueName: bindQueue,
          exchangeName: bindExchange,
          routingKey: bindRoutingKey,
        }),
      });
      setStatusMsg(res.message);
      await cargarTopologia();
    } catch (err: unknown) {
      setErrorMsg(err instanceof Error ? err.message : 'Error creando binding');
    }
  };

  return (
    <div style={{ display: 'grid', gap: '1.25rem' }}>
      {statusMsg && (
        <div
          style={{
            background: '#ecfdf5',
            border: '1px solid #10b981',
            color: '#065f46',
            padding: '0.85rem 1rem',
            borderRadius: '8px',
          }}
        >
          <strong>RabbitAdminService:</strong> {statusMsg}
        </div>
      )}
      {errorMsg && (
        <div
          style={{
            background: '#fef2f2',
            border: '1px solid #ef4444',
            color: '#991b1b',
            padding: '0.85rem 1rem',
            borderRadius: '8px',
          }}
        >
          <strong>Respuesta de Validación / Error:</strong> {errorMsg}
        </div>
      )}

      {/* Formularios de creación */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))', gap: '1rem' }}>
        {/* Crear Cola */}
        <section
          style={{
            background: '#fff',
            padding: '1.1rem',
            borderRadius: '10px',
            boxShadow: '0 1px 4px rgba(0,0,0,0.08)',
          }}
        >
          <h3 style={{ marginTop: 0 }}>POST /queues (Crear Cola)</h3>
          <form onSubmit={(e) => void handleCrearCola(e)} style={{ display: 'grid', gap: '0.55rem' }}>
            <input
              type="text"
              value={queueName}
              onChange={(e) => setQueueName(e.target.value)}
              placeholder="Nombre de la cola (ej. pedidos.nueva.queue)"
              style={{ padding: '0.45rem' }}
            />
            <input
              type="text"
              value={queueDlx}
              onChange={(e) => setQueueDlx(e.target.value)}
              placeholder="Dead Letter Exchange (opcional)"
              style={{ padding: '0.45rem' }}
            />
            <input
              type="text"
              value={queueDlxRk}
              onChange={(e) => setQueueDlxRk(e.target.value)}
              placeholder="DLQ Routing Key (opcional)"
              style={{ padding: '0.45rem' }}
            />
            <button
              type="submit"
              style={{
                background: '#2563eb',
                color: '#fff',
                border: 'none',
                padding: '0.5rem',
                borderRadius: '6px',
                cursor: 'pointer',
                fontWeight: 600,
              }}
            >
              Crear Cola en RabbitMQ
            </button>
            <button
              type="button"
              onClick={() => void handleProbarValidacionInvalida()}
              style={{
                background: '#f59e0b',
                color: '#fff',
                border: 'none',
                padding: '0.45rem',
                borderRadius: '6px',
                cursor: 'pointer',
                fontSize: '0.82rem',
              }}
            >
              Probar Validación DTO (Enviar nombre vacío -&gt; 400)
            </button>
          </form>
        </section>

        {/* Crear Exchange */}
        <section
          style={{
            background: '#fff',
            padding: '1.1rem',
            borderRadius: '10px',
            boxShadow: '0 1px 4px rgba(0,0,0,0.08)',
          }}
        >
          <h3 style={{ marginTop: 0 }}>POST /exchanges (Crear Exchange)</h3>
          <form onSubmit={(e) => void handleCrearExchange(e)} style={{ display: 'grid', gap: '0.55rem' }}>
            <input
              type="text"
              value={exchangeName}
              onChange={(e) => setExchangeName(e.target.value)}
              placeholder="Nombre del Exchange"
              style={{ padding: '0.45rem' }}
            />
            <select
              value={exchangeType}
              onChange={(e) => setExchangeType(e.target.value)}
              style={{ padding: '0.45rem' }}
            >
              <option value="direct">direct</option>
              <option value="topic">topic</option>
              <option value="fanout">fanout</option>
              <option value="headers">headers</option>
            </select>
            <button
              type="submit"
              style={{
                background: '#7c3aed',
                color: '#fff',
                border: 'none',
                padding: '0.5rem',
                borderRadius: '6px',
                cursor: 'pointer',
                fontWeight: 600,
              }}
            >
              Crear Exchange
            </button>
          </form>
        </section>

        {/* Crear Binding */}
        <section
          style={{
            background: '#fff',
            padding: '1.1rem',
            borderRadius: '10px',
            boxShadow: '0 1px 4px rgba(0,0,0,0.08)',
          }}
        >
          <h3 style={{ marginTop: 0 }}>POST /bindings (Crear Binding)</h3>
          <form onSubmit={(e) => void handleCrearBinding(e)} style={{ display: 'grid', gap: '0.55rem' }}>
            <input
              type="text"
              value={bindExchange}
              onChange={(e) => setBindExchange(e.target.value)}
              placeholder="Exchange de origen"
              style={{ padding: '0.45rem' }}
            />
            <input
              type="text"
              value={bindQueue}
              onChange={(e) => setBindQueue(e.target.value)}
              placeholder="Cola de destino"
              style={{ padding: '0.45rem' }}
            />
            <input
              type="text"
              value={bindRoutingKey}
              onChange={(e) => setBindRoutingKey(e.target.value)}
              placeholder="Routing Key"
              style={{ padding: '0.45rem' }}
            />
            <button
              type="submit"
              style={{
                background: '#059669',
                color: '#fff',
                border: 'none',
                padding: '0.5rem',
                borderRadius: '6px',
                cursor: 'pointer',
                fontWeight: 600,
              }}
            >
              Vincular Exchange con Cola
            </button>
          </form>
        </section>
      </div>

      {/* Listado de Colas y DLQs en Vivo */}
      <section
        style={{
          background: '#fff',
          padding: '1.25rem',
          borderRadius: '10px',
          boxShadow: '0 1px 4px rgba(0,0,0,0.08)',
        }}
      >
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.75rem' }}>
          <h3 style={{ margin: 0 }}>Colas Activas y Dead Letter Queues (DLQ) en el Clúster RabbitMQ</h3>
          <button
            type="button"
            onClick={() => void cargarTopologia()}
            style={{ padding: '0.4rem 0.85rem', cursor: 'pointer', fontWeight: 600 }}
          >
            Refrescar Estado del Broker
          </button>
        </div>

        <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.9rem' }}>
          <thead>
            <tr style={{ background: '#f3f4f6', textAlign: 'left' }}>
              <th style={{ padding: '0.6rem' }}>Nombre de Cola</th>
              <th style={{ padding: '0.6rem' }}>Tipo</th>
              <th style={{ padding: '0.6rem' }}>Mensajes Encolados (Ready)</th>
              <th style={{ padding: '0.6rem' }}>Consumidores Activos</th>
              <th style={{ padding: '0.6rem' }}>Acciones REST</th>
            </tr>
          </thead>
          <tbody>
            {topology.queues.map((q) => (
              <tr key={q.name} style={{ borderBottom: '1px solid #e5e7eb' }}>
                <td style={{ padding: '0.6rem', fontFamily: 'monospace', fontWeight: 600 }}>{q.name}</td>
                <td style={{ padding: '0.6rem' }}>
                  <span
                    style={{
                      padding: '0.2rem 0.5rem',
                      borderRadius: '4px',
                      background: q.isDlq ? '#fee2e2' : '#dbeafe',
                      color: q.isDlq ? '#991b1b' : '#1e40af',
                      fontSize: '0.78rem',
                      fontWeight: 600,
                    }}
                  >
                    {q.isDlq ? 'DEAD LETTER QUEUE (DLQ)' : 'COLA PRINCIPAL'}
                  </span>
                </td>
                <td style={{ padding: '0.6rem', fontWeight: 700, color: q.messageCount > 0 ? '#d97706' : '#111827' }}>
                  {q.messageCount}
                </td>
                <td style={{ padding: '0.6rem' }}>{q.consumerCount}</td>
                <td style={{ padding: '0.6rem', display: 'flex', gap: '0.5rem' }}>
                  <button
                    type="button"
                    onClick={() => void handlePurgarCola(q.name)}
                    style={{
                      background: '#f59e0b',
                      color: '#fff',
                      border: 'none',
                      padding: '0.3rem 0.6rem',
                      borderRadius: '4px',
                      cursor: 'pointer',
                      fontSize: '0.8rem',
                    }}
                  >
                    Purgar Mensajes
                  </button>
                  <button
                    type="button"
                    onClick={() => void handleEliminarCola(q.name)}
                    style={{
                      background: '#dc2626',
                      color: '#fff',
                      border: 'none',
                      padding: '0.3rem 0.6rem',
                      borderRadius: '4px',
                      cursor: 'pointer',
                      fontSize: '0.8rem',
                    }}
                  >
                    DELETE /queues/{q.name}
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>

      {/* Exchanges y Bindings */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1.25rem' }}>
        <section
          style={{
            background: '#fff',
            padding: '1.1rem',
            borderRadius: '10px',
            boxShadow: '0 1px 4px rgba(0,0,0,0.08)',
          }}
        >
          <h3 style={{ marginTop: 0 }}>Exchanges Configurados (Direct, Topic y DLX)</h3>
          <ul style={{ paddingLeft: '1.2rem' }}>
            {topology.exchanges.map((ex) => (
              <li
                key={ex.name}
                style={{ marginBottom: '0.5rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}
              >
                <span>
                  <code>{ex.name}</code> — <strong>{ex.type}</strong>
                </span>
                <button
                  type="button"
                  onClick={() => void handleEliminarExchange(ex.name)}
                  style={{
                    background: '#ef4444',
                    color: '#fff',
                    border: 'none',
                    padding: '0.2rem 0.5rem',
                    borderRadius: '4px',
                    cursor: 'pointer',
                    fontSize: '0.75rem',
                  }}
                >
                  Eliminar
                </button>
              </li>
            ))}
          </ul>
        </section>

        <section
          style={{
            background: '#fff',
            padding: '1.1rem',
            borderRadius: '10px',
            boxShadow: '0 1px 4px rgba(0,0,0,0.08)',
          }}
        >
          <h3 style={{ marginTop: 0 }}>Bindings Activos</h3>
          <ul style={{ paddingLeft: '1.2rem', fontSize: '0.88rem' }}>
            {topology.bindings.map((b, idx) => (
              <li key={`${b.exchangeName}-${b.queueName}-${idx}`} style={{ marginBottom: '0.4rem' }}>
                <code>{b.exchangeName}</code> &rarr; <code>{b.queueName}</code> (rk: <code>{b.routingKey}</code>)
              </li>
            ))}
          </ul>
        </section>
      </div>
    </div>
  );
}
