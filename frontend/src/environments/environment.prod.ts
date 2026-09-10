export const environment = {
  production: true,
  apiUrl: 'https://api.gamb.site',
  /**
   * Deve ser IGUAL a ADMIN_API_KEY no .env da raiz (lido pelo docker-compose).
   * O interceptor manda esse valor no header x-admin-token; o Spring compara com admin.api.key.
   */
  adminApiKey: 'sua-chave-admin',
  cloudinary: {
    cloudName: 'dol0640d6',
    uploadPreset: 'saas_locação'
  }
};
