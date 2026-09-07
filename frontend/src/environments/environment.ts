export const environment = {
  production: false,
  apiUrl: '/api',
  /**
   * Deve ser IGUAL a ADMIN_API_KEY no .env da raiz (lido pelo docker-compose).
   * O interceptor manda esse valor no header x-admin-token; o Spring compara com admin.api.key.
   */
  adminApiKey: 'sua-chave-admin'
};
