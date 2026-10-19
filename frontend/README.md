# Expense Splitter frontend

Mobile-first React homepage for shared expenses, group balances, and groups.
Login, registration, session restoration, remember me, and logout use the Spring
backend. Groups, balances, and expenses still use sample data and are labelled
as a demo; they are not saved yet.

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
reverse proxy. The Vite development proxy is not included in the static build.

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
Once a dashboard API exists, replace the sample groups, expenses, and balances
with that response and pass the returned data to `App`.
Agree on the response fields and money representation with the backend team
first. The server must calculate balances and verify group membership; the
sample data is not business logic and must not be used as an authorization rule.

The homepage currently provides Home, Groups, and Expenses anchors within one
page. Group detail pages, expense creation, and settlements
are subsequent features.
