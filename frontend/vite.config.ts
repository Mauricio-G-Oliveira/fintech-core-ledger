import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  // Use relative base path so the build works on GitHub Pages (e.g. username.github.io/repo-name/)
  base: './',
  server: {
    port: 3000,
  },
})
