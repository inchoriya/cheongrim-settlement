/**
 * 면접용 데모 화면 녹화 (관리자 / 대행사 / 가맹점)
 * 사용: npm run demo:install && npm run demo:record
 */
import { chromium } from 'playwright'
import fs from 'node:fs'
import path from 'node:path'

const BASE = process.env.DEMO_BASE_URL || 'http://localhost:5173'
const OUT_DIR = path.resolve('docs/demo-assets')
const VIDEO_DIR = path.join(OUT_DIR, 'raw-video')

fs.mkdirSync(VIDEO_DIR, { recursive: true })

async function clickNav(page, label) {
  const link = page.locator('aside nav a', { hasText: label })
  if (await link.count()) {
    await link.click()
    await page.waitForTimeout(800)
  }
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
    recordVideo: {
      dir: VIDEO_DIR,
      size: { width: 1440, height: 900 },
    },
  })
  const page = await context.newPage()

  // ========== 1) 관리자 ==========
  await loginAs(page, '관리자')
  await page.waitForTimeout(1000)

  await clickNav(page, '주문')
  await page.waitForTimeout(1000)

  await clickNav(page, '정산')
  await page.waitForTimeout(700)
  const batchBtn = page.getByRole('button', { name: '배치 실행' })
  if (await batchBtn.count()) {
    await batchBtn.click()
    await page.waitForTimeout(1800)
  }

  const firstSettlement = page.locator('table a').first()
  if (await firstSettlement.count()) {
    await firstSettlement.click()
    await page.waitForTimeout(900)
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
    await clickNav(page, '정산')
  }

  await clickNav(page, '지급')
  await page.waitForTimeout(800)
  const payBtn = page.getByRole('button', { name: '지급 실행' }).first()
  if (await payBtn.count()) {
    await payBtn.click()
    await page.waitForTimeout(1600)
  }

  await clickNav(page, '수수료 정책')
  await page.waitForTimeout(800)
  await clickNav(page, '조직')
  await page.waitForTimeout(800)
  await clickNav(page, '감사 로그')
  await page.waitForTimeout(1100)
  await logout(page)

  // ========== 2) 대행사 ==========
  await loginAs(page, '대행사')
  await page.waitForTimeout(900)
  await clickNav(page, '주문')
  await page.waitForTimeout(1200)
  await clickNav(page, '정산')
  await page.waitForTimeout(1200)
  await clickNav(page, '조직')
  await page.waitForTimeout(1000)
  // 대행사에게는 지급/감사로그 메뉴가 없어야 함 (권한 차이)
  await page.waitForTimeout(800)
  await logout(page)

  // ========== 3) 가맹점 ==========
  await loginAs(page, '가맹점')
  await page.waitForTimeout(900)
  await clickNav(page, '주문')
  await page.waitForTimeout(1200)
  await clickNav(page, '정산')
  await page.waitForTimeout(1400)
  // 가맹점은 본인 매장 건만 / 쓰기·지급 메뉴 없음
  await page.waitForTimeout(1000)

  await page.close()
  await context.close()
  await browser.close()

  const videos = fs
    .readdirSync(VIDEO_DIR)
    .filter((f) => f.endsWith('.webm'))
    .map((f) => {
      const full = path.join(VIDEO_DIR, f)
      return { full, mtime: fs.statSync(full).mtimeMs }
    })
    .sort((a, b) => b.mtime - a.mtime)
  if (!videos.length) {
    throw new Error('녹화 파일이 생성되지 않았습니다.')
  }
  const src = videos[0].full
  const dest = path.join(OUT_DIR, 'cheongrim-demo.webm')
  fs.copyFileSync(src, dest)
  // raw는 임시본만 남기고 정리
  for (const v of videos.slice(1)) {
    fs.unlinkSync(v.full)
  }
  console.log(`OK: ${dest}`)
  console.log(`source: ${path.basename(src)}`)
  console.log('Roles covered: 관리자 → 대행사 → 가맹점')
}

main().catch((err) => {
  console.error(err)
  process.exit(1)
})
