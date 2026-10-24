import React, { PropsWithChildren } from 'react';
import { useAuth } from './useAuth';

export function ProtectedRoute({ children }: PropsWithChildren) {
  const { isAuthenticated, login } = useAuth();

  if (!isAuthenticated) {
    return (
      <section
        style={{
          background: '#fff',
          padding: '2rem',
          borderRadius: '10px',
          boxShadow: '0 2px 8px rgba(0,0,0,0.08)',
          marginTop: '1rem',
        }}
      >
        <h3 style={{ marginTop: 0 }}>Autenticación Requerida (Microsoft Entra ID - OAuth 2.0 / OIDC)</h3>
        <p style={{ color: '#4b5563' }}>
          Inicia sesión con tu cuenta de Microsoft Entra ID para obtener un token JWT válido y consumir los
          microservicios protegidos y la administración de RabbitMQ.
        </p>
        <button
          type="button"
          onClick={() => void login()}
          style={{
            background: '#2563eb',
            color: '#fff',
            border: 'none',
            padding: '0.65rem 1.25rem',
            borderRadius: '6px',
            fontWeight: 600,
            cursor: 'pointer',
          }}
        >
          Iniciar Sesión con Microsoft Entra ID
        </button>
      </section>
    );
  }

  return <>{children}</>;
}
