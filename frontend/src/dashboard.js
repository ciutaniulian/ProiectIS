export const demoDashboard = {
  user: { name: 'Iulian' },
  currency: 'RON',
  summary: {
    receivableCents: 62000,
    payableCents: 18000,
    totalExpensesCents: 94000,
  },
  groups: [
    { id: 1, name: 'Weekend in Brașov', memberCount: 4 },
    { id: 2, name: 'Apartment', memberCount: 3 },
    { id: 3, name: 'University friends', memberCount: 5 },
  ],
  expenses: [
    { id: 1, description: 'Dinner together', groupName: 'University friends', paidBy: 'Iulian', amountCents: 36000, date: '2026-10-13' },
    { id: 2, description: 'Weekly groceries', groupName: 'Apartment', paidBy: 'Ana', amountCents: 24000, date: '2026-10-12' },
    { id: 3, description: 'Train tickets', groupName: 'Weekend in Brașov', paidBy: 'Andrei', amountCents: 18000, date: '2026-10-11' },
    { id: 4, description: 'Utilities', groupName: 'Apartment', paidBy: 'Iulian', amountCents: 16000, date: '2026-10-10' },
  ],
}

export function formatMoney(cents, currency = 'RON') {
  return new Intl.NumberFormat('ro-RO', {
    style: 'currency', currency,
  }).format(cents / 100)
}
