/**
 * 포트폴리오용 화면 캡처 (관리자 / 대행사 / 가맹점)
 * 사용: npm run demo:install && node scripts/capture-screens.mjs
 *
 * demo-record.mjs와 같은 흐름을 따라가되, 영상 대신 PNG로 남깁니다.
 */
import { chromium } from 'playwright'
import fs from 'node:fs'
import path from 'node:path'

const BASE = process.env.DEMO_BASE_URL || 'http://localhost:5173'
const OUT_DIR = path.resolve('docs/screenshots')

fs.mkdirSync(OUT_DIR, { recursive: true })

let seq = 0
async function shot(page, name) {
  seq += 1
  const file = path.join(OUT_DIR, `${String(seq).padStart(2, '0')}-${name}.png`)
  await page.screenshot({ path: file })
  console.log(`  ${path.basename(file)}`)
}

async function clickNav(page, label) {
  const link = page.locator('aside nav a', { hasText: label })
  if (await link.count()) {
    await link.click()
    await page.waitForTimeout(800)
    return true
  }
  return false
}

async function loginAs(page, roleLabel) {
  await page.goto(`${BASE}/login`, { waitUntil: 'networkidle' })
  await page.waitForTimeout(600)
  await page.getByRole('button', { name: roleLabel }).click()
  await page.waitForTimeout(350)
  await page.getByRole('button', { name: '로그인' }).click()
  await page.waitForURL((url) => !url.pathname.includes('/login'))
  await page.waitForTimeout(1000)
}

async function logout(page) {
  await page.getByRole('button', { name: '로그아웃' }).click()
  await page.waitForURL('**/login')
  await page.waitForTimeout(700)
}

async function main() {
  const browser = await chromium.launch({ headless: true })
  const context = await browser.newContext({
    viewport: { width: 1440, height: 900 },
    locale: 'ko-KR',
    deviceScaleFactor: 2, // 문서에 넣어도 흐리지 않게
  })
  const page = await context.newPage()

  // ========== 로그인 화면 ==========
  console.log('[로그인]')
  await page.goto(`${BASE}/login`, { waitUntil: 'networkidle' })
  await page.waitForTimeout(800)
  await shot(page, 'login')

  // ========== 관리자 ==========
  console.log('[관리자]')
  await loginAs(page, '관리자')
  await shot(page, 'admin-dashboard')

  if (await clickNav(page, '주문')) {
    await page.waitForTimeout(600)
    await shot(page, 'admin-orders')
  }

  if (await clickNav(page, '정산')) {
    await page.waitForTimeout(600)
    const batchBtn = page.getByRole('button', { name: '배치 실행' })
    if (await batchBtn.count()) {
      await batchBtn.click()
      await page.waitForTimeout(1800)
    }
    await shot(page, 'admin-settlements')

    const firstSettlement = page.locator('table a').first()
    if (await firstSettlement.count()) {
      await firstSettlement.click()
      await page.waitForTimeout(1000)
      await shot(page, 'admin-settlement-detail')

      const confirmBtn = page.getByRole('button', { name: '확정' })
      if (await confirmBtn.count()) {
        await confirmBtn.click()
        await page.waitForTimeout(900)
      }
      const readyBtn = page.getByRole('button', { name: '지급 대기로' })
      if (await readyBtn.count()) {
        await readyBtn.click()
        await page.waitForTimeout(900)
      }
      await shot(page, 'admin-settlement-confirmed')
    }
  }

  if (await clickNav(page, '지급')) {
    await page.waitForTimeout(700)
    const payBtn = page.getByRole('button', { name: '지급 실행' }).first()
    if (await payBtn.count()) {
      await payBtn.click()
      await page.waitForTimeout(1600)
    }
    await shot(page, 'admin-payouts')
  }

  if (await clickNav(page, '수수료 정책')) {
    await page.waitForTimeout(600)
    await shot(page, 'admin-fee-policy')
  }

  if (await clickNav(page, '조직')) {
    await page.waitForTimeout(600)
    await shot(page, 'admin-organizations')
  }

  if (await clickNav(page, '감사 로그')) {
    await page.waitForTimeout(1000)
    await shot(page, 'admin-audit-log')
  }

  await logout(page)

  // ========== 대행사 (권한 범위 축소) ==========
  console.log('[대행사]')
  await loginAs(page, '대행사')
  await shot(page, 'agency-dashboard')
  if (await clickNav(page, '정산')) {
    await page.waitForTimeout(1000)
    await shot(page, 'agency-settlements')
  }
  await logout(page)

  // ========== 가맹점 (본인 건만) ==========
  console.log('[가맹점]')
  await loginAs(page, '가맹점')
  await shot(page, 'merchant-dashboard')
  if (await clickNav(page, '정산')) {
    await page.waitForTimeout(1200)
    await shot(page, 'merchant-settlements')
  }

  await page.close()
  await context.close()
  await browser.close()

  console.log(`\n완료: ${seq}장 → ${OUT_DIR}`)
}

main().catch((err) => {
  console.error(err)
  process.exit(1)
})
