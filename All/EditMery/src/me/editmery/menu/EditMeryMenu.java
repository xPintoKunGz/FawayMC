package me.editmery.menu;

import me.editmery.ColorUtil;
import me.editmery.EditMeryPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class EditMeryMenu {

    public static void openMenu(Player player, MenuType type) {
        if (player == null) return;

        switch (type) {
            case SELECTION:
                openSelectionMenu(player);
                break;
            case REGION:
                openRegionMenu(player);
                break;
            case CLIPBOARD:
                openClipboardMenu(player);
                break;
            case SHAPES:
                openShapesMenu(player);
                break;
            case UTILITIES:
                openUtilitiesMenu(player);
                break;
            case HISTORY:
                openHistoryMenu(player);
                break;
            case MAIN:
            default:
                openMainMenu(player);
                break;
        }
    }

    // ==========================================
    // 1. MAIN MENU
    // ==========================================
    public static void openMainMenu(Player player) {
        EditMeryMenuHolder holder = new EditMeryMenuHolder(MenuType.MAIN);
        Inventory inv = Bukkit.createInventory(holder, 54, ColorUtil.colorize("&#B388FF✦ &#C084FCE&#D080FAd&#E07CF8i&#F078F6t&#FF74F4M&#FF79DDe&#FF7EC6r&#FF83AFy &#FF83AF✦ &8» &8[&7เมนูคำสั่ง&8]"));
        holder.setInventory(inv);

        fillBorders(inv, holder, Material.BLACK_STAINED_GLASS_PANE);

        // Header / Info
        boolean isEnabled = EditMeryPlugin.getEditMeryInstance().getManager().isEnabled(player);
        String statusColor = isEnabled ? "&#06D6A0● เปิดใช้งาน (ENABLED)" : "&#EF476F● ปิดการใช้งาน (DISABLED)";
        
        ItemStack infoItem = createItem(Material.NETHER_STAR,
                "&#B388FF✦ &#FF80ABระบบจัดการ EditMery &8[&7WorldEdit Edition&8]",
                "&7รวมคำสั่ง WorldEdit ทุกคำสั่งอย่างละเอียดและครบถ้วน",
                "&7คลิกเมนูด้านล่างเพื่อเลือกหมวดหมู่ที่ต้องการใช้งาน",
                "",
                "&8• &fสถานะปัจจุบัน: " + statusColor,
                "&8• &fผู้พัฒนา: &#FF80ABxPintoKunG_Z &7& &#B388FFPeak4567",
                "&8• &fเวอร์ชัน: &eMC 26.3 &8(&bFastAsyncWorldEdit 2.16.1&8)");
        holder.setMenuItem(4, new MenuItem(infoItem, (String) null));

        // Category 1: Selection & Wand
        ItemStack wandCat = createItem(Material.WOODEN_AXE,
                "&#B388FF✦ &#C084FCหมวด: การเลือกพื้นที่ & คฑา &8[คลิกเปิด]",
                "&7รวบรวมคำสั่งกำหนดจุด //pos1, //pos2, //wand",
                "&7ขยายหดพื้นที่ //expand, //contract, //size ฯลฯ",
                "",
                "&#FFD166➜ คลิกเพื่อเปิดเมนูหมวดนี้");
        holder.setMenuItem(11, new MenuItem(wandCat, MenuType.SELECTION));

        // Category 2: Region & Block Operations
        ItemStack regionCat = createItem(Material.STONE_BRICKS,
                "&#B388FF✦ &#FF80ABหมวด: จัดการบล็อก & พื้นผิว &8[คลิกเปิด]",
                "&7รวบรวมคำสั่งวางบล็อก //set, สลับบล็อก //replace",
                "&7สร้างกำแพง //walls, กรอบ //faces, เจาะกลวง //hollow",
                "&7ปรับธรรมชาติ //naturalize, เกลี่ยเนียน //smooth ฯลฯ",
                "",
                "&#FFD166➜ คลิกเพื่อเปิดเมนูหมวดนี้");
        holder.setMenuItem(13, new MenuItem(regionCat, MenuType.REGION));

        // Category 3: Clipboard & Transform
        ItemStack clipCat = createItem(Material.MAP,
                "&#B388FF✦ &#06D6A0หมวด: คลิปบอร์ด & คัดลอก/วาง &8[คลิกเปิด]",
                "&7รวบรวมคำสั่งคัดลอก //copy, ตัด //cut",
                "&7วางบล็อก //paste, วางไม่ทับอากาศ //paste -a",
                "&7หมุนทิศ //rotate, พลิกด้าน //flip, ไฟล์ Schematic ฯลฯ",
                "",
                "&#FFD166➜ คลิกเพื่อเปิดเมนูหมวดนี้");
        holder.setMenuItem(15, new MenuItem(clipCat, MenuType.CLIPBOARD));

        // Category 4: Shapes & Generation
        ItemStack shapeCat = createItem(Material.SEA_LANTERN,
                "&#B388FF✦ &#FFD166หมวด: สร้างรูปทรงเรขาคณิต &8[คลิกเปิด]",
                "&7รวบรวมคำสั่งทรงกระบอก //cyl, ทรงกลม //sphere",
                "&7ปิรามิด //pyramid, เสกป่า //forest, ไร่ฟักทอง ฯลฯ",
                "",
                "&#FFD166➜ คลิกเพื่อเปิดเมนูหมวดนี้");
        holder.setMenuItem(29, new MenuItem(shapeCat, MenuType.SHAPES));

        // Category 5: Utilities & Tools
        ItemStack utilCat = createItem(Material.WATER_BUCKET,
                "&#B388FF✦ &#64B5F6หมวด: เครื่องมือ & อำนวยความสะดวก &8[คลิกเปิด]",
                "&7รวบรวมคำสั่งสูบน้ำ/ลาวา //drain, ซ่อมน้ำ //fixwater",
                "&7ดับไฟ //extinguish, ทะลุกำแพง //thru, วาร์ป //jumpto",
                "&7เสากระจก //up, แปรงพ่นบล็อก /brush ฯลฯ",
                "",
                "&#FFD166➜ คลิกเพื่อเปิดเมนูหมวดนี้");
        holder.setMenuItem(31, new MenuItem(utilCat, MenuType.UTILITIES));

        // Category 6: History & Settings
        ItemStack histCat = createItem(Material.CLOCK,
                "&#B388FF✦ &#EF476Fหมวด: ประวัติคำสั่ง & ตั้งค่าโหมด &8[คลิกเปิด]",
                "&7ย้อนกลับการกระทำ //undo, ทำซ้ำ //redo",
                "&7เปิดโหมดขวาน /em enable, ปิดโหมด /em disable",
                "&7ตรวจสอบสถานะ /em status, รีโหลดคอนฟิก ฯลฯ",
                "",
                "&#FFD166➜ คลิกเพื่อเปิดเมนูหมวดนี้");
        holder.setMenuItem(33, new MenuItem(histCat, MenuType.HISTORY));

        // Center Quick Toggle Status Button
        ItemStack toggleItem = isEnabled ?
                createItem(Material.EMERALD_BLOCK,
                        "&#06D6A0● โหมด EditMery: เปิดใช้งานอยู่",
                        "&7ขวานไม้ทำหน้าที่เป็นคฑา Wand และคำสั่ง // ใช้งานได้",
                        "",
                        "&#EF476F➜ คลิกเพื่อปิดโหมดขวาน (/em disable)") :
                createItem(Material.REDSTONE_BLOCK,
                        "&#EF476F● โหมด EditMery: ปิดการใช้งานอยู่",
                        "&7ขวานไม้ทำหน้าที่เป็นขวานตัดไม้ธรรมดา ทุบบล็อกได้ปกติ",
                        "",
                        "&#06D6A0➜ คลิกเพื่อเปิดโหมดขวาน (/em enable)");
        holder.setMenuItem(22, new MenuItem(toggleItem, p -> {
            boolean newState = EditMeryPlugin.getEditMeryInstance().getManager().toggle(p);
            EditMeryPlugin.getEditMeryInstance().getManager().sendToggleSuccess(p, newState);
            openMainMenu(p);
        }));

        // Quick Bottom Actions
        ItemStack quickUndo = createItem(Material.ARROW, "&#FFD166↶ ย้อนกลับคำสั่งด่วน (//undo)", "&7คลิกเพื่อย้อนกลับสิ่งก่อสร้างล่าสุดทันที");
        holder.setMenuItem(48, new MenuItem(quickUndo, "//undo"));

        ItemStack closeBtn = createItem(Material.BARRIER, "&#EF476F✖ ปิดเมนู", "&7คลิกเพื่อออกจากเมนู");
        holder.setMenuItem(49, new MenuItem(closeBtn, p -> p.closeInventory()));

        ItemStack quickRedo = createItem(Material.SPECTRAL_ARROW, "&#06D6A0↷ ทำซ้ำคำสั่งด่วน (//redo)", "&7คลิกเพื่อทำซ้ำสิ่งก่อสร้างที่เพิ่งย้อนกลับ");
        holder.setMenuItem(50, new MenuItem(quickRedo, "//redo"));

        player.openInventory(inv);
        ColorUtil.playSound(player, "BLOCK_CHEST_OPEN", 0.8f, 1.2f);
    }

    // ==========================================
    // 2. SELECTION & WAND SUB-MENU
    // ==========================================
    private static void openSelectionMenu(Player player) {
        EditMeryMenuHolder holder = new EditMeryMenuHolder(MenuType.SELECTION);
        Inventory inv = Bukkit.createInventory(holder, 54, ColorUtil.colorize("&#B388FF✦ เมนู: การเลือกพื้นที่ & คฑา Wand"));
        holder.setInventory(inv);

        fillBorders(inv, holder, Material.PURPLE_STAINED_GLASS_PANE);

        holder.setMenuItem(10, new MenuItem(createItem(Material.WOODEN_AXE,
                "&#B388FF✦ เสกขวานไม้คฑา (//wand)",
                "&7รับขวานไม้เพื่อใช้เลือกพิกัด Pos 1 (คลิกซ้าย) และ Pos 2 (คลิกขวา)",
                "&#FFD166➜ คลิกเพื่อเสกขวานทันที"), "//wand"));

        holder.setMenuItem(11, new MenuItem(createItem(Material.COMPASS,
                "&#C084FC✦ กำหนดจุดที่ 1 ที่เท้า (//pos1)",
                "&7ตั้งค่าจุดตำแหน่งแรกตามพิกัดที่ตัวคุณยืนอยู่",
                "&#FFD166➜ คลิกเพื่อรันคำสั่ง"), "//pos1"));

        holder.setMenuItem(12, new MenuItem(createItem(Material.TARGET,
                "&#FF80AB✦ กำหนดจุดที่ 2 ที่เท้า (//pos2)",
                "&7ตั้งค่าจุดตำแหน่งที่สองตามพิกัดที่ตัวคุณยืนอยู่",
                "&#FFD166➜ คลิกเพื่อรันคำสั่ง"), "//pos2"));

        holder.setMenuItem(13, new MenuItem(createItem(Material.ENDER_EYE,
                "&#C084FC✦ กำหนดจุดที่ 1 ตามที่มอง (//hpos1)",
                "&7ตั้งค่าจุดที่ 1 ตามบล็อกที่เป้าสายตาของคุณเล็งอยู่",
                "&#FFD166➜ คลิกเพื่อรันคำสั่ง"), "//hpos1"));

        holder.setMenuItem(14, new MenuItem(createItem(Material.ENDER_PEARL,
                "&#FF80AB✦ กำหนดจุดที่ 2 ตามที่มอง (//hpos2)",
                "&7ตั้งค่าจุดที่ 2 ตามบล็อกที่เป้าสายตาของคุณเล็งอยู่",
                "&#FFD166➜ คลิกเพื่อรันคำสั่ง"), "//hpos2"));

        holder.setMenuItem(15, new MenuItem(createItem(Material.BARRIER,
                "&#EF476F✦ ยกเลิกการเลือกพื้นที่ (//desel)",
                "&7ล้างการเลือกพื้นที่ปัจจุบันออกทั้งหมด",
                "&#FFD166➜ คลิกเพื่อรันคำสั่ง"), "//desel"));

        holder.setMenuItem(16, new MenuItem(createItem(Material.NAME_TAG,
                "&#06D6A0✦ ดูขนาดและปริมาตรพื้นที่ (//size)",
                "&7แสดงความกว้าง ยาว สูง และจำนวนบล็อกทั้งหมดที่เลือก",
                "&#FFD166➜ คลิกเพื่อรันคำสั่ง"), "//size"));

        // Row 2: Expansions
        holder.setMenuItem(19, new MenuItem(createItem(Material.ARROW,
                "&#64B5F6✦ ขยายพื้นที่ขึ้นบน 1 บล็อก (//expand 1 up)",
                "&7ขยายขอบเขตการเลือกขึ้นด้านบน 1 บล็อก",
                "&#FFD166➜ คลิกเพื่อรันคำสั่ง"), "//expand 1 up"));

        holder.setMenuItem(20, new MenuItem(createItem(Material.ARROW,
                "&#64B5F6✦ ขยายพื้นที่ลงล่าง 1 บล็อก (//expand 1 down)",
                "&7ขยายขอบเขตการเลือกลงด้านล่าง 1 บล็อก",
                "&#FFD166➜ คลิกเพื่อรันคำสั่ง"), "//expand 1 down"));

        holder.setMenuItem(21, new MenuItem(createItem(Material.SPECTRAL_ARROW,
                "&#FFD166✦ ขยายพื้นที่สุดขอบโลกบน-ล่าง (//expand vert)",
                "&7ขยายพื้นที่ตั้งแต่เพดานโลก (y:320) ถึงใต้โลก (y:-64)",
                "&#FFD166➜ คลิกเพื่อรันคำสั่ง"), "//expand vert"));

        holder.setMenuItem(22, new MenuItem(createItem(Material.FEATHER,
                "&#64B5F6✦ ขยายพื้นที่ขึ้นบน 10 บล็อก (//expand 10 up)",
                "&7ขยายขอบเขตการเลือกขึ้นด้านบน 10 บล็อก",
                "&#FFD166➜ คลิกเพื่อรันคำสั่ง"), "//expand 10 up"));

        holder.setMenuItem(23, new MenuItem(createItem(Material.ANVIL,
                "&#64B5F6✦ ขยายพื้นที่ลงล่าง 10 บล็อก (//expand 10 down)",
                "&7ขยายขอบเขตการเลือกลงด้านล่าง 10 บล็อก",
                "&#FFD166➜ คลิกเพื่อรันคำสั่ง"), "//expand 10 down"));

        holder.setMenuItem(24, new MenuItem(createItem(Material.SHEARS,
                "&#EF476F✦ หดพื้นที่จากด้านบน 1 บล็อก (//contract 1 up)",
                "&7ลดขอบเขตการเลือกลงจากด้านบน 1 บล็อก",
                "&#FFD166➜ คลิกเพื่อรันคำสั่ง"), "//contract 1 up"));

        holder.setMenuItem(25, new MenuItem(createItem(Material.CHAIN,
                "&#EF476F✦ หดพื้นที่จากด้านล่าง 1 บล็อก (//contract 1 down)",
                "&7ลดขอบเขตการเลือกขึ้นจากด้านล่าง 1 บล็อก",
                "&#FFD166➜ คลิกเพื่อรันคำสั่ง"), "//contract 1 down"));

        // Row 3: Shifts & Modes
        holder.setMenuItem(28, new MenuItem(createItem(Material.PISTON,
                "&#FFA726✦ เลื่อนพื้นที่ขึ้นบน 1 บล็อก (//shift 1 up)",
                "&7เลื่อนทั้งพื้นที่ที่เลือกขึ้นบนโดยไม่เปลี่ยนขนาด",
                "&#FFD166➜ คลิกเพื่อรันคำสั่ง"), "//shift 1 up"));

        holder.setMenuItem(29, new MenuItem(createItem(Material.STICKY_PISTON,
                "&#FFA726✦ เลื่อนพื้นที่ลงล่าง 1 บล็อก (//shift 1 down)",
                "&7เลื่อนทั้งพื้นที่ที่เลือกลงล่างโดยไม่เปลี่ยนขนาด",
                "&#FFD166➜ คลิกเพื่อรันคำสั่ง"), "//shift 1 down"));

        holder.setMenuItem(30, new MenuItem(createItem(Material.PUFFERFISH,
                "&#06D6A0✦ ขยายขอบเขตทุกทิศทาง 1 บล็อก (//outset 1)",
                "&7ขยายพื้นที่ออกไปรอบทิศทาง กว้าง ยาว สูง อย่างละ 1 บล็อก",
                "&#FFD166➜ คลิกเพื่อรันคำสั่ง"), "//outset 1"));

        holder.setMenuItem(31, new MenuItem(createItem(Material.NAUTILUS_SHELL,
                "&#EF476F✦ ลดขอบเขตทุกทิศทาง 1 บล็อก (//inset 1)",
                "&7ลดขนาดพื้นที่หดเข้ามาทุกทิศทางอย่างละ 1 บล็อก",
                "&#FFD166➜ คลิกเพื่อรันคำสั่ง"), "//inset 1"));

        holder.setMenuItem(32, new MenuItem(createItem(Material.EXPERIENCE_BOTTLE,
                "&#FFD166✦ นับจำนวนบล็อกในพื้นที่ (//count)",
                "&7นับสถิติจำนวนบล็อกในพื้นที่ที่เลือก",
                "&#FFD166➜ คลิกเพื่อรันคำสั่ง"), "//count"));

        holder.setMenuItem(33, new MenuItem(createItem(Material.BOOK,
                "&#B388FF✦ โหมดสี่เหลี่ยมลูกบาศก์ (//sel cuboid)",
                "&7เปลี่ยนโหมดการเลือกพื้นที่เป็นสี่เหลี่ยมลูกบาศก์ (ค่ามาตรฐาน)",
                "&#FFD166➜ คลิกเพื่อรันคำสั่ง"), "//sel cuboid"));

        holder.setMenuItem(34, new MenuItem(createItem(Material.WRITABLE_BOOK,
                "&#FF80AB✦ โหมดขยายตามจุด (//sel extend)",
                "&7เปลี่ยนโหมดเลือกพื้นที่เป็นแบบขยายจุดตามคลิก",
                "&#FFD166➜ คลิกเพื่อรันคำสั่ง"), "//sel extend"));

        addBackButton(inv, holder, MenuType.MAIN);
        player.openInventory(inv);
        ColorUtil.playSound(player, "BLOCK_CHEST_OPEN", 0.8f, 1.2f);
    }

    // ==========================================
    // 3. REGION & BLOCK OPERATIONS SUB-MENU
    // ==========================================
    private static void openRegionMenu(Player player) {
        EditMeryMenuHolder holder = new EditMeryMenuHolder(MenuType.REGION);
        Inventory inv = Bukkit.createInventory(holder, 54, ColorUtil.colorize("&#B388FF✦ เมนู: จัดการบล็อก & พื้นที่"));
        holder.setInventory(inv);

        fillBorders(inv, holder, Material.MAGENTA_STAINED_GLASS_PANE);

        // Palette Set
        holder.setMenuItem(10, new MenuItem(createItem(Material.AIR, "&#EF476F✦ ลบพื้นที่เป็นอากาศ (//set air)", "&7ลบทุกบล็อกในพื้นที่ที่เลือกให้ว่างเปล่า", "&#FFD166➜ คลิกเพื่อลบพื้นที่"), "//set air"));
        holder.setMenuItem(11, new MenuItem(createItem(Material.STONE, "&#B388FF✦ วางหิน (//set stone)", "&7เปลี่ยนทุกบล็อกในพื้นที่เป็นหินเรียบ", "&#FFD166➜ คลิกเพื่อวางบล็อก"), "//set stone"));
        holder.setMenuItem(12, new MenuItem(createItem(Material.DIRT, "&#B388FF✦ วางดิน (//set dirt)", "&7เปลี่ยนทุกบล็อกในพื้นที่เป็นบล็อกดิน", "&#FFD166➜ คลิกเพื่อวางบล็อก"), "//set dirt"));
        holder.setMenuItem(13, new MenuItem(createItem(Material.GRASS_BLOCK, "&#06D6A0✦ วางบล็อกหญ้า (//set grass_block)", "&7เปลี่ยนทุกบล็อกในพื้นที่เป็นบล็อกหญ้า", "&#FFD166➜ คลิกเพื่อวางบล็อก"), "//set grass_block"));
        holder.setMenuItem(14, new MenuItem(createItem(Material.GLASS, "&#64B5F6✦ วางกระจกใส (//set glass)", "&7เปลี่ยนทุกบล็อกในพื้นที่เป็นกระจกใส", "&#FFD166➜ คลิกเพื่อวางบล็อก"), "//set glass"));
        holder.setMenuItem(15, new MenuItem(createItem(Material.OAK_PLANKS, "&#FFA726✦ วางไม้โอ๊ค (//set oak_planks)", "&7เปลี่ยนทุกบล็อกในพื้นที่เป็นไม้แปรรูปโอ๊ค", "&#FFD166➜ คลิกเพื่อวางบล็อก"), "//set oak_planks"));
        holder.setMenuItem(16, new MenuItem(createItem(Material.STONE_BRICKS, "&#C084FC✦ วางหินอิฐ (//set stone_bricks)", "&7เปลี่ยนทุกบล็อกในพื้นที่เป็นบล็อกหินอิฐ", "&#FFD166➜ คลิกเพื่อวางบล็อก"), "//set stone_bricks"));

        holder.setMenuItem(19, new MenuItem(createItem(Material.BRICKS, "&#EF5350✦ วางอิฐแดง (//set bricks)", "&7เปลี่ยนทุกบล็อกในพื้นที่เป็นบล็อกอิฐแดง", "&#FFD166➜ คลิกเพื่อวางบล็อก"), "//set bricks"));
        holder.setMenuItem(20, new MenuItem(createItem(Material.GLOWSTONE, "&#FFD166✦ วางบล็อกแสง (//set glowstone)", "&7เปลี่ยนทุกบล็อกในพื้นที่เป็นโกลว์สโตนสว่าง", "&#FFD166➜ คลิกเพื่อวางบล็อก"), "//set glowstone"));
        holder.setMenuItem(21, new MenuItem(createItem(Material.SEA_LANTERN, "&#80DEEA✦ วางโคมสมุทร (//set sea_lantern)", "&7เปลี่ยนทุกบล็อกในพื้นที่เป็นโคมไฟซีแลนเทิร์น", "&#FFD166➜ คลิกเพื่อวางบล็อก"), "//set sea_lantern"));
        holder.setMenuItem(22, new MenuItem(createItem(Material.DIAMOND_BLOCK, "&#4DD0E1✦ วางบล็อกเพชร (//set diamond_block)", "&7เปลี่ยนทุกบล็อกในพื้นที่เป็นบล็อกเพชร", "&#FFD166➜ คลิกเพื่อวางบล็อก"), "//set diamond_block"));
        holder.setMenuItem(23, new MenuItem(createItem(Material.QUARTZ_BLOCK, "&#ECEFF1✦ วางหินควอตซ์ (//set quartz_block)", "&7เปลี่ยนทุกบล็อกในพื้นที่เป็นบล็อกควอตซ์ขาว", "&#FFD166➜ คลิกเพื่อวางบล็อก"), "//set quartz_block"));
        holder.setMenuItem(24, new MenuItem(createItem(Material.PACKED_ICE, "&#81D4FA✦ วางน้ำแข็งแน่น (//set packed_ice)", "&7เปลี่ยนทุกบล็อกในพื้นที่เป็นแพ็คน้ำแข็ง", "&#FFD166➜ คลิกเพื่อวางบล็อก"), "//set packed_ice"));
        holder.setMenuItem(25, new MenuItem(createItem(Material.POLISHED_DEEPSLATE, "&#78909C✦ วางดีพสเลต (//set polished_deepslate)", "&7เปลี่ยนทุกบล็อกในพื้นที่เป็นดีพสเลตขัดเงา", "&#FFD166➜ คลิกเพื่อวางบล็อก"), "//set polished_deepslate"));

        // Special Operations
        holder.setMenuItem(28, new MenuItem(createItem(Material.HOPPER,
                "&#FFA726✦ แทนที่อากาศด้วยหิน (//replace air stone)",
                "&7เติมบล็อกหินลงในช่องว่างของพื้นที่ที่เลือก",
                "&#FFD166➜ คลิกเพื่อรันคำสั่ง"), "//replace air stone"));

        holder.setMenuItem(29, new MenuItem(createItem(Material.WATER_BUCKET,
                "&#64B5F6✦ แทนที่น้ำด้วยหิน (//replace water stone)",
                "&7เปลี่ยนบล็อกน้ำทั้งหมดในพื้นที่ให้กลายเป็นหิน",
                "&#FFD166➜ คลิกเพื่อรันคำสั่ง"), "//replace water stone"));

        holder.setMenuItem(30, new MenuItem(createItem(Material.COBBLESTONE_WALL,
                "&#C084FC✦ สร้างกำแพงหิน 4 ด้าน (//walls stone)",
                "&7สร้างกำแพงล้อมรอบ 4 ด้านของพื้นที่ (ด้านในกลวง)",
                "&#FFD166➜ คลิกเพื่อรันคำสั่ง"), "//walls stone"));

        holder.setMenuItem(31, new MenuItem(createItem(Material.GLASS_PANE,
                "&#80DEEA✦ สร้างกำแพงกระจก 4 ด้าน (//walls glass)",
                "&7สร้างกำแพงกระจกล้อมรอบ 4 ด้านของพื้นที่",
                "&#FFD166➜ คลิกเพื่อรันคำสั่ง"), "//walls glass"));

        holder.setMenuItem(32, new MenuItem(createItem(Material.CHEST,
                "&#FF80AB✦ สร้างกรอบครอบ 6 ด้าน (//faces stone)",
                "&7สร้างพื้น ผนัง และเพดาน ครอบเป็นกล่อง 6 ด้าน",
                "&#FFD166➜ คลิกเพื่อรันคำสั่ง"), "//faces stone"));

        holder.setMenuItem(33, new MenuItem(createItem(Material.SCAFFOLDING,
                "&#EF476F✦ เจาะกลวงด้านใน (//hollow)",
                "&7ลบเนื้อบล็อกภายในให้กลวง เหลือเฉพาะเปลือกนอก",
                "&#FFD166➜ คลิกเพื่อรันคำสั่ง"), "//hollow"));

        holder.setMenuItem(34, new MenuItem(createItem(Material.MOSS_BLOCK,
                "&#06D6A0✦ ปูหญ้าเคลือบผิว (//overlay grass_block)",
                "&7วางบล็อกหญ้าคลุมบนผิวด้านบนสุดของทุกบล็อก",
                "&#FFD166➜ คลิกเพื่อรันคำสั่ง"), "//overlay grass_block"));

        holder.setMenuItem(37, new MenuItem(createItem(Material.PODZOL,
                "&#8D6E63✦ ปรับชั้นบล็อกเป็นธรรมชาติ (//naturalize)",
                "&7ปรับชั้นด้านบนเป็นหญ้า ชั้นกลางเป็นดิน และชั้นล่างเป็นหิน",
                "&#FFD166➜ คลิกเพื่อรันคำสั่ง"), "//naturalize"));

        holder.setMenuItem(38, new MenuItem(createItem(Material.SAND,
                "&#FFD166✦ เกลี่ยภูมิประเทศเนียนตา (//smooth 1)",
                "&7เกลี่ยความสูงของพื้นดินให้นุ่มนวลและลดรอยเหลี่ยม",
                "&#FFD166➜ คลิกเพื่อรันคำสั่ง"), "//smooth 1"));

        holder.setMenuItem(39, new MenuItem(createItem(Material.DISPENSER,
                "&#64B5F6✦ ย้ายบล็อกขึ้นบน 1 (//move 1 up)",
                "&7ย้ายสิ่งก่อสร้างในพื้นที่ขึ้นด้านบน 1 บล็อก",
                "&#FFD166➜ คลิกเพื่อรันคำสั่ง"), "//move 1 up"));

        holder.setMenuItem(40, new MenuItem(createItem(Material.DROPPER,
                "&#64B5F6✦ ย้ายบล็อกลงล่าง 1 (//move 1 down)",
                "&7ย้ายสิ่งก่อสร้างในพื้นที่ลงด้านล่าง 1 บล็อก",
                "&#FFD166➜ คลิกเพื่อรันคำสั่ง"), "//move 1 down"));

        holder.setMenuItem(41, new MenuItem(createItem(Material.REPEATER,
                "&#FF80AB✦ ต่อบล็อกซ้ำขึ้นบน 1 (//stack 1 up)",
                "&7คัดลอกต่อเติมสิ่งก่อสร้างขึ้นบนอีก 1 ชั้น",
                "&#FFD166➜ คลิกเพื่อรันคำสั่ง"), "//stack 1 up"));

        holder.setMenuItem(42, new MenuItem(createItem(Material.COMPARATOR,
                "&#FF80AB✦ ต่อบล็อกซ้ำไปข้างหน้า (//stack 1)",
                "&7คัดลอกต่อเติมสิ่งก่อสร้างไปข้างหน้าตามทิศที่มอง",
                "&#FFD166➜ คลิกเพื่อรันคำสั่ง"), "//stack 1"));

        holder.setMenuItem(43, new MenuItem(createItem(Material.WRITABLE_BOOK,
                "&#FFD166✦ กำหนดบล็อก //set เอง",
                "&7คลิกเพื่อนำคำสั่ง //set วางในแชทแล้วพิมพ์ชื่อบล็อกเอง",
                "&#FFD166➜ คลิกเพื่อพิมพ์คำสั่ง"), "//set ", true));

        addBackButton(inv, holder, MenuType.MAIN);
        player.openInventory(inv);
        ColorUtil.playSound(player, "BLOCK_CHEST_OPEN", 0.8f, 1.2f);
    }

    // ==========================================
    // 4. CLIPBOARD & TRANSFORM SUB-MENU
    // ==========================================
    private static void openClipboardMenu(Player player) {
        EditMeryMenuHolder holder = new EditMeryMenuHolder(MenuType.CLIPBOARD);
        Inventory inv = Bukkit.createInventory(holder, 54, ColorUtil.colorize("&#B388FF✦ เมนู: คลิปบอร์ด & คัดลอก/วาง"));
        holder.setInventory(inv);

        fillBorders(inv, holder, Material.CYAN_STAINED_GLASS_PANE);

        holder.setMenuItem(11, new MenuItem(createItem(Material.PAPER,
                "&#06D6A0✦ คัดลอกสิ่งก่อสร้าง (//copy)",
                "&7คัดลอกบล็อกในพื้นที่ที่เลือกลงในคลิปบอร์ด โดยจำตำแหน่งเทียบกับตัวคุณ",
                "&#FFD166➜ คลิกเพื่อรันคำสั่ง"), "//copy"));

        holder.setMenuItem(12, new MenuItem(createItem(Material.SHEARS,
                "&#EF476F✦ ตัดสิ่งก่อสร้าง (//cut)",
                "&7คัดลอกสิ่งก่อสร้างลงคลิปบอร์ด พร้อมลบพื้นที่ทิ้งเป็นอากาศทันที",
                "&#FFD166➜ คลิกเพื่อรันคำสั่ง"), "//cut"));

        holder.setMenuItem(13, new MenuItem(createItem(Material.MAP,
                "&#B388FF✦ วางสิ่งก่อสร้าง (//paste)",
                "&7วางบล็อกที่คัดลอกไว้ในคลิปบอร์ดตรงตำแหน่งที่คุณยืน",
                "&#FFD166➜ คลิกเพื่อรันคำสั่ง"), "//paste"));

        holder.setMenuItem(14, new MenuItem(createItem(Material.FEATHER,
                "&#64B5F6✦ วางโดยไม่ทับอากาศ (//paste -a)",
                "&7วางเฉพาะบล็อกที่มีเนื้อ โดยไม่นำบล็อกอากาศไปทับสิ่งก่อสร้างเดิม",
                "&#FFD166➜ คลิกเพื่อรันคำสั่ง"), "//paste -a"));

        holder.setMenuItem(15, new MenuItem(createItem(Material.ENDER_PEARL,
                "&#C084FC✦ วางที่พิกัดเดิม (//paste -o)",
                "&7วางสิ่งก่อสร้างกลับไปยังพิกัดเดิมในโลกตอนที่กดคัดลอก",
                "&#FFD166➜ คลิกเพื่อรันคำสั่ง"), "//paste -o"));

        // Rotate & Flip
        holder.setMenuItem(20, new MenuItem(createItem(Material.CLOCK,
                "&#FFD166✦ หมุน 90 องศา (//rotate 90)",
                "&7หมุนสิ่งก่อสร้างในคลิปบอร์ดตามเข็มนาฬิกา 90 องศา",
                "&#FFD166➜ คลิกเพื่อรันคำสั่ง"), "//rotate 90"));

        holder.setMenuItem(21, new MenuItem(createItem(Material.COMPASS,
                "&#FFD166✦ หมุน 180 องศา (//rotate 180)",
                "&7หมุนสิ่งก่อสร้างในคลิปบอร์ดกลับทิศ 180 องศา",
                "&#FFD166➜ คลิกเพื่อรันคำสั่ง"), "//rotate 180"));

        holder.setMenuItem(22, new MenuItem(createItem(Material.RECOVERY_COMPASS,
                "&#FFD166✦ หมุน 270 องศา (//rotate 270)",
                "&7หมุนสิ่งก่อสร้างในคลิปบอร์ดตามเข็มนาฬิกา 270 องศา",
                "&#FFD166➜ คลิกเพื่อรันคำสั่ง"), "//rotate 270"));

        holder.setMenuItem(23, new MenuItem(createItem(Material.LEVER,
                "&#FF80AB✦ พลิกด้านซ้าย-ขวา (//flip)",
                "&7สะท้อนสิ่งก่อสร้างสลับด้านซ้าย-ขวา/หน้า-หลังตามทิศที่มอง",
                "&#FFD166➜ คลิกเพื่อรันคำสั่ง"), "//flip"));

        holder.setMenuItem(24, new MenuItem(createItem(Material.TRIPWIRE_HOOK,
                "&#FF80AB✦ พลิกกลับหัวบน-ล่าง (//flip up)",
                "&7สะท้อนสิ่งก่อสร้างกลับหัวบน-ล่าง",
                "&#FFD166➜ คลิกเพื่อรันคำสั่ง"), "//flip up"));

        holder.setMenuItem(29, new MenuItem(createItem(Material.CAULDRON,
                "&#EF476F✦ ล้างคลิปบอร์ด (//clearclipboard)",
                "&7ลบสิ่งก่อสร้างในคลิปบอร์ดออกจากหน่วยความจำ",
                "&#FFD166➜ คลิกเพื่อรันคำสั่ง"), "//clearclipboard"));

        holder.setMenuItem(31, new MenuItem(createItem(Material.BOOKSHELF,
                "&#64B5F6✦ ดูรายการ Schematic (//schem list)",
                "&7แสดงรายชื่อไฟล์ Schematic ทั้งหมดที่มีในเซิร์ฟเวอร์",
                "&#FFD166➜ คลิกเพื่อรันคำสั่ง"), "//schem list"));

        holder.setMenuItem(32, new MenuItem(createItem(Material.CHEST_MINECART,
                "&#06D6A0✦ บันทึกไฟล์ Schematic (//schem save)",
                "&7คลิกเพื่อแนะนำคำสั่งบันทึกลงในแชท //schem save <ชื่อไฟล์>",
                "&#FFD166➜ คลิกเพื่อพิมพ์คำสั่ง"), "//schem save ", true));

        holder.setMenuItem(33, new MenuItem(createItem(Material.FURNACE_MINECART,
                "&#FFA726✦ โหลดไฟล์ Schematic (//schem load)",
                "&7คลิกเพื่อแนะนำคำสั่งโหลดลงในแชท //schem load <ชื่อไฟล์>",
                "&#FFD166➜ คลิกเพื่อพิมพ์คำสั่ง"), "//schem load ", true));

        addBackButton(inv, holder, MenuType.MAIN);
        player.openInventory(inv);
        ColorUtil.playSound(player, "BLOCK_CHEST_OPEN", 0.8f, 1.2f);
    }

    // ==========================================
    // 5. SHAPES & GENERATION SUB-MENU
    // ==========================================
    private static void openShapesMenu(Player player) {
        EditMeryMenuHolder holder = new EditMeryMenuHolder(MenuType.SHAPES);
        Inventory inv = Bukkit.createInventory(holder, 54, ColorUtil.colorize("&#B388FF✦ เมนู: สร้างรูปทรงเรขาคณิต"));
        holder.setInventory(inv);

        fillBorders(inv, holder, Material.PURPLE_STAINED_GLASS_PANE);

        holder.setMenuItem(10, new MenuItem(createItem(Material.SEA_LANTERN,
                "&#80DEEA✦ ทรงกระบอกตัน รัศมี 5 (//cyl stone 5 1)",
                "&7สร้างแผ่นทรงกระบอกตันหิน รัศมี 5 บล็อก ความสูง 1 บล็อก",
                "&#FFD166➜ คลิกเพื่อรันคำสั่ง"), "//cyl stone 5 1"));

        holder.setMenuItem(11, new MenuItem(createItem(Material.GLOWSTONE,
                "&#FFD166✦ ทรงกระบอกตัน รัศมี 10 (//cyl stone 10 1)",
                "&7สร้างแผ่นทรงกระบอกตันหิน รัศมี 10 บล็อก ความสูง 1 บล็อก",
                "&#FFD166➜ คลิกเพื่อรันคำสั่ง"), "//cyl stone 10 1"));

        holder.setMenuItem(12, new MenuItem(createItem(Material.GLASS_BOTTLE,
                "&#B388FF✦ ทรงกระบอกกลวง รัศมี 5 สูง 5 (//hcyl stone 5 5)",
                "&7สร้างท่อทรงกระบอกกลวงหิน รัศมี 5 ความสูง 5 บล็อก",
                "&#FFD166➜ คลิกเพื่อรันคำสั่ง"), "//hcyl stone 5 5"));

        holder.setMenuItem(13, new MenuItem(createItem(Material.GLASS,
                "&#64B5F6✦ ท่อทรงกระบอกกระจก (//hcyl glass 10 10)",
                "&7สร้างหอคอยท่อกระจกกลวง รัศมี 10 ความสูง 10 บล็อก",
                "&#FFD166➜ คลิกเพื่อรันคำสั่ง"), "//hcyl glass 10 10"));

        holder.setMenuItem(14, new MenuItem(createItem(Material.SLIME_BALL,
                "&#06D6A0✦ ทรงกลมตัน รัศมี 5 (//sphere stone 5)",
                "&7สร้างลูกบอลทรงกลมตันหิน รัศมี 5 บล็อก",
                "&#FFD166➜ คลิกเพื่อรันคำสั่ง"), "//sphere stone 5"));

        holder.setMenuItem(15, new MenuItem(createItem(Material.MAGMA_CREAM,
                "&#FFA726✦ ทรงกลมตัน รัศมี 10 (//sphere stone 10)",
                "&7สร้างลูกบอลทรงกลมตันหิน รัศมี 10 บล็อก",
                "&#FFD166➜ คลิกเพื่อรันคำสั่ง"), "//sphere stone 10"));

        holder.setMenuItem(16, new MenuItem(createItem(Material.HEART_OF_THE_SEA,
                "&#4DD0E1✦ ทรงกลมกลวง รัศมี 5 (//hsphere stone 5)",
                "&7สร้างลูกบอลกลวงหิน รัศมี 5 บล็อก (ด้านในกลวง)",
                "&#FFD166➜ คลิกเพื่อรันคำสั่ง"), "//hsphere stone 5"));

        holder.setMenuItem(19, new MenuItem(createItem(Material.TINTED_GLASS,
                "&#C084FC✦ ทรงกลมกลวงกระจก (//hsphere glass 10)",
                "&7สร้างโดมลูกบอลกลวงกระจก รัศมี 10 บล็อก",
                "&#FFD166➜ คลิกเพื่อรันคำสั่ง"), "//hsphere glass 10"));

        holder.setMenuItem(20, new MenuItem(createItem(Material.SANDSTONE_STAIRS,
                "&#FFD166✦ ปิรามิดตัน สูง 5 (//pyramid stone 5)",
                "&7สร้างปิรามิดตันหิน ความสูง 5 ชั้น",
                "&#FFD166➜ คลิกเพื่อรันคำสั่ง"), "//pyramid stone 5"));

        holder.setMenuItem(21, new MenuItem(createItem(Material.RED_SANDSTONE_STAIRS,
                "&#FF80AB✦ ปิรามิดตัน สูง 10 (//pyramid stone 10)",
                "&7สร้างปิรามิดตันหิน ความสูง 10 ชั้น",
                "&#FFD166➜ คลิกเพื่อรันคำสั่ง"), "//pyramid stone 10"));

        holder.setMenuItem(22, new MenuItem(createItem(Material.COBBLESTONE_STAIRS,
                "&#B388FF✦ ปิรามิดกลวง สูง 5 (//hpyramid stone 5)",
                "&7สร้างปิรามิดกลวงหิน ความสูง 5 ชั้น",
                "&#FFD166➜ คลิกเพื่อรันคำสั่ง"), "//hpyramid stone 5"));

        holder.setMenuItem(23, new MenuItem(createItem(Material.SMOOTH_SANDSTONE,
                "&#FFE082✦ ปิรามิดทรายกลวง (//hpyramid sandstone 10)",
                "&7สร้างปิรามิดทรายกลวงขนาดยักษ์ สูง 10 ชั้น",
                "&#FFD166➜ คลิกเพื่อรันคำสั่ง"), "//hpyramid sandstone 10"));

        holder.setMenuItem(24, new MenuItem(createItem(Material.OAK_SAPLING,
                "&#81C784✦ เสกป่าโอ๊ค (//forest 10 oak)",
                "&7สร้างป่าต้นไม้โอ๊คธรรมชาติ รัศมี 10 บล็อก",
                "&#FFD166➜ คลิกเพื่อรันคำสั่ง"), "//forest 10 oak"));

        holder.setMenuItem(25, new MenuItem(createItem(Material.BIRCH_SAPLING,
                "&#A5D6A7✦ เสกป่าเบิร์ช (//forest 15 birch)",
                "&7สร้างป่าต้นไม้เบิร์ชธรรมชาติ รัศมี 15 บล็อก",
                "&#FFD166➜ คลิกเพื่อรันคำสั่ง"), "//forest 15 birch"));

        holder.setMenuItem(28, new MenuItem(createItem(Material.SPRUCE_SAPLING,
                "&#66BB6A✦ เสกป่าสน (//forest 15 spruce)",
                "&7สร้างป่าต้นไม้สนไทกาธรรมชาติ รัศมี 15 บล็อก",
                "&#FFD166➜ คลิกเพื่อรันคำสั่ง"), "//forest 15 spruce"));

        holder.setMenuItem(29, new MenuItem(createItem(Material.CARVED_PUMPKIN,
                "&#FFA726✦ เสกไร่ฟักทอง (//pumpkins 10)",
                "&7สร้างแปลงฟักทองธรรมชาติ รัศมี 10 บล็อก",
                "&#FFD166➜ คลิกเพื่อรันคำสั่ง"), "//pumpkins 10"));

        holder.setMenuItem(32, new MenuItem(createItem(Material.WRITABLE_BOOK,
                "&#FFD166✦ กำหนดคำสั่ง //cyl เอง",
                "&7คลิกเพื่อแนะนำคำสั่ง //cyl <บล็อก> <รัศมี> [ความสูง]",
                "&#FFD166➜ คลิกเพื่อพิมพ์คำสั่ง"), "//cyl ", true));

        holder.setMenuItem(33, new MenuItem(createItem(Material.BOOK,
                "&#FFD166✦ กำหนดคำสั่ง //sphere เอง",
                "&7คลิกเพื่อแนะนำคำสั่ง //sphere <บล็อก> <รัศมี>",
                "&#FFD166➜ คลิกเพื่อพิมพ์คำสั่ง"), "//sphere ", true));

        holder.setMenuItem(34, new MenuItem(createItem(Material.MAP,
                "&#FFD166✦ กำหนดคำสั่ง //pyramid เอง",
                "&7คลิกเพื่อแนะนำคำสั่ง //pyramid <บล็อก> <ขนาด>",
                "&#FFD166➜ คลิกเพื่อพิมพ์คำสั่ง"), "//pyramid ", true));

        addBackButton(inv, holder, MenuType.MAIN);
        player.openInventory(inv);
        ColorUtil.playSound(player, "BLOCK_CHEST_OPEN", 0.8f, 1.2f);
    }

    // ==========================================
    // 6. UTILITIES & TOOLS SUB-MENU
    // ==========================================
    private static void openUtilitiesMenu(Player player) {
        EditMeryMenuHolder holder = new EditMeryMenuHolder(MenuType.UTILITIES);
        Inventory inv = Bukkit.createInventory(holder, 54, ColorUtil.colorize("&#B388FF✦ เมนู: เครื่องมือ & อำนวยความสะดวก"));
        holder.setInventory(inv);

        fillBorders(inv, holder, Material.BLACK_STAINED_GLASS_PANE);

        holder.setMenuItem(10, new MenuItem(createItem(Material.BUCKET,
                "&#64B5F6✦ สูบน้ำ/ลาวาออก รัศมี 10 (//drain 10)",
                "&7ดูดของเหลวน้ำและลาวาในรัศมี 10 บล็อกจากตัวคุณ",
                "&#FFD166➜ คลิกเพื่อรันคำสั่ง"), "//drain 10"));

        holder.setMenuItem(11, new MenuItem(createItem(Material.LAVA_BUCKET,
                "&#EF5350✦ สูบน้ำ/ลาวาออก รัศมี 25 (//drain 25)",
                "&7ดูดของเหลวน้ำและลาวาในรัศมี 25 บล็อกเป็นวงกว้าง",
                "&#FFD166➜ คลิกเพื่อรันคำสั่ง"), "//drain 25"));

        holder.setMenuItem(12, new MenuItem(createItem(Material.WATER_BUCKET,
                "&#81D4FA✦ ซ่อมน้ำไม่ไหล รัศมี 10 (//fixwater 10)",
                "&7เปลี่ยนน้ำที่ไหลเป็นน้ำนิ่งเต็มพื้นที่ รัศมี 10 บล็อก",
                "&#FFD166➜ คลิกเพื่อรันคำสั่ง"), "//fixwater 10"));

        holder.setMenuItem(13, new MenuItem(createItem(Material.ICE,
                "&#81D4FA✦ ซ่อมน้ำไม่ไหล รัศมี 25 (//fixwater 25)",
                "&7เปลี่ยนน้ำที่ไหลเป็นน้ำนิ่งเต็มพื้นที่ รัศมี 25 บล็อก",
                "&#FFD166➜ คลิกเพื่อรันคำสั่ง"), "//fixwater 25"));

        holder.setMenuItem(14, new MenuItem(createItem(Material.MAGMA_BLOCK,
                "&#FF7043✦ ซ่อมลาวาไม่ไหล รัศมี 10 (//fixlava 10)",
                "&7เปลี่ยนลาวาที่ไหลเป็นลาวานิ่งเต็มพื้นที่ รัศมี 10 บล็อก",
                "&#FFD166➜ คลิกเพื่อรันคำสั่ง"), "//fixlava 10"));

        holder.setMenuItem(15, new MenuItem(createItem(Material.FIRE_CHARGE,
                "&#FFA726✦ ดับไฟทั้งหมดรอบตัว (//extinguish 15)",
                "&7ดับไฟที่ลุกไหม้ในรัศมี 15 บล็อกจากตัวคุณทันที",
                "&#FFD166➜ คลิกเพื่อรันคำสั่ง"), "//extinguish 15"));

        holder.setMenuItem(16, new MenuItem(createItem(Material.SNOWBALL,
                "&#ECEFF1✦ เสกหิมะปกคลุม (//snow 10)",
                "&7เสกแผ่นหิมะปกคลุมผิวดินในรัศมี 10 บล็อก",
                "&#FFD166➜ คลิกเพื่อรันคำสั่ง"), "//snow 10"));

        holder.setMenuItem(19, new MenuItem(createItem(Material.CHORUS_FRUIT,
                "&#CE93D8✦ ทะลุกำแพงไปข้างหน้า (//thru)",
                "&7วาร์ปทะลุกำแพงหรือสิ่งกีดขวางไปอีกฝั่งทันที",
                "&#FFD166➜ คลิกเพื่อรันคำสั่ง"), "//thru"));

        holder.setMenuItem(20, new MenuItem(createItem(Material.FIREWORK_ROCKET,
                "&#06D6A0✦ วาร์ปไปยังจุดที่มอง (//jumpto)",
                "&7วาร์ปตัวคุณไปยังบล็อกที่เป้าสายตาเล็งอยู่ทันที",
                "&#FFD166➜ คลิกเพื่อรันคำสั่ง"), "//jumpto"));

        holder.setMenuItem(21, new MenuItem(createItem(Material.GLASS_PANE,
                "&#64B5F6✦ ขึ้นที่สูง 1 บล็อก (//up 1)",
                "&7วาร์ปขึ้นด้านบน 1 บล็อก พร้อมเสกบล็อกกระจกใต้เท้า",
                "&#FFD166➜ คลิกเพื่อรันคำสั่ง"), "//up 1"));

        holder.setMenuItem(22, new MenuItem(createItem(Material.SCAFFOLDING,
                "&#64B5F6✦ ขึ้นที่สูง 5 บล็อก (//up 5)",
                "&7วาร์ปขึ้นด้านบน 5 บล็อก พร้อมเสกบล็อกกระจกใต้เท้า",
                "&#FFD166➜ คลิกเพื่อรันคำสั่ง"), "//up 5"));

        holder.setMenuItem(23, new MenuItem(createItem(Material.ELYTRA,
                "&#64B5F6✦ ขึ้นที่สูง 20 บล็อก (//up 20)",
                "&7วาร์ปขึ้นด้านบน 20 บล็อก เพื่อมองดูวิวจากมุมสูง",
                "&#FFD166➜ คลิกเพื่อรันคำสั่ง"), "//up 20"));

        holder.setMenuItem(24, new MenuItem(createItem(Material.LADDER,
                "&#FFA726✦ วาร์ปขึ้นเพดาน/ชั้นบน (//ascend)",
                "&7วาร์ปทะลุเพดานขึ้นไปยืนบนชั้นถัดไป",
                "&#FFD166➜ คลิกเพื่อรันคำสั่ง"), "//ascend"));

        holder.setMenuItem(25, new MenuItem(createItem(Material.IRON_BARS,
                "&#FFA726✦ วาร์ปทะลุพื้นลงชั้นล่าง (//descend)",
                "&7วาร์ปทะลุพื้นลงไปยังชั้นข้างล่าง",
                "&#FFD166➜ คลิกเพื่อรันคำสั่ง"), "//descend"));

        holder.setMenuItem(28, new MenuItem(createItem(Material.PAINTING,
                "&#B388FF✦ แปรงพ่นทรงกลมหิน (/brush sphere stone 3)",
                "&7ติดตั้งแปรงพ่นบล็อกหินทรงกลม รัศมี 3 ลงในไอเทมที่ถือ",
                "&#FFD166➜ คลิกเพื่อรันคำสั่ง"), "/brush sphere stone 3"));

        holder.setMenuItem(29, new MenuItem(createItem(Material.BRUSH,
                "&#06D6A0✦ แปรงเกลี่ยผิวภูมิประเทศ (/brush smooth 3)",
                "&7ติดตั้งแปรงเกลี่ยดินเนียนนุ่ม รัศมี 3 ในไอเทมที่ถือ",
                "&#FFD166➜ คลิกเพื่อรันคำสั่ง"), "/brush smooth 3"));

        holder.setMenuItem(30, new MenuItem(createItem(Material.CAMPFIRE,
                "&#FFD166✦ แปรงทรงกระบอก (/brush cylinder stone 3 1)",
                "&7ติดตั้งแปรงพ่นทรงกระบอก รัศมี 3 สูง 1 ลงในไอเทมที่ถือ",
                "&#FFD166➜ คลิกเพื่อรันคำสั่ง"), "/brush cylinder stone 3 1"));

        holder.setMenuItem(31, new MenuItem(createItem(Material.MILK_BUCKET,
                "&#EF476F✦ ล้างแปรงออกจากมือ (/none)",
                "&7ยกเลิกแปรงและเครื่องมือพิเศษทั้งหมดจากไอเทมที่ถืออยู่",
                "&#FFD166➜ คลิกเพื่อรันคำสั่ง"), "/none"));

        addBackButton(inv, holder, MenuType.MAIN);
        player.openInventory(inv);
        ColorUtil.playSound(player, "BLOCK_CHEST_OPEN", 0.8f, 1.2f);
    }

    // ==========================================
    // 7. HISTORY & SETTINGS SUB-MENU
    // ==========================================
    private static void openHistoryMenu(Player player) {
        EditMeryMenuHolder holder = new EditMeryMenuHolder(MenuType.HISTORY);
        Inventory inv = Bukkit.createInventory(holder, 54, ColorUtil.colorize("&#B388FF✦ เมนู: ประวัติ & ตั้งค่าโหมด"));
        holder.setInventory(inv);

        fillBorders(inv, holder, Material.PURPLE_STAINED_GLASS_PANE);

        holder.setMenuItem(11, new MenuItem(createItem(Material.ARROW,
                "&#FFD166✦ ย้อนกลับ 1 คำสั่ง (//undo)",
                "&7ย้อนกลับการวางบล็อกหรือการกระทำล่าสุด 1 ครั้ง",
                "&#FFD166➜ คลิกเพื่อรันคำสั่ง"), "//undo"));

        holder.setMenuItem(12, new MenuItem(createItem(Material.FEATHER,
                "&#FFD166✦ ย้อนกลับ 3 คำสั่ง (//undo 3)",
                "&7ย้อนกลับการกระทำล่าสุดติดต่อกัน 3 ครั้ง",
                "&#FFD166➜ คลิกเพื่อรันคำสั่ง"), "//undo 3"));

        holder.setMenuItem(13, new MenuItem(createItem(Material.BOOK,
                "&#FFD166✦ ย้อนกลับ 5 คำสั่ง (//undo 5)",
                "&7ย้อนกลับการกระทำล่าสุดติดต่อกัน 5 ครั้ง",
                "&#FFD166➜ คลิกเพื่อรันคำสั่ง"), "//undo 5"));

        holder.setMenuItem(14, new MenuItem(createItem(Material.SPECTRAL_ARROW,
                "&#06D6A0✦ ทำซ้ำ 1 คำสั่ง (//redo)",
                "&7ทำซ้ำคำสั่งที่เพิ่งย้อนกลับไปล่าสุด 1 ครั้ง",
                "&#FFD166➜ คลิกเพื่อรันคำสั่ง"), "//redo"));

        holder.setMenuItem(15, new MenuItem(createItem(Material.AMETHYST_SHARD,
                "&#06D6A0✦ ทำซ้ำ 3 คำสั่ง (//redo 3)",
                "&7ทำซ้ำคำสั่งที่เพิ่งย้อนกลับไปติดต่อกัน 3 ครั้ง",
                "&#FFD166➜ คลิกเพื่อรันคำสั่ง"), "//redo 3"));

        holder.setMenuItem(21, new MenuItem(createItem(Material.LAVA_BUCKET,
                "&#EF476F✦ ล้างประวัติทั้งหมด (//clearhistory)",
                "&7ลบประวัติการ Undo/Redo ในเซสชันของคุณเพื่อประหยัด RAM",
                "&#FFD166➜ คลิกเพื่อรันคำสั่ง"), "//clearhistory"));

        // Mode Management
        holder.setMenuItem(23, new MenuItem(createItem(Material.WOODEN_AXE,
                "&#B388FF✦ สลับสถานะโหมด (/em toggle)",
                "&7สลับสถานะเปิดหรือปิดโหมดก่อสร้าง EditMery ทันที",
                "&#FFD166➜ คลิกเพื่อสลับสถานะ"), p -> {
            boolean newState = EditMeryPlugin.getEditMeryInstance().getManager().toggle(p);
            EditMeryPlugin.getEditMeryInstance().getManager().sendToggleSuccess(p, newState);
            openHistoryMenu(p);
        }));

        holder.setMenuItem(29, new MenuItem(createItem(Material.EMERALD_BLOCK,
                "&#06D6A0✦ เปิดโหมดขวาน WorldEdit (/em enable)",
                "&7เปิดใช้งานขวานไม้เป็นคฑา Wand และปลดล็อคคำสั่ง WorldEdit",
                "&#FFD166➜ คลิกเพื่อเปิดโหมด"), p -> {
            EditMeryPlugin.getEditMeryInstance().getManager().setEnabled(p, true);
            EditMeryPlugin.getEditMeryInstance().getManager().sendToggleSuccess(p, true);
            openHistoryMenu(p);
        }));

        holder.setMenuItem(31, new MenuItem(createItem(Material.REDSTONE_BLOCK,
                "&#EF476F✦ ปิดโหมดขวาน WorldEdit (/em disable)",
                "&7ปิดโหมดขวานกลับเป็นขวานตัดไม้ธรรมดา ทุบบล็อกได้ปกติ",
                "&#FFD166➜ คลิกเพื่อปิดโหมด"), p -> {
            EditMeryPlugin.getEditMeryInstance().getManager().setEnabled(p, false);
            EditMeryPlugin.getEditMeryInstance().getManager().sendToggleSuccess(p, false);
            openHistoryMenu(p);
        }));

        holder.setMenuItem(33, new MenuItem(createItem(Material.NETHER_STAR,
                "&#64B5F6✦ รีโหลดการตั้งค่าทั้งหมด (/em reload)",
                "&7รีโหลดไฟล์ config.yml และระบบธีมข้อความของปลั๊กอิน",
                "&#FFD166➜ คลิกเพื่อรันคำสั่ง"), "/editmery reload"));

        addBackButton(inv, holder, MenuType.MAIN);
        player.openInventory(inv);
        ColorUtil.playSound(player, "BLOCK_CHEST_OPEN", 0.8f, 1.2f);
    }

    // ==========================================
    // UTILITY BUILDERS
    // ==========================================
    public static ItemStack createItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.colorize(name));
            if (lore != null && lore.length > 0) {
                List<String> list = new ArrayList<>();
                for (String l : lore) {
                    list.add(ColorUtil.colorize(l));
                }
                meta.setLore(list);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private static void fillBorders(Inventory inv, EditMeryMenuHolder holder, Material mat) {
        ItemStack glass = createItem(mat, " ");
        int size = inv.getSize();
        for (int i = 0; i < size; i++) {
            if (i < 9 || i >= size - 9 || i % 9 == 0 || i % 9 == 8) {
                inv.setItem(i, glass);
                holder.setMenuItem(i, new MenuItem(glass, (String) null));
            }
        }
    }

    private static void addBackButton(Inventory inv, EditMeryMenuHolder holder, MenuType backTo) {
        ItemStack back = createItem(Material.ARROW,
                "&#FF80AB« &#F8FAFCย้อนกลับไปหน้าหลัก",
                "&7คลิกเพื่อกลับสู่เมนูหลัก EditMery");
        inv.setItem(49, back);
        holder.setMenuItem(49, new MenuItem(back, backTo));
    }
}
