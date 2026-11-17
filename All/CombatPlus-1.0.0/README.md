# ⚔️ CombatPlus (Minecraft 26.3)
### 👑 Made By FawayMC

ปลั๊กอินสำหรับ **Paper / Purpur / Spigot Minecraft เวอร์ชั่น 26.3** ที่นำระบบ **Sword Blocking (การกดคลิกขวาแล้วเอาดาบมานาบตัวป้องกันเหมือนในเวอร์ชั่น 1.8.9)** กลับมาให้ใช้งานได้อย่างสมบูรณ์แบบบนตัวเกมเวอร์ชั่นใหม่ล่าสุด พร้อมตัวเปิดเซิร์ฟเวอร์แบบพร้อมใช้งานทันที (`start.bat` / `start.ps1` / `run.sh`)!

---

## ✨ จุดเด่นและฟีเจอร์หลัก (Features)

1. **ดาบนาบตัวป้องกันเหมือน 1.8.9 เป๊ะๆ (Native 1.8 Sword Blocking Animation):**
   - ใช้เทคโนโลยี **Item Data Component** ของ Minecraft เวอร์ชั่นใหม่ (`minecraft:consumable` ร่วมกับ `animation: block` และ `minecraft:blocks_attacks`)
   - ผู้เล่นทั้งมุมมองบุคคลที่หนึ่ง (First-person) และมุมมองบุคคลที่สาม (Third-person) จะเห็นท่าทางเอาดาบมานาบที่ลำตัวเพื่อป้องกันอย่างเป็นธรรมชาติในวานิลลา โดยไม่ต้องลง Mod เสริมที่ฝั่ง Client
2. **รองรับดาบทุกชนิดและทุกวิธีเสก 100%:**
   - ดาบไม้, หิน, เหล็ก, ทอง, เพชร, เนเธอไรต์
   - รองรับทั้งการเสกจากหน้าต่าง Creative Mode (ทุกแท็บ), คำสั่ง `/give`, ดรอปจากมอนสเตอร์ หรือเปิดเจอในหีบสมบัติ
   - มีระบบ Auto-Sync Scanner ตรวจจับและแปลงดาบอัตโนมัติแบบ Real-time ทุกๆ 0.25 วินาที
3. **ระบบป้องกันดาเมจ (Damage Reduction):**
   - ลดความเสียหายที่ได้รับลง **50%** (ตรงตามกลไกของ 1.8.9 ดั้งเดิม) หรือสามารถปรับแต่งเป็น 100% (เหมือนโล่) ได้ตามต้องการใน `config.yml`
4. **ป้องกันและสะท้อนลูกธนู/กระสุน (Projectile Deflection):**
   - สามารถยกดาบสะท้อนลูกธนูหรือกระสุนที่ยิงเข้ามาด้านหน้าได้ พร้อมเอฟเฟกต์กระดอนกลับ
5. **ลดแรงผลักจากการถูกโจมตี (Knockback Reduction):**
   - ขณะกดคลิกขวาบล็อกดาเมจ จะลดแรงกระเด็นลง 50% เหมือนกลไกคอมแบท 1.8.9
6. **เสียงและเอฟเฟกต์สมจริง (Sound & Particle Effects):**
   - เล่นเสียงดาบปะทะ/โล่ป้องกัน (`ITEM_SHIELD_BLOCK`) และเอฟเฟกต์สะเก็ดประกายไฟเมื่อบล็อกการโจมตีสำเร็จ
7. **ลดความคงทนของดาบอย่างสมดุล (Durability Damage):**
   - ความคงทนของดาบลดลง 1 แต้ม (หรือปรับแต่งได้) ต่อการบล็อกแต่ละครั้ง โดยคำนวณ Enchantment Unbreaking ตามปกติ

---

## 🚀 วิธีติดตั้งและใช้งานกับตัวเปิดเซิร์ฟเวอร์ (Server Runner)

ในโฟลเดอร์นี้มีไฟล์ปลั๊กอินที่คอมไพล์เสร็จเรียบร้อยแล้ว: `CombatPlus-1.0.0.jar`

### วิธีเปิดเซิร์ฟเวอร์ง่ายๆ (สำหรับ Windows):
1. ดับเบิ้ลคลิกเปิดไฟล์ **`start.bat`** (หรือรัน `start.ps1` ผ่าน PowerShell)
2. สคริปต์จะทำงานโดยอัตโนมัติ:
   - ตรวจหา Java 21 / Java 25 บนเครื่อง
   - หากยังไม่มีไฟล์เซิร์ฟเวอร์ สคริปต์จะช่วยดาวน์โหลด `paper.jar` เวอร์ชั่น 26.3 ให้ทันที
   - คัดลอก `CombatPlus-1.0.0.jar` เข้าโฟลเดอร์ `plugins/` ให้อัตโนมัติ
   - ตั้งค่า `eula.txt` เป็น `eula=true` ให้เรียบร้อย
   - รันเซิร์ฟเวอร์ด้วยค่า JVM Flags (Aikar's Flags) ที่เพิ่มประสิทธิภาพสูงสุดสำหรับ Paper
   - มีระบบ Auto-Restart ถอยหลัง 5 วินาทีหากเซิร์ฟเวอร์ปิดตัวลง

### สำหรับ Linux / macOS:
```bash
chmod +x run.sh
./run.sh
```

---

## 🛠️ คำสั่งในเกม (Commands) & Permissions

| คำสั่ง | รายละเอียด | สิทธิ์ (Permission) |
|---|---|---|
| `/combatplus help` | แสดงเมนูช่วยเหลือ (แสดง Made By FawayMC) | ทุกคน |
| `/combatplus toggle` | สลับเปิด/ปิดการยกดาบป้องกันสำหรับตัวเอง | `combatplus.toggle` (เปิดใช้งานเป็นค่าเริ่มต้น) |
| `/combatplus info` | ดูข้อมูลการตั้งค่าและสถานะปลั๊กอิน | ทุกคน |
| `/combatplus reload` | โหลดการตั้งค่าจาก `config.yml` ใหม่ทันที | `combatplus.admin` (OP) |
| `/combatplus give <ผู้เล่น> [ดาบ]` | เสกดาบที่ติดตั้งระบบป้องกันให้ผู้เล่น | `combatplus.admin` (OP) |

**คำสั่งย่อ (Aliases):** `/cp`, `/combat`, `/blocksword`

---

## ⚙️ การตั้งค่า `plugins/CombatPlus/config.yml`

```yaml
# ========================================================
#              CombatPlus - Configuration
#                  Made By FawayMC
#    Classic 1.8.9 Sword Blocking for Minecraft 26.3
# ========================================================

# เปิดหรือปิดใช้งานระบบ Sword Blocking
enabled: true

# เปอร์เซ็นต์การลดดาเมจเมื่อยกดาบป้องกัน (1.8.9 ดั้งเดิมคือ 50.0%)
damage-reduction-percent: 50.0

# มุมองศาในการป้องกันด้านหน้า (180.0 คือครึ่งวงกลมด้านหน้า)
horizontal-blocking-angle: 180.0

# เสียงเมื่อบล็อกการโจมตี
block-sound: "minecraft:item.shield.block"

# ดีเลย์ก่อนเริ่มบล็อก (0.0 คือยกปุ๊บบล็อกทันทีเหมือน 1.8.9)
block-delay-seconds: 0.0

# สะท้อนลูกธนู/กระสุนหรือไม่
deflect-projectiles: true

# แสดงเอฟเฟกต์สะเก็ดไฟเมื่อบล็อก
play-particles: true

# ดาบเสียความทนทานกี่แต้มต่อการบล็อก 1 ครั้ง
durability-damage: 1

# เปอร์เซ็นต์การลดแรงกระเด็น (Knockback Reduction)
knockback-reduction-percent: 50.0

# ต้องมี Permission "combatplus.block" หรือไม่
require-permission: false

# โลกที่ต้องการปิดระบบนี้
disabled-worlds:
  - "example_disabled_world"
```

---

## 📁 โครงสร้างโปรเจกต์ (Project Structure)

```
c:\Users\User\Downloads\Combat\
├── CombatPlus-1.0.0.jar      # ไฟล์ปลั๊กอินที่คอมไพล์เสร็จแล้ว (Made By FawayMC)
├── pom.xml                   # ไฟล์โครงสร้างสำหรับ Maven
├── start.bat                 # ตัวเปิดเซิร์ฟเวอร์สำหรับ Windows (Batch)
├── start.ps1                 # ตัวเปิดเซิร์ฟเวอร์สำหรับ Windows (PowerShell)
├── run.sh                    # ตัวเปิดเซิร์ฟเวอร์สำหรับ Linux / macOS
├── README.md                 # คู่มือการใช้งานภาษาไทย
└── src/                      # ซอร์สโค้ด Java ทั้งหมด
    └── main/
        ├── java/com/combatplus/
        │   ├── CombatPlus.java
        │   ├── command/CombatPlusCommand.java
        │   ├── config/ConfigManager.java
        │   ├── listener/SwordBlockListener.java
        │   └── service/SwordService.java
        └── resources/
            ├── plugin.yml
            ├── paper-plugin.yml
            └── config.yml
```
