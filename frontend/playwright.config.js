import { defineConfig, devices } from '@playwright/test'

const backendPort = process.env.E2E_BACKEND_PORT || '18080'
const frontendPort = process.env.E2E_FRONTEND_PORT || '15173'
const backendURL = `http://localhost:${backendPort}`
const baseURL = `http://localhost:${frontendPort}`

export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  workers: 1,
  forbidOnly: !!process.env.CI,
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL,
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: [
    { name: 'desktop-chromium', use: { ...devices['Desktop Chrome'] } },
    { name: 'mobile-chromium', use: { ...devices['Pixel 7'] } },
  ],
  webServer: [
    {
      command: `${process.platform === 'win32' ? 'mvnw.cmd' : 'sh ./mvnw'} "-Dspring-boot.run.profiles=local" spring-boot:run`,
      cwd: '..',
      env: {
        SERVER_PORT: backendPort,
        SPRING_DATASOURCE_URL: 'jdbc:h2:mem:playwright;MODE=MySQL',
        SPRING_JPA_HIBERNATE_DDL_AUTO: 'create-drop',
        SPRING_JPA_DEFER_DATASOURCE_INITIALIZATION: 'true',
        SPRING_SQL_INIT_MODE: 'always',
        SPRING_SQL_INIT_DATA_LOCATIONS: 'file:frontend/e2e/chat-fixtures.sql',
      },
      url: `${backendURL}/api/auth/csrf`,
      timeout: 120_000,
      reuseExistingServer: false,
    },
    {
      command: `npm run dev -- --host localhost --port ${frontendPort} --strictPort`,
      env: { EXPENSE_SPLITTER_BACKEND_URL: backendURL },
      url: baseURL,
      reuseExistingServer: false,
    },
  ],
})
