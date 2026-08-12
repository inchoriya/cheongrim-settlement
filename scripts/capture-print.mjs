/**
 * 포트폴리오용 정산서 출력 화면 캡처 (Thymeleaf SSR)
 * 사용: npm run demo:install && node scripts/capture-print.mjs
 *
 * capture-screens.mjs는 SPA(:5173)를 찍지만, 인쇄 화면은 백엔드가 직접 렌더링하므로
 * API 서버(:8080)만 떠 있으면 됩니다.
 */
import { chromium } from 'playwright'
import fs from 'node:fs'
import path from 'node:path'

const BASE = process.env.PRINT_BASE_URL || 'http://localhost:8080'
const OUT_DIR = path.resolve('docs/screenshots')

fs.mkdirSync(OUT_DIR, { recursive: true })

async function shot(page, name) {
  const file = path.join(OUT_DIR, `${name}.png`)
  await page.screenshot({ path: file, fullPage: true })
  console.log(`  ${path.basename(file)}`)
}

async function main() {
  const browser = await chromium.launch({ headless: true })
  const context = await browser.newContext({
    viewport: { width: 1280, height: 900 },
    locale: 'ko-KR',
    deviceScaleFactor: 2,
  })
  const page = await context.newPage()

  console.log('[정산서 출력 화면]')

  // 데모 계정으로 폼 로그인 (시드 고정값, README에 공개된 값)
  await page.goto(`${BASE}/print/login`, { waitUntil: 'networkidle' })
  await shot(page, '15-print-login')

  await page.fill('#username', 'admin@cheongnim.local')
  await page.fill('#password', 'Demo1234!')
  await page.click('button[type=submit]')
  await page.waitForURL('**/print/settlements')
  await page.waitForTimeout(400)
  await shot(page, '16-print-settlement-list')

  // 첫 정산 건의 정산서
  await page.locator('table tbody tr a').first().click()
  await page.waitForURL('**/print/settlements/*')
  await page.waitForTimeout(400)
  await shot(page, '17-print-settlement-sheet')

  // 인쇄 미리보기 상태 — @media print 가 적용된 모습
  await page.emulateMedia({ media: 'print' })
  await page.waitForTimeout(200)
  await shot(page, '18-print-settlement-sheet-print-media')

  await page.close()
  await context.close()
  await browser.close()

  console.log(`\n완료 → ${OUT_DIR}`)
}

main().catch((err) => {
  console.error(err)
  process.exit(1)
})
