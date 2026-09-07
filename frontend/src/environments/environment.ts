export const environment = {
  production: false,
  apiUrl: '/api',
  /**
   * Deve coincidir com ADMIN_API_KEY do backend (.env).
   * O JWT do painel é simulado no front; este header é o que o Spring valida.
   */
  adminApiKey: 'sua-chave-admin'
};
