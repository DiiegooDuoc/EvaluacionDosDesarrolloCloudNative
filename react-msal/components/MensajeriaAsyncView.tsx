import React, { useState } from 'react';
import { useMsal } from '@azure/msal-react';
import { apiRequest } from '../apiClient';

type ModoSimulacion = 'ACK_OK' | 'RETRY_RECOVERABLE' | 'NACK_DLQ';

export function MensajeriaAsyncView() {
  const { instance } = useMsal();
  const [resultado, setResultado] = useState<string>('');
  const [errorMsg, setErrorMsg] = useState<string>('');

  // Caso 1: Notificación
  const [notifEmail, setNotifEmail] = useState('cliente@duocuc.cl');
  const [notifAsunto, setNotifAsunto] = useState('Aviso de Despacho Programado');
  const [notifMensaje, setNotifMensaje] = useState('Su pedido ha salido de bodega central.');
  const [notifModo, setNotifModo] = useState<ModoSimulacion>('ACK_OK');

  // Caso 2: Ticket
  const [ticketCliente, setTicketCliente] = useState('Empresa Constructora S.A.');
  const [ticketArea, setTicketArea] = useState('SOPORTE_POSTVENTA');
  const [ticketPrioridad, setTicketPrioridad] = useState('ALTA');
  const [ticketDesc, setTicketDesc] = useState('Solicitud de seguimiento de orden #104');
  const [ticketModo, setTicketModo] = useState<ModoSimulacion>('ACK_OK');

  // Caso 3: Documento
  const [docTipo, setDocTipo] = useState('FACTURA');
  const [docRut, setDocRut] = useState('76.123.456-7');
  const [docGlosa, setDocGlosa] = useState('Equipamiento Computacional Cloud');
  const [docMonto, setDocMonto] = useState<number>(450000);
  const [docModo, setDocModo] = useState<ModoSimulacion>('ACK_OK');

  const enviarNotificacion = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setErrorMsg('');
      const res = await apiRequest<Record<string, unknown>>('/api/mensajeria/notificaciones', instance, {
        method: 'POST',
        body: JSON.stringify({
          destinatario: notifEmail,
          asunto: notifAsunto,
          mensaje: notifMensaje,
          canal: 'EMAIL',
          simularErrorRecuperable: notifModo === 'RETRY_RECOVERABLE',
          simularErrorNoRecuperable: notifModo === 'NACK_DLQ',
        }),
      });
      setResultado(`Notificación publicada en DirectExchange -> pedidos.notificaciones.queue: ${JSON.stringify(res)}`);
    } catch (err: unknown) {
      setErrorMsg(err instanceof Error ? err.message : 'Error enviando notificación');
    }
  };

  const enviarTicket = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setErrorMsg('');
      const res = await apiRequest<Record<string, unknown>>('/api/mensajeria/tickets', instance, {
        method: 'POST',
        body: JSON.stringify({
          cliente: ticketCliente,
          areaResponsable: ticketArea,
          prioridad: ticketPrioridad,
          descripcion: ticketDesc,
          simularErrorRecuperable: ticketModo === 'RETRY_RECOVERABLE',
          simularErrorNoRecuperable: ticketModo === 'NACK_DLQ',
        }),
      });
      setResultado(`Ticket publicado en DirectExchange -> pedidos.tickets.queue: ${JSON.stringify(res)}`);
    } catch (err: unknown) {
      setErrorMsg(err instanceof Error ? err.message : 'Error enviando ticket');
    }
  };

  const enviarDocumento = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setErrorMsg('');
      const res = await apiRequest<Record<string, unknown>>('/api/mensajeria/documentos', instance, {
        method: 'POST',
        body: JSON.stringify({
          tipoDocumento: docTipo,
          rutReceptor: docRut,
          detalleGlosa: docGlosa,
          montoTotal: docMonto,
          simularErrorRecuperable: docModo === 'RETRY_RECOVERABLE',
          simularErrorNoRecuperable: docModo === 'NACK_DLQ',
        }),
      });
      setResultado(`Documento publicado en TopicExchange -> pedidos.documentos.queue: ${JSON.stringify(res)}`);
    } catch (err: unknown) {
      setErrorMsg(err instanceof Error ? err.message : 'Error enviando documento');
    }
  };

  return (
    <div style={{ display: 'grid', gap: '1.25rem' }}>
      {resultado && (
        <div
          style={{
            background: '#ecfdf5',
            border: '1px solid #10b981',
            color: '#065f46',
            padding: '0.85rem 1rem',
            borderRadius: '8px',
          }}
        >
          <strong>Mensaje Publicado:</strong> {resultado}
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
          <strong>Error Validación / API:</strong> {errorMsg}
        </div>
      )}

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(310px, 1fr))', gap: '1.25rem' }}>
        {/* Caso de Uso 1: Notificaciones */}
        <section
          style={{
            background: '#fff',
            padding: '1.2rem',
            borderRadius: '10px',
            boxShadow: '0 1px 4px rgba(0,0,0,0.08)',
            borderTop: '4px solid #2563eb',
          }}
        >
          <h3 style={{ marginTop: 0 }}>Caso 1: Notificaciones (Direct)</h3>
          <p style={{ fontSize: '0.82rem', color: '#4b5563' }}>
            Cola: <code>pedidos.notificaciones.queue</code>
            <br />
            DLQ: <code>pedidos.notificaciones.dlq</code>
          </p>
          <form onSubmit={(e) => void enviarNotificacion(e)} style={{ display: 'grid', gap: '0.6rem' }}>
            <input
              type="email"
              value={notifEmail}
              onChange={(e) => setNotifEmail(e.target.value)}
              placeholder="Correo destinatario"
              required
              style={{ padding: '0.45rem' }}
            />
            <input
              type="text"
              value={notifAsunto}
              onChange={(e) => setNotifAsunto(e.target.value)}
              placeholder="Asunto"
              required
              style={{ padding: '0.45rem' }}
            />
            <textarea
              value={notifMensaje}
              onChange={(e) => setNotifMensaje(e.target.value)}
              rows={2}
              required
              style={{ padding: '0.45rem' }}
            />
            <select
              value={notifModo}
              onChange={(e) => setNotifModo(e.target.value as ModoSimulacion)}
              style={{ padding: '0.45rem', fontWeight: 600 }}
            >
              <option value="ACK_OK">1. Éxito &rarr; Confirmar con basicAck()</option>
              <option value="RETRY_RECOVERABLE">2. Error Recuperable &rarr; Reintentos (requeue) + DLQ</option>
              <option value="NACK_DLQ">3. Error No Recuperable &rarr; basicNack() directo a DLQ</option>
            </select>
            <button
              type="submit"
              style={{
                background: '#2563eb',
                color: '#fff',
                border: 'none',
                padding: '0.55rem',
                borderRadius: '6px',
                cursor: 'pointer',
                fontWeight: 600,
              }}
            >
              Enviar a Cola de Notificaciones
            </button>
          </form>
        </section>

        {/* Caso de Uso 2: Tickets */}
        <section
          style={{
            background: '#fff',
            padding: '1.2rem',
            borderRadius: '10px',
            boxShadow: '0 1px 4px rgba(0,0,0,0.08)',
            borderTop: '4px solid #7c3aed',
          }}
        >
          <h3 style={{ marginTop: 0 }}>Caso 2: Tickets (Direct)</h3>
          <p style={{ fontSize: '0.82rem', color: '#4b5563' }}>
            Cola: <code>pedidos.tickets.queue</code>
            <br />
            DLQ: <code>pedidos.tickets.dlq</code>
          </p>
          <form onSubmit={(e) => void enviarTicket(e)} style={{ display: 'grid', gap: '0.6rem' }}>
            <input
              type="text"
              value={ticketCliente}
              onChange={(e) => setTicketCliente(e.target.value)}
              placeholder="Cliente"
              required
              style={{ padding: '0.45rem' }}
            />
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.5rem' }}>
              <input
                type="text"
                value={ticketArea}
                onChange={(e) => setTicketArea(e.target.value)}
                placeholder="Área"
                required
                style={{ padding: '0.45rem' }}
              />
              <select
                value={ticketPrioridad}
                onChange={(e) => setTicketPrioridad(e.target.value)}
                style={{ padding: '0.45rem' }}
              >
                <option value="ALTA">ALTA</option>
                <option value="MEDIA">MEDIA</option>
                <option value="BAJA">BAJA</option>
                <option value="CRITICA">CRITICA</option>
              </select>
            </div>
            <textarea
              value={ticketDesc}
              onChange={(e) => setTicketDesc(e.target.value)}
              rows={2}
              required
              style={{ padding: '0.45rem' }}
            />
            <select
              value={ticketModo}
              onChange={(e) => setTicketModo(e.target.value as ModoSimulacion)}
              style={{ padding: '0.45rem', fontWeight: 600 }}
            >
              <option value="ACK_OK">1. Éxito &rarr; Confirmar con basicAck()</option>
              <option value="RETRY_RECOVERABLE">2. Error Recuperable &rarr; Reintentos (requeue) + DLQ</option>
              <option value="NACK_DLQ">3. Error No Recuperable &rarr; basicNack() directo a DLQ</option>
            </select>
            <button
              type="submit"
              style={{
                background: '#7c3aed',
                color: '#fff',
                border: 'none',
                padding: '0.55rem',
                borderRadius: '6px',
                cursor: 'pointer',
                fontWeight: 600,
              }}
            >
              Enviar a Cola de Tickets
            </button>
          </form>
        </section>

        {/* Caso de Uso 3: Documentos (Topic Exchange) */}
        <section
          style={{
            background: '#fff',
            padding: '1.2rem',
            borderRadius: '10px',
            boxShadow: '0 1px 4px rgba(0,0,0,0.08)',
            borderTop: '4px solid #059669',
          }}
        >
          <h3 style={{ marginTop: 0 }}>Caso 3: Documentos (Topic)</h3>
          <p style={{ fontSize: '0.82rem', color: '#4b5563' }}>
            Cola: <code>pedidos.documentos.queue</code> (<code>pedidos.documento.#</code>)
            <br />
            DLQ: <code>pedidos.documentos.dlq</code>
          </p>
          <form onSubmit={(e) => void enviarDocumento(e)} style={{ display: 'grid', gap: '0.6rem' }}>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.5rem' }}>
              <select value={docTipo} onChange={(e) => setDocTipo(e.target.value)} style={{ padding: '0.45rem' }}>
                <option value="FACTURA">FACTURA</option>
                <option value="BOLETA">BOLETA</option>
                <option value="GUIA_DESPACHO">GUIA_DESPACHO</option>
              </select>
              <input
                type="text"
                value={docRut}
                onChange={(e) => setDocRut(e.target.value)}
                placeholder="RUT Receptor"
                required
                style={{ padding: '0.45rem' }}
              />
            </div>
            <input
              type="text"
              value={docGlosa}
              onChange={(e) => setDocGlosa(e.target.value)}
              placeholder="Glosa documento"
              required
              style={{ padding: '0.45rem' }}
            />
            <input
              type="number"
              value={docMonto}
              onChange={(e) => setDocMonto(Number(e.target.value))}
              placeholder="Monto Total"
              required
              style={{ padding: '0.45rem' }}
            />
            <select
              value={docModo}
              onChange={(e) => setDocModo(e.target.value as ModoSimulacion)}
              style={{ padding: '0.45rem', fontWeight: 600 }}
            >
              <option value="ACK_OK">1. Éxito &rarr; Confirmar con basicAck()</option>
              <option value="RETRY_RECOVERABLE">2. Error Recuperable &rarr; Reintentos (requeue) + DLQ</option>
              <option value="NACK_DLQ">3. Error No Recuperable &rarr; basicNack() directo a DLQ</option>
            </select>
            <button
              type="submit"
              style={{
                background: '#059669',
                color: '#fff',
                border: 'none',
                padding: '0.55rem',
                borderRadius: '6px',
                cursor: 'pointer',
                fontWeight: 600,
              }}
            >
              Enviar a Topic Exchange (Documentos)
            </button>
          </form>
        </section>
      </div>
    </div>
  );
}
