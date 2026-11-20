# Expense Splitter frontend

Mobile-first React homepage for shared expenses, group balances, and groups.
Login, registration, groups, invitations, and chat use the Spring backend.
The dashboard uses demo data for balances and expenses.

## Run locally

From this directory, run `npm install`, then `npm run dev` and open the URL
printed in the terminal. Run `npm run lint`, `npm test`, and `npm run build`
before publishing changes.

Start the Spring backend on port 8080 with `DB_USERNAME` and `DB_PASSWORD`
configured for your local MySQL database. Vite forwards `/api` requests to
`http://localhost:8080`, so cookies and CSRF requests use the frontend origin.
Open the Vite URL, create an account, and log in. Registration does not log you
in automatically. Sessions survive page reloads; remember me lasts seven days.
For deployment, serve the frontend and `/api` under the same origin using a
reverse proxy. Forward `/ws` with WebSocket upgrade support and serve
`index.html` for frontend routes such as `/groups/42`. The Vite development
proxy is included only in the development server.

Without MySQL, run `./mvnw.cmd "-Dspring-boot.run.profiles=local" spring-boot:run`
from the repository root in PowerShell. This profile uses an embedded H2 database
stored in `.local-data/expense_splitter.mv.db`, so local accounts survive restarts.
The database files are ignored by Git. No database credentials are required.

## Browser tests with Playwright

Install Chromium once with `npx playwright install chromium`, then run
`npm run test:e2e` from this directory. Java 17 or newer must be installed and
`JAVA_HOME` must point to the JDK directory. For example, in PowerShell:

```powershell
$env:JAVA_HOME = 'C:\Users\ciuta\.jdks\corretto-27.0.1'
npm run test:e2e
```

The tests start Spring and Vite automatically on ports 18080 and 15173. Set
`E2E_BACKEND_PORT` or `E2E_FRONTEND_PORT` to use different ports.
They use a temporary in-memory H2 database and do not modify local
accounts in `.local-data`. The suite runs in Chromium with desktop and Pixel 7
viewports and checks registration, failed login, session restoration, logout,
expense expansion, the group lifecycle, invitations, equal member permissions,
live chat between two sessions, chat history, reconnection, and access after
removal. The mobile project uses Pixel 7 browser emulation.

Run `npm run test:e2e:report` to open the HTML report. Screenshots are saved in
`test-results`; failures also retain a Playwright trace. Generated reports and
test artifacts are ignored by Git.

The chat tests seed two accounts and their memberships in the temporary database.
They also verify that a non-member gets HTTP 403 when requesting chat history.

## Groups and chat

Use **Groups → Create group** to save a group. The creator becomes its first
member in the same transaction, and the group appears only for its members.
All members share the same permissions to update the group, invite people,
remove members, and archive or restore it.

Invite a person from **Invitations** using their email address. They can respond
from their Groups inbox or open the link you copy and share with them. They must
sign in with the invited address. Invitations expire after seven days.

The group page shows members, invitations, activity, and chat. Archived groups
allow members to read chat history. Leaving or being removed ends access to the
group, including live messages on a previously open connection.

## Files

- `src/App.jsx`: homepage, group list, expandable expense list, and navigation.
- `src/AuthGateway.jsx`: session restoration and authenticated homepage.
- `src/AuthForm.jsx`: login and registration forms.
- `src/auth.js`: API calls with cookies and fresh CSRF tokens for mutations.
- `src/App.css`: mobile layout and styles.
- `src/index.css`: global styles.
- `src/dashboard.js`: sample dashboard data and currency formatting.

Amounts in the sample data are integer cents (100 means 1 RON). Expense rows
show the total shared expense, not the current user's share. The balance is
money owed to the user minus money the user owes; it is not a bank balance.
Sample receivable and payable amounts are illustrative, not calculated from
the expense rows, which do not yet include individual shares or settlements.

## Backend integration

`App` accepts a `dashboard` prop and currently defaults to `demoDashboard`.
`AuthGateway` supplies the authenticated user's profile from `/api/auth/me`.
Once a dashboard API exists, replace the sample expenses and balances
with that response and pass the returned data to `App`.
Agree on the response fields and money representation with the backend team
first. The server must calculate balances and verify group membership; the
sample data is not business logic and must not be used as an authorization rule.

Use the Groups navigation to open the group list and invitation inbox. Group
details and invitation links have their own addresses.
