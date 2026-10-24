import React, { useEffect, useState } from 'react';
import { useMsal } from '@azure/msal-react';
import { apiRequest } from '../apiClient';

interface Producto {
  id: number;
  nombre: string;
  precio: number;
  categoria?: { id: number; nombre: string };
}

interface Pedido {
  id: number;
  clienteEmail: string;
  productoNombre: string;
  cantidad: number;
  montoTotal: number;
  tipoDocumento: string;
  estado: string;
  fechaCreacion: string;
}

export function CatalogoPedidosView() {
  const { instance } = useMsal();
  const [productos, setProductos] = useState<Producto[]>([]);
  const [pedidos, setPedidos] = useState<Pedido[]>([]);
  const [feedback, setFeedback] = useState<string>('');
  const [errorMsg, setErrorMsg] = useState<string>('');

  // Formulario nuevo pedido
  const [clienteEmail, setClienteEmail] = useState('diego.estudiante@duocuc.cl');
  const [productoId, setProductoId] = useState<number>(1);
  const [cantidad, setCantidad] = useState<number>(1);
  const [tipoDocumento, setTipoDocumento] = useState('FACTURA');
  const [simularErrorDlq, setSimularErrorDlq] = useState(false);

  // Formulario nuevo producto
  const [nuevoProdNombre, setNuevoProdNombre] = useState('');
  const [nuevoProdPrecio, setNuevoProdPrecio] = useState('');

  const cargarDatos = async () => {
    try {
      setErrorMsg('');
      const [prods, peds] = await Promise.all([
        apiRequest<Producto[]>('/api/productos', instance),
        apiRequest<Pedido[]>('/api/pedidos', instance),
      ]);
      setProductos(prods);
      setPedidos(peds);
      if (prods.length > 0 && !prods.some((p) => p.id === productoId)) {
        setProductoId(prods[0].id);
      }
    } catch (err: unknown) {
      setErrorMsg(err instanceof Error ? err.message : 'Error cargando datos');
    }
  };

  useEffect(() => {
    void cargarDatos();
  }, []);

  const handleCrearPedido = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setErrorMsg('');
      setFeedback('');
      const res = await apiRequest<Record<string, unknown>>('/api/pedidos', instance, {
        method: 'POST',
        body: JSON.stringify({
          clienteEmail,
          productoId,
          cantidad,
          tipoDocumento,
          simularErrorDlq,
        }),
      });
      setFeedback(
        `Pedido creado en BD y 3 eventos enviados a RabbitMQ (Notificación, Ticket y Documento): ${JSON.stringify(
          res.eventosPublicados,
        )}`,
      );
      await cargarDatos();
    } catch (err: unknown) {
      setErrorMsg(err instanceof Error ? err.message : 'Error al crear pedido');
    }
  };

  const handleCrearProducto = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setErrorMsg('');
      setFeedback('');
      await apiRequest<Producto>('/api/productos', instance, {
        method: 'POST',
        body: JSON.stringify({
          nombre: nuevoProdNombre,
          precio: Number(nuevoProdPrecio),
        }),
      });
      setNuevoProdNombre('');
      setNuevoProdPrecio('');
      setFeedback('Producto registrado exitosamente en la base de datos.');
      await cargarDatos();
    } catch (err: unknown) {
      setErrorMsg(err instanceof Error ? err.message : 'Error al crear producto');
    }
  };

  return (
    <div style={{ display: 'grid', gap: '1.5rem' }}>
      {feedback && (
        <div
          style={{
            background: '#ecfdf5',
            border: '1px solid #10b981',
            color: '#065f46',
            padding: '0.85rem 1rem',
            borderRadius: '8px',
          }}
        >
          <strong>Éxito:</strong> {feedback}
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
          <strong>Error:</strong> {errorMsg}
        </div>
      )}

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(340px, 1fr))', gap: '1.5rem' }}>
        {/* Crear Pedido y Disparar 3 Eventos RabbitMQ */}
        <section
          style={{
            background: '#fff',
            padding: '1.25rem',
            borderRadius: '10px',
            boxShadow: '0 1px 4px rgba(0,0,0,0.08)',
          }}
        >
          <h3 style={{ marginTop: 0 }}>1. Registrar Pedido (Dispara las 3 Colas RabbitMQ)</h3>
          <p style={{ fontSize: '0.9rem', color: '#4b5563' }}>
            Al crear un pedido se guarda en la Base de Datos y se publican desacopladamente 3 tareas asíncronas:
            <strong> Notificación</strong> (Direct), <strong>Ticket</strong> (Direct) y <strong>Documento</strong>{' '}
            (Topic).
          </p>
          <form onSubmit={(e) => void handleCrearPedido(e)} style={{ display: 'grid', gap: '0.75rem' }}>
            <label>
              <strong>Email Cliente:</strong>
              <input
                type="email"
                value={clienteEmail}
                onChange={(e) => setClienteEmail(e.target.value)}
                required
                style={{ width: '100%', padding: '0.5rem', marginTop: '0.25rem', boxSizing: 'border-box' }}
              />
            </label>
            <label>
              <strong>Producto del Catálogo:</strong>
              <select
                value={productoId}
                onChange={(e) => setProductoId(Number(e.target.value))}
                style={{ width: '100%', padding: '0.5rem', marginTop: '0.25rem' }}
              >
                {productos.map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.nombre} (${p.precio.toLocaleString('es-CL')})
                  </option>
                ))}
              </select>
            </label>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
              <label>
                <strong>Cantidad:</strong>
                <input
                  type="number"
                  min={1}
                  value={cantidad}
                  onChange={(e) => setCantidad(Number(e.target.value))}
                  required
                  style={{ width: '100%', padding: '0.5rem', marginTop: '0.25rem', boxSizing: 'border-box' }}
                />
              </label>
              <label>
                <strong>Documento (Topic Exchange):</strong>
                <select
                  value={tipoDocumento}
                  onChange={(e) => setTipoDocumento(e.target.value)}
                  style={{ width: '100%', padding: '0.5rem', marginTop: '0.25rem' }}
                >
                  <option value="FACTURA">FACTURA (pedidos.documento.factura)</option>
                  <option value="BOLETA">BOLETA (pedidos.documento.boleta)</option>
                  <option value="GUIA_DESPACHO">GUIA_DESPACHO (pedidos.documento.guia_despacho)</option>
                </select>
              </label>
            </div>
            <label
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '0.5rem',
                background: '#fffbeb',
                padding: '0.6rem',
                borderRadius: '6px',
                border: '1px solid #f59e0b',
              }}
            >
              <input
                type="checkbox"
                checked={simularErrorDlq}
                onChange={(e) => setSimularErrorDlq(e.target.checked)}
              />
              <span>
                <strong>Simular fallo en consumidores (Enviar los 3 mensajes a sus DLQ con NACK)</strong>
              </span>
            </label>
            <button
              type="submit"
              style={{
                background: '#2563eb',
                color: '#fff',
                border: 'none',
                padding: '0.65rem 1rem',
                borderRadius: '6px',
                fontWeight: 600,
                cursor: 'pointer',
              }}
            >
              Confirmar Pedido y Publicar Eventos en RabbitMQ
            </button>
          </form>
        </section>

        {/* Catálogo de Productos en BD */}
        <section
          style={{
            background: '#fff',
            padding: '1.25rem',
            borderRadius: '10px',
            boxShadow: '0 1px 4px rgba(0,0,0,0.08)',
          }}
        >
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <h3 style={{ marginTop: 0 }}>2. Catálogo de Productos (JPA / BD)</h3>
            <button
              type="button"
              onClick={() => void cargarDatos()}
              style={{ padding: '0.35rem 0.75rem', cursor: 'pointer' }}
            >
              Actualizar
            </button>
          </div>
          <ul style={{ paddingLeft: '1.2rem', marginBottom: '1rem' }}>
            {productos.map((prod) => (
              <li key={prod.id} style={{ marginBottom: '0.35rem' }}>
                <strong>#{prod.id} - {prod.nombre}</strong> — ${prod.precio.toLocaleString('es-CL')}{' '}
                {prod.categoria ? `(${prod.categoria.nombre})` : ''}
              </li>
            ))}
          </ul>

          <hr style={{ border: 'none', borderTop: '1px solid #e5e7eb', margin: '1rem 0' }} />
          <h4 style={{ margin: '0 0 0.5rem 0' }}>Agregar Nuevo Producto</h4>
          <form onSubmit={(e) => void handleCrearProducto(e)} style={{ display: 'flex', gap: '0.5rem' }}>
            <input
              type="text"
              placeholder="Nombre del producto"
              value={nuevoProdNombre}
              onChange={(e) => setNuevoProdNombre(e.target.value)}
              required
              style={{ flex: 2, padding: '0.45rem' }}
            />
            <input
              type="number"
              placeholder="Precio"
              value={nuevoProdPrecio}
              onChange={(e) => setNuevoProdPrecio(e.target.value)}
              required
              style={{ flex: 1, padding: '0.45rem' }}
            />
            <button
              type="submit"
              style={{
                background: '#059669',
                color: '#fff',
                border: 'none',
                padding: '0.45rem 0.85rem',
                borderRadius: '6px',
                cursor: 'pointer',
              }}
            >
              Guardar
            </button>
          </form>
        </section>
      </div>

      {/* Tabla de Pedidos Registrados */}
      <section
        style={{
          background: '#fff',
          padding: '1.25rem',
          borderRadius: '10px',
          boxShadow: '0 1px 4px rgba(0,0,0,0.08)',
        }}
      >
        <h3 style={{ marginTop: 0 }}>Pedidos Registrados en Base de Datos ({pedidos.length})</h3>
        {pedidos.length === 0 ? (
          <p style={{ color: '#6b7280' }}>Aún no se han creado pedidos en esta sesión.</p>
        ) : (
          <div style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.9rem' }}>
              <thead>
                <tr style={{ background: '#f3f4f6', textAlign: 'left' }}>
                  <th style={{ padding: '0.6rem' }}>ID</th>
                  <th style={{ padding: '0.6rem' }}>Cliente</th>
                  <th style={{ padding: '0.6rem' }}>Producto</th>
                  <th style={{ padding: '0.6rem' }}>Cantidad</th>
                  <th style={{ padding: '0.6rem' }}>Total</th>
                  <th style={{ padding: '0.6rem' }}>Documento</th>
                  <th style={{ padding: '0.6rem' }}>Estado</th>
                </tr>
              </thead>
              <tbody>
                {pedidos.map((ped) => (
                  <tr key={ped.id} style={{ borderBottom: '1px solid #e5e7eb' }}>
                    <td style={{ padding: '0.6rem' }}>#{ped.id}</td>
                    <td style={{ padding: '0.6rem' }}>{ped.clienteEmail}</td>
                    <td style={{ padding: '0.6rem' }}>{ped.productoNombre}</td>
                    <td style={{ padding: '0.6rem' }}>{ped.cantidad}</td>
                    <td style={{ padding: '0.6rem' }}>${ped.montoTotal.toLocaleString('es-CL')}</td>
                    <td style={{ padding: '0.6rem' }}>{ped.tipoDocumento}</td>
                    <td style={{ padding: '0.6rem' }}>
                      <span
                        style={{
                          padding: '0.2rem 0.5rem',
                          borderRadius: '4px',
                          background: ped.estado.includes('DLQ') ? '#fee2e2' : '#dbeafe',
                          color: ped.estado.includes('DLQ') ? '#991b1b' : '#1e40af',
                          fontWeight: 600,
                          fontSize: '0.8rem',
                        }}
                      >
                        {ped.estado}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  );
}
