import React, { PropsWithChildren, useEffect, useState } from 'react';
import { PublicClientApplication } from '@azure/msal-browser';
import { MsalProvider } from '@azure/msal-react';
import { msalConfig } from './msalConfig';

const msalInstance = new PublicClientApplication(msalConfig);

export function AuthProvider({ children }: PropsWithChildren) {
  const [initialized, setInitialized] = useState(false);

  useEffect(() => {
    msalInstance.initialize().then(() => {
      setInitialized(true);
    });
  }, []);

  if (!initialized) {
    return <div style={{ padding: '2rem' }}>Cargando módulo de autenticación Microsoft Entra ID...</div>;
  }

  return <MsalProvider instance={msalInstance}>{children}</MsalProvider>;
}
