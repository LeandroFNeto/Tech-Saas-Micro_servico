/** @type {import('tailwindcss').Config} */
const design = require('../especificacao/front/telas.design.json');

module.exports = {
  content: [
    './src/**/*.{html,ts}',
    // Pasta onde o HTML colado do v0.dev entra no build do Tailwind.
    './src/app/features/design-v0/**/*.{html,ts}',
    '../especificacao/front/**/*.json'
  ],
  theme: {
    extend: {
      fontFamily: {
        sans: design.tema.fontes.sans,
        display: design.tema.fontes.display
      },
      colors: {
        brand: design.tema.cores.brand,
        accent: design.tema.cores.accent
      }
    }
  },
  plugins: []
};
