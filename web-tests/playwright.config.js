const { defineConfig } = require('@playwright/test');
module.exports = defineConfig({
  testDir: './tests', workers: 1, fullyParallel: false, retries: 0,
  timeout: 45000,
  use: { baseURL: process.env.CACAOSELVA_WEB_TEST_URL || 'http://127.0.0.1:5081', screenshot: 'only-on-failure', trace: 'off' },
  reporter: [['list'], ['html', { open: 'never' }]],
  projects: [
    { name: 'desktop', use: { viewport: { width: 1365, height: 900 } } },
    { name: 'tablet', use: { viewport: { width: 768, height: 1024 } } },
    { name: 'mobile', use: { viewport: { width: 375, height: 812 }, isMobile: true, hasTouch: true } },
    { name: 'mobile-small', use: { viewport: { width: 320, height: 740 }, isMobile: true, hasTouch: true } }
  ]
});
