# ✦ HoloMery (v1.0.0) ✦
> **Next-Generation Floating Hologram Plugin for Minecraft (1.20 - 1.21.x - 26.3 และทุกเวอร์ชั่นในอนาคต)**  
> โฮโลแกรมลอยได้ยุคใหม่: รองรับ **Interactive Click Commands & Page Jumps (คลิกสั่งการ/ข้ามหน้าทันที)**, **GUI แก้ไขในเกมแบบ In-Place ไม่ปิดเมนู**, **สลับย้ายตำแหน่งบรรทัด (Line Reorder)**, **ไอเทมลอยได้ 3D**, **บล็อกลอยได้ 3D**, **มอนสเตอร์ลอยได้**, **แอนิเมชันหมุน 360° รอบทิศ**, **ระบบหลายหน้า** และ **Multi-Stop RGB Gradient Engine**

---

## 🌟 ฟีเจอร์เด่นระดับพรีเมียม (Premium Features)

1. **⚡ ระบบคลิกสั่งการ & กระโดดข้ามหน้าทันที (Click Actions & Page Jumps):**
   - **ข้ามไปยังหน้าที่ต้องการทันที:** เช่น หน้าแรกมีปุ่มให้กดข้ามไปหน้า 2 หรือหน้า 3 ได้ทันที
   - **คลิกแล้วรันคำสั่งทันที:**
     - `[player] <คำสั่ง>` : ผู้เล่นรันคำสั่งเอง เช่น `[player] spawn`, `[player] warp pvp`, `[player] shop`
     - `[console] <คำสั่ง>` : คอนโซลรันคำสั่ง (ให้ของ/ให้เงิน/ให้ยศ) เช่น `[console] give %player_name% diamond 1`
     - `[message] <ข้อความ>` : ส่งข้อความแชทไล่สี Gradient ให้ผู้เล่นที่คลิก
     - `[sound] <ชื่อเสียง>` : เล่นเสียงเอฟเฟกต์ เช่น `[sound] ENTITY_PLAYER_LEVELUP`
     - `[server] <ชื่อเซิร์ฟ>` : วาร์ปข้ามเซิร์ฟเวอร์ BungeeCord / Velocity
   - **ตั้งค่าได้ง่ายๆ ใน GUI:** คลิกที่ปุ่ม **`⚡ Click Actions`** (Slot 1 แถวบน) ในเมนู `/hm edit <id>` หรือใช้คำสั่ง `/hm action <id> add ...`

2. **🖥️ เมนู GUI แก้ไขในเกมแบบ In-Place (กดปรับค่าแล้วเมนูไม่ปิด):**
   - พิมพ์ `/hm gui` หรือ `/hm edit <id>`
   - **เปิด/ปิดพื้นหลังสีดำได้ในคลิกเดียว (Slot 48):** สลับระหว่างกล่องสีดำแบบมายคราฟ กับแบบโปร่งใสไร้กล่องดำสวยงาม
   - ปรับ Scale, Spacing, Billboard, Shadow, Background, Flip Page ได้รัวๆ โดยเมนู **ไม่ปิดและไม่กระพริบ**
   - คลิกซ้ายที่บรรทัดเพื่อแก้ข้อความผ่านแชทได้ทันที

3. **↕️ สลับย้ายตำแหน่งบรรทัดได้อย่างอิสระ (Line 1, 2, 3...):**
   - **ใน GUI:**
     - **Shift + คลิกซ้ายที่บรรทัด:** ย้ายบรรทัดขึ้นทันที (▲ Move Up)
     - **Shift + คลิกขวาที่บรรทัด:** ย้ายบรรทัดลงทันที (▼ Move Down)
   - **ผ่านคำสั่ง:**
     - `/hm swap <id> <บรรทัด1> <บรรทัด2>` : สลับตำแหน่ง 2 บรรทัด
     - `/hm move <id> <จากบรรทัด> <ไปบรรทัด>` : ย้ายบรรทัดไปยังตำแหน่งใหม่

4. **🔄 แอนิเมชันการหมุน: หมุนรอบ 360° ตลอดเวลา หรือหมุนตามผู้เล่น:**
   - **หมุนรอบ 360° (Spin Animation):** ไอเทม, บล็อก และมอนสเตอร์จะหมุนรอบตัวเองอย่างราบรื่น 60+ FPS
   - **หมุนตามผู้เล่น (Billboard CENTER):** หันหน้าตามมุมมองกล้องผู้เล่นตลอดเวลา
   - เปิด/ปิดได้ด้วย 1 คลิกใน GUI (ปุ่มแท่ง Blaze Rod แถวบน) หรือพิมพ์ `/hm spin <id>`

5. **⬛ เปิด/ปิดพื้นหลังสีดำของตัวหนังสือ (Black Background Toggle):**
   - **ปิดพื้นหลังดำ (โปร่งใส สวยงาม ไร้กล่องดำ):** ค่าเริ่มต้นของปลั๊กอินทำให้โฮโลแกรมลอยได้เนียนตา
   - **เปิดพื้นหลังดำ (Black Box):** แสดงกล่องสี่เหลี่ยมสีดำโปร่งแสงแบบคลาสสิกของมายคราฟ
   - **ผ่าน GUI:** คลิกที่ปุ่ม Slot 48 (ไอคอน Black Concrete / Glass) สลับได้ทันทีในคลิกเดียว เมนูไม่ปิด
   - **ผ่านคำสั่ง:** `/hm bg <id> [on|off]` หรือ `/hm background <id> [on|off]` (พิมพ์ `/hm bg <id>` เพื่อสลับทันที)

5. **⚔️ ไอเทม 3D (`ItemDisplay`) & 🧱 บล็อก 3D (`BlockDisplay`):**
   - รูปแบบ: `ITEM:<ชื่อไอเทม>` เช่น `ITEM:NETHER_STAR`, `ITEM:DIAMOND_SWORD`
   - รูปแบบ: `BLOCK:<ชื่อบล็อก>` เช่น `BLOCK:BEACON`, `BLOCK:DRAGON_EGG`, `BLOCK:NETHERITE_BLOCK`
   - มาพร้อมระดับแสงสว่างเต็มที่ **Brightness 15/15** เรืองแสงได้ในทุกสภาพแสง

6. **🐉 มอนสเตอร์และสิ่งมีชีวิตลอยได้ (`Mob/Entity`):**
   - รูปแบบ: `MOB:<ชื่อมอนสเตอร์>` เช่น `MOB:ALLAY`, `MOB:BLAZE`, `MOB:WARDEN`, `MOB:CAT`
   - ไม่ส่งเสียง, ไร้ AI, ตีไม่ตาย และปรับขนาดย่อ/ขยายได้

---

## 🕹️ คำสั่งการคลิกโฮโลแกรม (Click Actions Command)

| รูปแบบคำสั่ง | ตัวอย่างการใช้งาน | คำอธิบาย |
|---|---|---|
| `/hm action <id> add PAGE:<เลขหน้า>` | `/hm action hub add PAGE:2` | คลิกแล้วข้ามไปหน้า 2 ทันที |
| `/hm action <id> add PAGE:NEXT` | `/hm action hub add PAGE:NEXT` | คลิกแล้วข้ามไปหน้าถัดไป |
| `/hm action <id> add [player] <cmd>` | `/hm action hub add [player] warp pvp` | คลิกแล้วให้ผู้เล่นพิมพ์คำสั่งวาร์ป |
| `/hm action <id> add [console] <cmd>` | `/hm action hub add [console] give %player_name% diamond 5` | คอนโซลแจกของให้ผู้เล่นที่คลิก |
| `/hm action <id> add [message] <text>` | `/hm action hub add [message] <cyberpunk>ยินดีต้อนรับ!</cyberpunk>` | ส่งข้อความแชทส่วนตัวให้ผู้เล่น |
| `/hm action <id> add [sound] <sound>` | `/hm action hub add [sound] ENTITY_EXPERIENCE_ORB_PICKUP` | เล่นเสียงเอฟเฟกต์เมื่อคลิก |
| `/hm action <id> list` | `/hm action hub list` | ดูรายการคำสั่งคลิกทั้งหมดของหน้านี้ |
| `/hm action <id> remove <index>` | `/hm action hub remove 1` | ลบคำสั่งคลิกตามลำดับ |
| `/hm action <id> clear` | `/hm action hub clear` | ล้างคำสั่งคลิกทั้งหมดของหน้านี้ |

---

## 💡 ตัวอย่างการสร้างโฮโลแกรมเมนูวาร์ป / ร้านค้าสุดอัจฉริยะ

```
# สร้างโฮโลแกรมหน้า 1 (หน้าเมนูหลัก)
/hm create menu <cyberpunk>&l✦ SERVER TELEPORT MENU ✦</cyberpunk>
/hm add menu BLOCK:BEACON
/hm add menu <sunset>คลิกเพื่อวาร์ปไปยังโซน Survival</sunset>
/hm add menu <neon>&l[คลิกที่นี่เพื่อไปหน้า 2 ▶]</neon>

# ตั้งค่าให้หน้า 1 เมื่อคลิกแล้วไปหน้า 2
/hm action menu add PAGE:2

# สร้างหน้า 2
/hm page menu add
/hm add menu <amethyst>&l✦ PVP WARZONE ✦</amethyst>
/hm add menu ITEM:DIAMOND_SWORD
/hm add menu <fire>คลิกเพื่อวาร์ปเข้าสู่สนามรบ PVP ทันที!</fire>

# ตั้งค่าให้หน้า 2 เมื่อคลิกแล้วรันคำสั่งวาร์ปทันที!
/hm action menu add [player] warp pvp
/hm action menu add [sound] ENTITY_ENDERMAN_TELEPORT
```

---

## 📦 การติดตั้ง
นำไฟล์ [`HoloMery-1.0.0.jar`](file:///c:/Users/User/Downloads/HoloMery/HoloMery-1.0.0.jar) ไปวางในโฟลเดอร์ `plugins/` ของเซิร์ฟเวอร์ และเริ่มเซิร์ฟเวอร์ได้ทันที!
