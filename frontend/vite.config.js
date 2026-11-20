import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

const backend = process.env.EXPENSE_SPLITTER_BACKEND_URL || 'http://localhost:8080'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': backend,
      '/ws': {
        target: backend,
        ws: true,
      },
    },
  },
})
