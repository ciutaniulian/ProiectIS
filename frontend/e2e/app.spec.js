import { test, expect } from '@playwright/test'

const password = 'PlaywrightTest123!'
const exact = name => ({ name, exact: true })
const button = (page, name) => page.getByRole('button', exact(name))
const link = (page, name) => page.getByRole('link', exact(name))
const heading = (page, name) => page.getByRole('heading', exact(name))
const groupLink = (page, name) => page.getByRole('link').filter({ has: heading(page, name) })
const unique = label => [label, Date.now(), Math.random().toString(36).slice(2, 8)].join(' ')
const freshEmail = () => unique('pw').replaceAll(' ', '-') + '@example.com'

async function register(page, name, email = freshEmail()) {
  await button(page, 'Create an account').click()
  await page.getByLabel('Name', { exact: true }).fill(name)
  await page.getByLabel('Email', { exact: true }).fill(email)
  await page.getByLabel('Password', { exact: true }).fill(password)
  await button(page, 'Create account').click()
  await expect(page.getByRole('status')).toHaveText('Account created. Log in with your email and password.')
  return email
}

async function login(page, email) {
  await page.getByLabel('Email', { exact: true }).fill(email)
  await page.getByLabel('Password', { exact: true }).fill(password)
  await button(page, 'Log in').click()
  await expect(button(page, 'Log out')).toBeVisible()
}

async function account(page, name) {
  await page.goto('/')
  const email = await register(page, name)
  await login(page, email)
  return email
}

async function createGroup(page, name, description = '') {
  await link(page, 'Groups').click()
  await button(page, 'Create group').click()
  const groupName = page.getByLabel('Group name', { exact: true })
  await groupName.fill('   ')
  await expect(button(page, 'Create group')).toBeDisabled()
  await groupName.fill(name)
  if (description) await page.getByLabel('Description (optional)', { exact: true }).fill(description)
  await button(page, 'Create group').click()
  await expect(heading(page, name)).toBeVisible()
  const path = new URL(page.url()).pathname
  expect(path).toMatch(/^\/groups\/\d+$/)
  return { path, id: Number(path.split('/').at(-1)) }
}

async function invite(page, email) {
  await link(page, 'Invitations').click()
  await page.getByLabel('Invite by email', { exact: true }).fill(email)
  await button(page, 'Create invitation').click()
  await expect(page.getByText(email, { exact: true })).toBeVisible()
}

async function confirmAction(page, action, confirmation = action) {
  await button(page, action).click()
  const dialog = page.getByRole('alertdialog')
  await expect(dialog).toBeVisible()
  await button(dialog, confirmation).click()
  await expect(dialog).toBeHidden()
}

async function openChat(page) {
  await link(page, 'Chat').click()
  await expect(page.getByText('Live chat', { exact: true })).toBeVisible()
}

async function send(page, message) {
  const input = page.getByRole('textbox', exact('Message'))
  await input.fill(message)
  await button(page, 'Send message').click()
  await expect(input).toHaveValue('')
  await expect(page.getByText(message, { exact: true })).toHaveCount(1)
}

async function expectNoAccess(page, id) {
  for (const path of ['/api/groups/' + id, '/api/group/' + id + '/messages']) {
    const response = await page.request.get(path)
    expect([403, 404]).toContain(response.status())
  }
}

test('registration, failed login, session restoration, dashboard and logout', async ({ page }, testInfo) => {
  const errors = []
  page.on('pageerror', error => errors.push(error.message))
  await page.goto('/')
  const email = await register(page, 'Playwright User')
  await page.getByLabel('Password', { exact: true }).fill('wrong-password')
  await button(page, 'Log in').click()
  await expect(page.getByRole('alert')).toHaveText('Incorrect email or password. Please try again.')
  await login(page, email)
  await expect(heading(page, 'Playwright User')).toBeVisible()
  await expectNoAccess(page, 1)
  await expect(page.locator('#expense-list > li')).toHaveCount(2)
  await button(page, 'See all').click()
  await expect(page.locator('#expense-list > li')).toHaveCount(4)
  await button(page, 'Show less').click()
  await expect(page.locator('#expense-list > li')).toHaveCount(2)
  await page.reload()
  await expect(heading(page, 'Playwright User')).toBeVisible()
  await page.screenshot({ path: testInfo.outputPath('dashboard.png'), fullPage: true })
  await button(page, 'Log out').click()
  await page.reload()
  await expect(heading(page, 'Welcome back.')).toBeVisible()
  expect(errors).toEqual([])
})

test('real groups persist, filter, and follow the archive and close lifecycle', async ({ page }, testInfo) => {
  await account(page, 'Trip Organizer')
  const name = unique('Day trip')
  const description = 'Bring rain gear and a picnic.'
  const group = await createGroup(page, name, description)
  await expect(page.getByText(description, { exact: true })).toBeVisible()
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true)
  await page.screenshot({ path: testInfo.outputPath('group-overview.png'), fullPage: true })
  await page.reload()
  await expect(heading(page, name)).toBeVisible()
  await openChat(page)
  const history = unique('Read-only history')
  await send(page, history)
  await button(page, 'Close chat').click()
  await confirmAction(page, 'Archive group')
  await expect(button(page, 'Restore group')).toBeVisible()
  await expect(button(page, 'Edit details')).toHaveCount(0)
  await link(page, 'Chat').click()
  await expect(page.getByText('This chat is read-only while the group is archived.')).toBeVisible()
  await expect(page.getByText(history, { exact: true })).toBeVisible()
  await page.keyboard.press('Escape')
  await expect(page.getByRole('dialog')).toHaveCount(0)
  await link(page, 'Activity').click()
  await expect(heading(page, 'Group activity')).toBeVisible()
  await page.goBack()
  await expect(button(page, 'Restore group')).toBeVisible()
  await link(page, 'Groups').click()
  await expect(heading(page, 'Your groups')).toBeVisible()
  await expect(groupLink(page, name)).toHaveCount(0)
  await expect(groupLink(page, 'Weekend in Brașov')).toHaveCount(0)
  await button(page, 'Archived').click()
  const search = page.getByRole('searchbox')
  await search.fill('another group')
  await expect(groupLink(page, name)).toHaveCount(0)
  await search.fill(name)
  await groupLink(page, name).click()
  await button(page, 'Restore group').click()
  await expect(button(page, 'Edit details')).toBeVisible()
  await link(page, 'Members').click()
  await button(page, 'Leave group').click()
  const dialog = page.getByRole('alertdialog')
  await expect(dialog).toBeVisible()
  await button(dialog, 'Close group').click()
  await expect(heading(page, 'Your groups')).toBeVisible()
  await expectNoAccess(page, group.id)
})

test('members have equal rights, chat live, and lose access when removed', async ({ page, browser, baseURL }, testInfo) => {
  await account(page, 'Group Creator')
  const initial = unique('Camping')
  const renamed = initial + ' updated'
  const group = await createGroup(page, initial)
  const memberContext = await browser.newContext({ baseURL })
  try {
    const member = await memberContext.newPage()
    const memberEmail = await account(member, 'Invited Member')
    await expectNoAccess(member, group.id)
    await invite(page, memberEmail)
    await link(member, 'Groups').click()
    await expect(heading(member, 'Invitations for you')).toBeVisible()
    await button(member, 'Accept invitation').click()
    await member.goto(group.path)
    await expect(heading(member, initial)).toBeVisible()
    await button(member, 'Edit details').click()
    await member.getByLabel('Group name', { exact: true }).fill(renamed)
    await button(member, 'Save changes').click()
    await expect(heading(member, renamed)).toBeVisible()

    await page.goto(group.path)
    await openChat(page)
    await openChat(member)
    const message = unique('Two-session message')
    await send(member, message)
    await expect(page.getByText(message, { exact: true })).toHaveCount(1)
    await page.screenshot({ path: testInfo.outputPath('group-chat.png'), fullPage: true })

    await button(member, 'Close chat').click()
    await link(member, 'Members').click()
    await confirmAction(member, 'Remove Group Creator', 'Remove member')
    await expectNoAccess(page, group.id)
    await openChat(member)
    const privateMessage = unique('After removal')
    await send(member, privateMessage)
    await expect(page.getByText(privateMessage, { exact: true })).toHaveCount(0)
    await expect(page.getByText('You no longer have access to this group.', { exact: true })).toBeVisible()
    await button(member, 'Close chat').click()
    await link(member, 'Activity').click()
    await expect(member.getByText(/removed Group Creator/i)).toBeVisible()
  } finally {
    await memberContext.close()
  }
})

test('an invitation link works after registration and the new member can leave', async ({ page, context, browser, baseURL }) => {
  await context.grantPermissions(['clipboard-read', 'clipboard-write'])
  await account(page, 'Link Sender')
  const groupName = unique('Shared trip')
  const group = await createGroup(page, groupName)
  const receiverEmail = freshEmail()
  await invite(page, receiverEmail)
  await button(page, 'Copy invitation link').click()
  const inviteLink = page.getByLabel('Invitation link', { exact: true })
  await expect(inviteLink).toHaveValue(/^http/)
  const url = await inviteLink.inputValue()
  await expect.poll(() => page.evaluate(() => navigator.clipboard.readText())).toBe(url)
  expect(new URL(url).pathname).toMatch(/^\/group-invitations\/.+/)

  const wrongAccount = await context.newPage()
  await wrongAccount.goto(url)
  await expect(wrongAccount.getByRole('alert')).toHaveText('This invitation is unavailable for your account.')
  await expect(button(wrongAccount, 'Accept invitation')).toHaveCount(0)
  await wrongAccount.close()

  const receiverContext = await browser.newContext({ baseURL })
  try {
    const receiver = await receiverContext.newPage()
    await receiver.goto(url)
    await register(receiver, 'New Member', receiverEmail)
    await login(receiver, receiverEmail)
    await button(receiver, 'Accept invitation').click()
    await expect(heading(receiver, groupName)).toBeVisible()
    await link(receiver, 'Members').click()
    await expect(receiver.getByText('Link Sender', { exact: true })).toBeVisible()
    await confirmAction(receiver, 'Leave group')
    await expect(heading(receiver, 'Your groups')).toBeVisible()
    await expectNoAccess(receiver, group.id)
  } finally {
    await receiverContext.close()
  }
})

test('duplicate invitations are rejected and declined invitations can be reissued and revoked', async ({ page, browser, baseURL }) => {
  await account(page, 'Invitation Organizer')
  const group = await createGroup(page, unique('Invitation review'))
  const recipientContext = await browser.newContext({ baseURL })
  try {
    const recipient = await recipientContext.newPage()
    const email = await account(recipient, 'Invitation Recipient')
    await invite(page, email)
    await page.getByLabel('Invite by email', { exact: true }).fill(email)
    await button(page, 'Create invitation').click()
    await expect(page.getByRole('alert')).toContainText('already a pending invitation')
    await link(recipient, 'Groups').click()
    await button(recipient, 'Decline invitation').click()
    await expect(recipient.getByText('Invitation declined.', { exact: true })).toBeVisible()
    await expectNoAccess(recipient, group.id)

    await page.reload()
    await expect(page.getByText(email, { exact: true })).toHaveCount(0)
    await page.getByLabel('Invite by email', { exact: true }).fill(email)
    await button(page, 'Create invitation').click()
    await expect(page.getByText(email, { exact: true })).toBeVisible()
    await confirmAction(page, 'Revoke invitation for ' + email, 'Revoke invitation')
    await expect(page.getByText(email, { exact: true })).toHaveCount(0)
    await recipient.reload()
    await expect(recipient.getByText('No pending invitations.', { exact: true })).toBeVisible()
  } finally {
    await recipientContext.close()
  }
})

test('legacy chat history is paginated and reconnection recovers missed messages', async ({ page, browser, baseURL }) => {
  let client
  let offline = false
  await page.routeWebSocket('**/ws', route => {
    if (offline) {
      route.close()
      return
    }
    client = route
    route.connectToServer()
  })
  await page.goto('/')
  await login(page, 'first-chat@example.com')
  await link(page, 'Groups').click()
  await groupLink(page, 'Weekend in Brașov').click()
  await openChat(page)
  await expect(page.getByText('Legacy history 1', { exact: true })).toHaveCount(0)
  await button(page, 'Load older messages').click()
  await expect(page.getByText('Legacy history 1', { exact: true })).toBeVisible()
  const secondContext = await browser.newContext({ baseURL })
  try {
    const peer = await secondContext.newPage()
    await peer.goto('/')
    await login(peer, 'second-chat@example.com')
    await expectNoAccess(peer, 2)
    await link(peer, 'Groups').click()
    await groupLink(peer, 'Weekend in Brașov').click()
    await openChat(peer)
    expect(client).toBeDefined()
    offline = true
    await client.close()
    await expect(page.getByText('Live chat', { exact: true })).toBeHidden()
    const missed = unique('Message during interruption')
    await send(peer, missed)
    await expect(page.getByText(missed, { exact: true })).toHaveCount(0)
    offline = false
    await expect(page.getByText('Live chat', { exact: true })).toBeVisible({ timeout: 15_000 })
    await expect(page.getByText(missed, { exact: true })).toHaveCount(1)
    await page.reload()
    await expect(page.getByText(missed, { exact: true })).toHaveCount(1)
  } finally {
    await secondContext.close()
  }
})
