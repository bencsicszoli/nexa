/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        // Nexa márkaszín (a kiválasztott "A" terv lila akcentusa)
        brand: {
          DEFAULT: '#6d28d9',
          light: '#8b5cf6',
          dark: '#5b21b6',
        },
      },
    },
  },
  plugins: [],
}
