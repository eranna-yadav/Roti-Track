/**
 * Rasterises assets/brand/*.svg into the app icon, adaptive icon, splash
 * and favicon. Needs `sharp` available: npx --yes -p sharp node scripts/generate-icons.js
 */
const sharp = require('sharp');
const fs = require('fs');
const path = require('path');
const R = path.join(__dirname, '..');
const A = path.join(R, 'assets');
const B = path.join(A, 'brand');
const logo = fs.readFileSync(path.join(B, 'logo.svg'));
const mark = fs.readFileSync(path.join(B, 'mark.svg'));
const mono = fs.readFileSync(path.join(B, 'monochrome.svg'));

const pad = (svg, size, scale, bg) => {
  const inner = Math.round(size * scale);
  return sharp(svg).resize(inner, inner).toBuffer().then((buf) =>
    sharp({ create: { width: size, height: size, channels: 4, background: bg } })
      .composite([{ input: buf, gravity: 'center' }])
      .png()
      .toBuffer()
  );
};

(async () => {
  await sharp(logo).resize(1024, 1024).png().toFile(path.join(A, 'icon.png'));
  await sharp(logo).resize(512, 512).png().toFile(path.join(B, 'logo-512.png'));
  await sharp(logo).resize(48, 48).png().toFile(path.join(A, 'favicon.png'));

  // Android adaptive icon: foreground mark on transparent, safe-zone padded
  fs.writeFileSync(path.join(A, 'android-icon-foreground.png'), await pad(mark, 1024, 0.6, { r: 0, g: 0, b: 0, alpha: 0 }));
  fs.writeFileSync(path.join(A, 'android-icon-monochrome.png'), await pad(mono, 1024, 0.6, { r: 0, g: 0, b: 0, alpha: 0 }));
  await sharp({ create: { width: 1024, height: 1024, channels: 4, background: '#1B4FF0' } })
    .png().toFile(path.join(A, 'android-icon-background.png'));

  // Splash: white mark on transparent, expo tints the background
  fs.writeFileSync(path.join(A, 'splash-icon.png'), await pad(mark, 1024, 0.72, { r: 0, g: 0, b: 0, alpha: 0 }));
  console.log('icons generated');
})();
