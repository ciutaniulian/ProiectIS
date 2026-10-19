import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import AuthGateway from './AuthGateway.jsx'

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <AuthGateway />
  </StrictMode>,
)
