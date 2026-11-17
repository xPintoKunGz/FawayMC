# 🌸 MeryITEM (v1.0.0)

> **Modern, Elegant & Intuitive In-Hand Item Creation Suite for Paper & Spigot Minecraft (1.20 - 26.3+)**

---

## ✨ What's New in this Major Update

1. **MMOItems Particle Engine (100% Match with MMOItems + Easy UI)**:
   - นำรูปแบบ Particle Math และ Motion จาก MMOItems มาใส่ครบทุกแบบ:
     - **HELIX**: เกลียวคู่หมุนวนขึ้นสู่ท้องฟ้ารอบตัวผู้เล่น
     - **VORTEX**: วงพายุกรวยหมุนวนรอบตัวอย่างต่อเนื่อง
     - **DOUBLE_RINGS**: วงแหวนดาวเคราะห์ 2 วงหมุนตัดกันรอบตัว
     - **GALAXY**: ดาราจักรหมุนวน 3 แขนรอบตัวผู้เล่น
     - **FIREFLIES**: หิ่งห้อยแสงลอยระยิบระยับรอบตัวแบบสุ่มทิศทาง
     - **OFFSET**: พุ่งออกจากปลายดาบ/อาวุธโดยตรง
     - **AURA**: ออร่าละอองรอบอาวุธ
   - รองรับทุกชนิดอนุภาค: Sakura, Flame, Soul Fire, Starlight, Void Shadow, Divine, Sonic Boom, Sculk Soul, Nautilus, Dragon Breath, Enchant Runes, Electric Spark ฯลฯ
   - เมนู **Particle Studio** ใน `/mitem` ให้เลือกทั้งชนิดละอองและรูปแบบการเคลื่อนไหว (Motion) ได้ด้วยคลิกเดียว

2. **Any-Item Custom Upgrade (ถือไอเท็มอะไรก็ใช้อัปเกรดได้ทันที)**:
   - ถือไอเท็มหรือวัตถุ Custom ชิ้นใดก็ได้ในมือซ้าย (Off-Hand) ➔ กดสร้างสูตรใน `/mitem upgrade` ระบบจะดึงชื่อไอเท็มนั้นมาเป็นวัตถุดิบให้อัตโนมัติทันที
   - กำหนดสเตตัส RPG ที่ต้องการเพิ่ม หรือเลือก `CMD` เพื่อผูกคำสั่งเข้าสู่อาวุธ
   - คลิกขวาที่สูตรเพื่อลบ หรือคลิกปุ่มลาวาเพื่อรีเซ็ตสูตรทั้งหมด

3. **Edit Saved Items directly from Catalog (/mitem browse)**:
   - ในหน้าแคตตาล็อก `/mitem browse` สามารถกด **Shift + คลิก** ที่ไอเท็มใดๆ ที่เคยเซฟไว้ เพื่อ **โหลดเข้ามือและเปิดหน้า Editor แก้ไขต่อได้ทันที** โดยไม่ต้องพิมพ์คำสั่งใหม่

4. **100% In-Game Working Abilities & Functions**:
   - บันทึกสกิลติดอาวุธ (Warp Blink, Grapple Hook, Celestial Slash, Thunder Strike, Heal, ฯลฯ) ลง PersistentDataContainer โดยตรง อาวุธที่สร้างใช้งานได้จริง 100% ในเกม

5. **Description Placeholders & Unlimited Lines**:
   - พิมพ์ข้อความหรือคูลดาวน์เองได้อิสระ รองรับ `%sec%`, `%cooldown%`, `%cd%` (เช่น `⌛ Cooldown: %sec% วิ`)
   - ซ่อนบรรทัดระบบออโต้ไม่ให้ขึ้นมาทับซ้อน

6. **Limits**:
   - Enchantment สูงสุด **255**
   - Potion Effect สูงสุด **250** (9999s) รองรับทั้งโหมดถือ (HOLD) และคลิกใช้งาน (RIGHT/LEFT)

---

## ⌨️ Commands & Usages

| Command | Description |
|---|---|
| `/mitem` | Open In-Hand Item Editor GUI |
| `/mitem browse` | Open saved items catalog (Shift-Click to EDIT saved item) |
| `/mitem upgrade` | Open weapon Upgrade & Smithing Station |
| `/mitem name <title/clear>` | Set or clear item display title |
| `/mitem desc add <text...>` | Add a description line (supports `%sec%`, `%cd%`, hex colors) |
| `/mitem desc edit <line#> <text...>` | Edit a description line by number |
| `/mitem desc remove <line#>` | Delete a description line by number |
| `/mitem desc clear` | Clear all description lines |
| `/mitem enchant <enchantment> [1-255]` | Add enchantment level |
| `/mitem potion <effect_name> [1-250]` | Add potion effect |
| `/mitem potion trigger <hold/right/left>` | Set potion trigger mode |
| `/mitem command add <cmd...>` | Attach command to weapon (hidden from lore) |
| `/mitem command click <left/right>` | Change command trigger to left-click or right-click |
| `/mitem command clear` | Remove attached commands |
| `/mitem effect <aura>` | Attach visual particle aura |
| `/mitem rarity <tier/toggle>` | Cycle tier or toggle rarity banner |
| `/mitem save <id>` | Save held item into browse catalog |
| `/mitem get <id> [amount]` | Take item from catalog |
| `/mitem give <player> <id> [amount]` | Give item to a player |
| `/mitem stats` | Inspect player's active RPG stats |
| `/mitem reload` | Reload configurations and upgrade recipes |

---

## 📦 File Output
- `MeryITEM.jar`
- `MeryITEM-1.0.0.jar`
