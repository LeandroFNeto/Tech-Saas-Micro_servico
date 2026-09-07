/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ['./src/**/*.{html,ts}'],
  theme: {
    extend: {
      fontFamily: {
        sans: ['Inter', 'ui-sans-serif', 'system-ui', 'sans-serif'],
        display: ['Cinzel', 'Georgia', 'serif']
      },
      colors: {
        brand: {
          50: '#f6f0fb',
          100: '#ebe0f5',
          200: '#d4bce8',
          300: '#b889d4',
          400: '#9b5cbf',
          500: '#7c3aad',
          600: '#652d90',
          700: '#4e2473',
          800: '#2E1A47',
          900: '#1A0B2E'
        },
        accent: {
          50: '#fff4ec',
          100: '#ffe4d1',
          200: '#ffc9a3',
          300: '#ffab70',
          400: '#FF8C42',
          500: '#F7931E',
          600: '#e07a12',
          700: '#c4620d'
        }
      }
    }
  },
  plugins: []
};
