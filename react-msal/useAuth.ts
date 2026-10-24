import { InteractionRequiredAuthError } from '@azure/msal-browser';
import { useAccount, useIsAuthenticated, useMsal } from '@azure/msal-react';
import { apiScopes, loginRequest } from './msalConfig';

export function useAuth() {
  const { instance, accounts } = useMsal();
  const isAuthenticated = useIsAuthenticated();
  const account = useAccount(accounts[0] ?? null);

  async function login() {
    await instance.loginPopup(loginRequest);
  }

  async function logout() {
    await instance.logoutPopup({
      account: account ?? undefined,
    });
  }

  async function getAccessToken(): Promise<string> {
    const activeAccount = account ?? accounts[0];

    if (!activeAccount) {
      throw new Error('No hay una cuenta autenticada.');
    }

    try {
      const result = await instance.acquireTokenSilent({
        account: activeAccount,
        scopes: apiScopes,
      });
      return result.accessToken;
    } catch (error) {
      if (error instanceof InteractionRequiredAuthError) {
        const popupResult = await instance.acquireTokenPopup({
          account: activeAccount,
          scopes: apiScopes,
        });
        return popupResult.accessToken;
      }
      throw error;
    }
  }

  return {
    isAuthenticated,
    account,
    login,
    logout,
    getAccessToken,
  };
}
