import { IPublicClientApplication } from '@azure/msal-browser';

export const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

async function acquireJwtToken(msalInstance: IPublicClientApplication): Promise<string> {
  const account = msalInstance.getActiveAccount() || msalInstance.getAllAccounts()[0];
  if (!account) {
    throw new Error('No hay una cuenta autenticada en Microsoft Entra ID.');
  }

  const scope = import.meta.env.VITE_API_SCOPE || 'User.Read';
  try {
    const response = await msalInstance.acquireTokenSilent({
      scopes: [scope],
      account,
    });
    return response.accessToken;
  } catch {
    const response = await msalInstance.acquireTokenPopup({
      scopes: [scope],
      account,
    });
    return response.accessToken;
  }
}

export async function apiRequest<T>(
  path: string,
  msalInstance: IPublicClientApplication,
  options: RequestInit = {},
): Promise<T> {
  const accessToken = await acquireJwtToken(msalInstance);

  const response = await fetch(`${apiBaseUrl}${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${accessToken}`,
      ...(options.headers || {}),
    },
  });

  const data = await response.json().catch(() => ({}));

  if (!response.ok) {
    const errorMsg =
      data.message || `Error HTTP ${response.status}: ${JSON.stringify(data)}`;
    throw new Error(errorMsg);
  }

  return data as T;
}
