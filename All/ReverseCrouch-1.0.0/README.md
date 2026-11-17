# ReverseCrouch (1.8.9 FastBridge for Modern Minecraft)

ปลั๊กอินสำหรับ **Paper / Spigot / Purpur** (Minecraft 1.21+ จนถึงเวอร์ชั่นล่าสุด 26.3) เพื่อนำสัมผัสการต่อบล็อก (Fast Bridge / Ninja Bridge) และระบบย่อที่กระฉับกระเฉงสไตล์ **1.8.9** กลับมาในเวอร์ชั่นใหม่ **โดยไม่ต้องเปิด/ปิดโหมดใดๆ ทั้งสิ้น (Always-On / Passive)**

---

## 🌟 ฟีเจอร์เด่น (Features)

1. **Passive FastBridging (ไม่ต้องพิมพ์คำสั่ง):**
   * ระบบจะตรวจจับเมื่อผู้เล่นถือบล็อกที่วางได้ ก้มหน้าเล็งขอบบล็อก และเดินถอยหลังเพื่อต่อสะพานโดยอัตโนมัติ
   * ป้องกันการก้าวตกขอบบล็อก (SafeWalk Edge-Lock) แม้ไม่กด Shift หรือกดไม่ทัน ทำให้วิ่งต่อสะพานได้ลื่นไหล ไม่ต้องกลัวตกหล่น
2. **1.8.9-Style Snappy Sneak Speed:**
   * ในเวอร์ชั่นใหม่ การกดย่อจะช้ามาก (Speed Multiplier อยู่ที่เพียง 0.3)
   * ปลั๊กอินนี้ใช้ `Attribute.SNEAKING_SPEED` ปรับสปีดการย่อให้คล่องตัวและกระฉับกระเฉงขึ้น (0.55) เหมือนจังหวะ Ninja Bridge ใน 1.8.9
3. **Smart Movement Detection:**
   * หากผู้เล่นวิ่งไปข้างหน้า, กระโดด, บิน หรือต่อสู้ PvP ระบบฟิสิกส์การเคลื่อนที่ของตัวเกมปกติจะไม่ถูกแทรกแซงเลยแม้แต่น้อย ทำงานเฉพาะตอนต่อสะพานเท่านั้น
4. **Zero Configuration Needed:**
   * ใส่ลงโฟลเดอร์ `plugins/` และเปิดเซิร์ฟเวอร์ก็พร้อมใช้งานได้ทันที

---

## 🛠️ การติดตั้ง (Installation)

1. นำไฟล์ `ReverseCrouch-1.0.0.jar` ไปวางในโฟลเดอร์ `plugins/` ของเซิร์ฟเวอร์ Paper / Spigot
2. รีสตาร์ทเซิร์ฟเวอร์ หรือใช้คำสั่ง `/reload confirm`
3. เข้าไปลองถือบล็อกแล้วเดินถอยหลังต่อสะพานได้ทันที!

---

## ⚙️ การตั้งค่า (`config.yml`)

```yaml
# เปิดใช้งานระบบแบบ Passive (ทำงานตลอดเวลา)
enabled: true

bridging:
  # ต้องถือบล็อกที่วางได้ในมือ
  require-holding-placeable-block: true
  # ต้องก้มหน้าเล็งขอบบล็อก
  require-looking-down: true
  min-pitch-angle: 30.0
  # ระบบกันตกขอบบล็อกขณะต่อสะพาน
  prevent-edge-fall: true
  # บล็อกที่อนุญาต (ว่างไว้ = ได้ทุกบล็อกที่วางได้)
  allowed-blocks: []

sneak-speed:
  # ปรับสปีดตอนกดย่อให้ว่องไวสไตล์ 1.8.9
  modify-speed: true
  speed-value: 0.55

feedback:
  # เล่นเสียงคลิกเบาๆ เมื่อระบบ SafeWalk ทำงาน (เปิด/ปิดได้)
  play-edge-sound: false
```

---

## 🔑 คำสั่งและ Permissions

* `/reversecrouch reload` — รีโหลดไฟล์ config (Permission: `reversecrouch.admin`)
* Permission `reversecrouch.use` — อนุญาตให้ใช้งานระบบ (ค่าเริ่มต้น: ให้ทุกคน `true`)
* Permission `reversecrouch.bypass` — ยกเว้นผู้เล่นจากระบบนี้ (ค่าเริ่มต้น: `false`)

