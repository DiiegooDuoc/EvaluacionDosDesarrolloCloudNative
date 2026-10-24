import React, { useState } from 'react';
import { AuthenticatedTemplate, useMsal } from '@azure/msal-react';
import { ProtectedRoute } from './ProtectedRoute';
import { CatalogoPedidosView } from './components/CatalogoPedidosView';
import { MensajeriaAsyncView } from './components/MensajeriaAsyncView';
import { RabbitAdminView } from './components/RabbitAdminView';
import { EventosLogView } from './components/EventosLogView';

type TabKey = 'PEDIDOS' | 'MENSAJERIA' | 'RABBIT_ADMIN' | 'LOGS';

export function App() {
  const { instance, accounts } = useMsal();
  const [activeTab, setActiveTab] = useState<TabKey>('PEDIDOS');
  const [jwtPreview, setJwtPreview] = useState<string>('');

  const handleLogout = () => {
    instance.logoutPopup().catch((e) => console.error('Error de logout:', e));
  };

  const mostrarTokenJwt = async () => {
    try {
      const scope = import.meta.env.VITE_API_SCOPE;
      const res = await instance.acquireTokenSilent({
        scopes: [scope],
        account: accounts[0],
      });
      setJwtPreview(res.accessToken);
    } catch (e) {
      console.error('Error obteniendo token:', e);
    }
  };

  const tabButtonStyle = (tab: TabKey): React.CSSProperties => ({
    padding: '0.65rem 1.15rem',
    border: 'none',
    borderRadius: '6px',
    cursor: 'pointer',
    fontWeight: 600,
    background: activeTab === tab ? '#1d4ed8' : '#e5e7eb',
    color: activeTab === tab ? '#ffffff' : '#1f2937',
  });

  return (
    <div style={{ maxWidth: '1240px', margin: '0 auto', padding: '1.5rem', fontFamily: 'system-ui, sans-serif' }}>
      <header
        style={{
          background: '#1e293b',
          color: '#ffffff',
          padding: '1.25rem 1.5rem',
          borderRadius: '10px',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          flexWrap: 'wrap',
          gap: '1rem',
        }}
      >
        <div>
          <h1 style={{ margin: 0, fontSize: '1.45rem' }}>
            Sistema Cloud Native: Gestión de Pedidos &amp; Mensajería RabbitMQ
          </h1>
          <p style={{ margin: '0.25rem 0 0 0', fontSize: '0.88rem', color: '#cbd5e1' }}>
            DSY1107 Desarrollo Cloud Native I — OAuth 2.0 / OIDC (Entra ID) + Clúster RabbitMQ (Direct, Topic &amp; DLQ)
          </p>
        </div>

        <AuthenticatedTemplate>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            <span style={{ fontSize: '0.88rem', background: '#334155', padding: '0.4rem 0.75rem', borderRadius: '6px' }}>
              Usuario: <strong>{accounts[0]?.username}</strong>
            </span>
            <button
              type="button"
              onClick={() => void mostrarTokenJwt()}
              style={{
                background: '#3b82f6',
                color: '#fff',
                border: 'none',
                padding: '0.45rem 0.8rem',
                borderRadius: '6px',
                cursor: 'pointer',
                fontSize: '0.82rem',
              }}
            >
              Ver JWT
            </button>
            <button
              type="button"
              onClick={handleLogout}
              style={{
                background: '#ef4444',
                color: '#fff',
                border: 'none',
                padding: '0.45rem 0.8rem',
                borderRadius: '6px',
                cursor: 'pointer',
                fontSize: '0.82rem',
              }}
            >
              Cerrar Sesión
            </button>
          </div>
        </AuthenticatedTemplate>
      </header>

      {jwtPreview && (
        <div
          style={{
            background: '#eff6ff',
            border: '1px solid #93c5fd',
            padding: '0.75rem 1rem',
            borderRadius: '8px',
            marginTop: '1rem',
            fontSize: '0.8rem',
            wordBreak: 'break-all',
          }}
        >
          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.35rem' }}>
            <strong>Access Token JWT (Bearer enviado al API Manager / Backend):</strong>
            <button type="button" onClick={() => setJwtPreview('')} style={{ cursor: 'pointer' }}>
              Ocultar
            </button>
          </div>
          <code>{jwtPreview}</code>
        </div>
      )}

      <ProtectedRoute>
        <AuthenticatedTemplate>
          <nav style={{ display: 'flex', gap: '0.6rem', margin: '1.25rem 0', flexWrap: 'wrap' }}>
            <button type="button" style={tabButtonStyle('PEDIDOS')} onClick={() => setActiveTab('PEDIDOS')}>
              1. Catálogo y Pedidos (BD + Eventos)
            </button>
            <button type="button" style={tabButtonStyle('MENSAJERIA')} onClick={() => setActiveTab('MENSAJERIA')}>
              2. Mensajería Asíncrona (ACK / NACK / DLQ)
            </button>
            <button type="button" style={tabButtonStyle('RABBIT_ADMIN')} onClick={() => setActiveTab('RABBIT_ADMIN')}>
              3. Administrador RabbitMQ (Colas, Exchanges, Bindings)
            </button>
            <button type="button" style={tabButtonStyle('LOGS')} onClick={() => setActiveTab('LOGS')}>
              4. Monitor de Auditoría y Logs DLQ
            </button>
          </nav>

          <main>
            {activeTab === 'PEDIDOS' && <CatalogoPedidosView />}
            {activeTab === 'MENSAJERIA' && <MensajeriaAsyncView />}
            {activeTab === 'RABBIT_ADMIN' && <RabbitAdminView />}
            {activeTab === 'LOGS' && <EventosLogView />}
          </main>
        </AuthenticatedTemplate>
      </ProtectedRoute>
    </div>
  );
}
