/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ['./index.html', './src/**/*.{js,vue}'],
  theme: {
    extend: {}
  },
  plugins: [require('daisyui')]
}
