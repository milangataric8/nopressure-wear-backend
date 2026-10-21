-- Product descriptions and footer copied from https://nopressurewear.com/ on 2026-10-21.
-- English source copy; text only. The React ProductDescription and Footer components
-- already append src/assets/nopressure_signature_footer.png below this HTML.
-- Updates only the 20 seeded SKU/color pairs and store_tagline. No account changes.
-- Restart the backend after direct SQL updates if settings are cached.
BEGIN;
SET LOCAL lock_timeout = '10s';

-- https://nopressurewear.com/products/no-pressure-moto-t-shirt-1
UPDATE product
SET description = $np$<p><strong><em>Wear your mindset. We proudly present the Moto T-Shirt. 🌿✨</em></strong></p><p><em>More than just a classic white tee, this piece carries our core message right on the chest: No Pressure. Be relaxed. Live easy. Designed for those who appreciate clean typography and pure simplicity, it serves as a daily reminder to filter out the noise and stay grounded.</em></p><p><em>Crafted from soft, breathable cotton, it delivers maximum comfort whether you're easing into your morning routine or stepping out for a relaxed evening.</em></p><p><em>🛒 Wear the mantra and embrace the calm.</em></p><p><strong><em>Be relaxed. Live Easy.</em></strong></p>$np$, updated_at = now()
WHERE sku = 'NP-MOTO'
  AND color_name IN ('Black', 'Green', 'Ivory', 'Navy Blue', 'White');

-- https://nopressurewear.com/products/nopressure-c-row-t-shirt-1
UPDATE product
SET description = $np$<p><strong>Barely there, always grounded. Meet the NoPressure Tiny Center Tee. ☁️👁️</strong></p><p>Sometimes, the quietest voice is the most powerful. This tee is for those who live by the philosophy of subtle, unspoken ease. It features our stacked signature "no pressure" logo shrunk down and centered on the chest—a tiny whisper in a world that never stops shouting.</p><p>Made from ultra-soft, garment-dyed or vintage-wash natural cotton that feels broken-in from day one, its relaxed fit invites you to just <em>be</em>. It’s a minimalist's dream, giving you the ultimate canvas for a stress-free day.</p><p>🛒 Keep it effortless, wear the whisper, and embrace the calm.</p><p><strong><em>Be relaxed. Live Easy.</em></strong></p>$np$, updated_at = now()
WHERE sku = 'NP-ROW'
  AND color_name IN ('Black', 'Green', 'Ivory', 'Navy Blue', 'White');

-- https://nopressurewear.com/products/minimal-script-serendipity-t-shirt
UPDATE product
SET description = $np$<p><strong><em>Less pressure, more style. We proudly present our Signature model. 🌊✨</em></strong></p><p><em>The minimalist design, with a fluid line across the chest, is made for anyone who chooses peace over hustle and simplicity over complication.</em></p><p><em>Whether you choose the crisp white version or the powerful dark tone, the Signature T-shirt brings you that invaluable feeling of ease the moment you put it on.</em></p><p><em>🛒 Find your favorite fit and release the pressure.</em></p><p><strong><em>Be relaxed. Live Easy.</em></strong></p>$np$, updated_at = now()
WHERE sku = 'NP-SIGNATURE'
  AND color_name IN ('Black', 'Green', 'Ivory', 'Navy Blue', 'White');

-- https://nopressurewear.com/products/nopressure-circle-tee-1
UPDATE product
SET description = $np$<p><strong><em>A perfect balance of form and focus. Meet the NoPressure Circle Tee. ⭕✨</em></strong></p><p><em>Geometric simplicity meets everyday ease. The </em><strong><em>Circle Tee</em></strong><em> features a fine, clean circular outline centered on the chest, framing our signature name with a subtle pop of red on "no". The circle represents completeness, balance, and creating your own peaceful bubble amidst the outer noise.</em></p><p><em>Crafted from soft, premium cotton with a relaxed fit, it offers maximum comfort for your everyday moves. It's a clean, graphic statement designed for those who appreciate modern minimalism with a thoughtful edge.</em></p><p><em>🛒 Find your balance and step out in effortless style.</em></p><p><strong><em>Be relaxed. Live Easy.</em></strong></p>$np$, updated_at = now()
WHERE sku = 'NP-CIRCLE'
  AND color_name IN ('Black', 'Green', 'Ivory', 'Navy Blue', 'White');

-- https://nopressurewear.com/ (brand text in footer)
UPDATE store_settings SET value = $np$<p><em><strong>Life's already complicated enough – your style doesn’t have to be.</strong></em></p><p><em><strong>NoPressure</strong></em><em> was created for those who choose to find their own rhythm in a world that’s always rushing.<br/>We believe that true strength comes from inner peace and the freedom to be yourself, free from societal expectations and imposed stress. Our pieces are designed for ultimate comfort, freedom of movement, and that invaluable feeling of ease.<br/>Slow down, take a breath, and embrace the moment. </em></p><p><em><strong>Be relaxed. Live Easy.</strong></em></p>$np$
WHERE key = 'store_tagline';
INSERT INTO store_settings (key, value, label)
SELECT 'store_tagline', $np$<p><em><strong>Life's already complicated enough – your style doesn’t have to be.</strong></em></p><p><em><strong>NoPressure</strong></em><em> was created for those who choose to find their own rhythm in a world that’s always rushing.<br/>We believe that true strength comes from inner peace and the freedom to be yourself, free from societal expectations and imposed stress. Our pieces are designed for ultimate comfort, freedom of movement, and that invaluable feeling of ease.<br/>Slow down, take a breath, and embrace the moment. </em></p><p><em><strong>Be relaxed. Live Easy.</strong></em></p>$np$, 'Tagline'
WHERE NOT EXISTS (SELECT 1 FROM store_settings WHERE key = 'store_tagline');
COMMIT;
