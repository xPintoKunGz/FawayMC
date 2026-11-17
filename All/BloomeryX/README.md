# 🌸 BloomeryX — Modern Server Essentials Suite

**BloomeryX** is an all-in-one, high-performance Essentials-style server utility plugin designed with modern architecture, fast command execution, and rich sound feedback. It features full **LuckPerms** & **PlaceholderAPI** integration, synchronized **Nametag & Tablist**, custom **Note Block Studio (.nbs) Jukebox Engine**, **AFK Zone & Auto-Teleport**, **Player & World Freeze**, and **@Mention chat notifications**.

---

## 🌟 Key Features

### 1. 🏷️ Nametag & Tablist (LuckPerms & PlaceholderAPI)
- **100% PlaceholderAPI (PAPI) Compatible**:
  - Supports all placeholders (e.g. `%luckperms_prefix%`, `%luckperms_suffix%`, `%vault_prefix%`, `%player_ping%`, etc.).
  - Can be used in floating nametags above player heads, Tablist, and chat formatting.
- **Head Nametags with Zero Sidebar Conflict**:
  - Uses native Minecraft Scoreboard Teams without forcing a sidebar objective.
  - Compatible with third-party scoreboard plugins (FeatherBoard, TAB, QuickBoard) with zero screen flickering.
- **Real-Time Synchronization**:
  - Dynamic cache checks ensure packets are only sent when metadata or nicknames actually change.
  - Pressing **TAB** shows rank, prefix, nickname, and suffix cleanly for all online players.

---

### 2. 🎼 Ultra-Intuitive Jukebox 2.0 (`.nbs` & Vanilla Discs)
- **Zero Dependencies**:
  - Does NOT require `NoteBlockAPI` or any external jars. Built-in high-performance `.nbs` parser supporting Open Note Block Studio formats (all 16 Minecraft instruments).
  - Requires **ZERO resource packs**.
- **Super-Intuitive Modern GUI (`/music` or `/jukebox`)**:
  - **Rows 1 to 5 (Slots 0 to 44)**: 45 songs per page! Click any disc to play immediately. Currently playing song glows with an enchanted effect.
  - **Row 6 (Slots 45 to 53) — Bottom Control Dock**:
    - `[ ◀ Previous Page ]` (Arrow)
    - `[ ⏮ Previous Song ]` (Prismarine)
    - `[ ⏸ Pause / ▶ Resume ]` (Green/Red Dye: instant pause and resume from the exact tick)
    - `[ ⏹ Stop Music ]` (Red Barrier: stop playback)
    - `[ ⏭ Next Song ]` (Prismarine)
    - `[ 🔀 Shuffle ]` (Hopper: ON / OFF)
    - `[ 🔁 Loop ]` (Repeater: Off / Repeat One / Repeat All)
    - `[ 🔊 Volume ]` (Note Block: 20% -> 40% -> 60% -> 80% -> 100%)
    - `[ Next Page ▶ ]` (Arrow)
- **Entity-Attached Sound (Follows the player)**:
  - Sound is bound directly to the player entity (`ClientboundSoundEntityPacket`), so whether you walk, sprint, fly with elytra, or teleport, the music follows you seamlessly.
- **Animated Action Bar Visualizer**:
  - Displays a live equalizer soundwave and timer while music is playing:
    `♬ PLAYING [01:24 / 03:40] ılı.lıllılı. Song Title - Artist`
- **Right-Click Jukebox Block in the World**:
  - Right-clicking any vanilla Jukebox block immediately opens the BloomeryX Music menu.
- **External Songs Folder**:
  - Simply place `.nbs` files into `plugins/BloomeryX/songs/` and type `/music reload`.

---

### 3. ❄️ Freeze System (Player & World Freeze)
- `/freeze <player>` — Completely freezes a player's movement and camera rotation with an on-screen title alert.
- `/unfreeze <player>` — Releases the frozen player.
- `/freeze world` or `/freezeworld` — Pauses the entire world (entities stop, physics/liquid flow/redstone pauses, similar to `/tick freeze`).

---

### 4. 💤 AFK Zone & Inactivity Detection
- `/afk [reason]` — Toggle your AFK status.
- `/setafk` — Set the destination point for AFK players.
- `/delafk` — Remove the AFK destination point.
- Automatically teleports inactive players to the AFK zone after X minutes (configured in `config.yml`) and returns them to their previous location upon movement.

---

### 5. ⚡ Administrative & Punishment Tools
- `/ban <player> [reason]` & `/unban <player>` — Ban or unban a player.
- `/ban-ip <player|ip> [reason]` & `/unban-ip <ip>` — Ban or unban an IP address.
- `/kill <player>` — Instantly kill a target player.
- `/smite [player]` — Strike lightning down upon a player or the block being targeted.

---

### 6. 📢 Clean Broadcast & Chat Mention
- `/broadcast <message>` — Clean, modern server announcement:
  `[Broadcast] : <message>`
- **Chat Mentions**: Typing `@PlayerName` highlights their name in chat, plays a bell chime, and displays an on-screen title alert.

---

### 7. 🌸 Full Essentials Shortcuts
- Gamemode: `/gm <0-3/s,c,a,sp>`, `/gms`, `/gmc`, `/gma`, `/gmsp`
- Time & Weather: `/day`, `/night`, `/noon`, `/midnight`, `/sun`, `/rain`, `/storm`
- Workbenches: `/craft`, `/ec`, `/anvil`, `/smithing`, `/loom`, `/grindstone`
- Teleportation: `/home`, `/sethome`, `/delhome`, `/homes`, `/warp`, `/setwarp`, `/delwarp`, `/warps`, `/back`, `/tpa`, `/tpaccept`, `/tpdeny`
- Utilities: `/fly`, `/heal`, `/feed`, `/god`, `/speed`, `/vanish`, `/hat`, `/clear`, `/repair`, `/msg`, `/r`, `/socialspy`, `/invsee`, `/nick`
