package com.holomery.gui;

import com.holomery.manager.HologramManager;
import com.holomery.model.BillboardMode;
import com.holomery.model.HoloLine;
import com.holomery.model.Hologram;
import com.holomery.util.MessageUtil;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public class HoloGuiListener implements Listener {

    private final HologramManager manager = HologramManager.getInstance();

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Inventory inv = event.getInventory();
        InventoryHolder holder = inv.getHolder();
        if (holder == null) return;

        if (holder instanceof HoloHolders.ListHolder listHolder) {
            event.setCancelled(true);
            handleListClick(event, player, listHolder);
        } else if (holder instanceof HoloHolders.EditHolder editHolder) {
            event.setCancelled(true);
            handleEditorClick(event, player, editHolder);
        } else if (holder instanceof HoloHolders.ThemeHolder themeHolder) {
            event.setCancelled(true);
            handleThemeClick(event, player, themeHolder);
        } else if (holder instanceof HoloHolders.AddLineHolder addLineHolder) {
            event.setCancelled(true);
            handleAddLineClick(event, player, addLineHolder);
        } else if (holder instanceof HoloHolders.ActionHolder actionHolder) {
            event.setCancelled(true);
            handleActionClick(event, player, actionHolder);
        }
    }

    private void handleListClick(InventoryClickEvent event, Player player, HoloHolders.ListHolder holder) {
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= 54) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        String action = GuiItemBuilder.getData(clicked, "action");
        String holoId = GuiItemBuilder.getData(clicked, "holo_id");

        if ("close".equals(action)) {
            player.closeInventory();
            return;
        }

        if ("prev_page".equals(action)) {
            HoloListGUI.open(player, holder.getPage() - 1);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.2f);
            return;
        }

        if ("next_page".equals(action)) {
            HoloListGUI.open(player, holder.getPage() + 1);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.2f);
            return;
        }

        if ("create_holo".equals(action)) {
            ChatInputManager.prompt(player, "Enter unique ID for new hologram:", "", 0, 0, true, newId -> {
                String clean = newId.trim().toLowerCase().replaceAll("[^a-z0-9_-]", "");
                if (clean.isEmpty()) {
                    MessageUtil.sendError(player, "Invalid ID! Please use alphanumeric characters.");
                    return;
                }
                if (manager.exists(clean)) {
                    MessageUtil.sendError(player, "A hologram with ID '&e" + clean + "&c' already exists!");
                    return;
                }

                // Prompt second step: initial line content
                ChatInputManager.prompt(player, "Enter initial text for hologram '" + clean + "':", clean, 0, 0, true, initialText -> {
                    Hologram holo = manager.createHologram(clean, player.getLocation().add(0, 1.2, 0));
                    holo.addLine(initialText);
                    manager.saveHolograms();
                    MessageUtil.sendSuccess(player, "Created hologram '&b" + clean + "&a' with your text!");
                    HoloEditorGUI.open(player, holo);
                });
            });
            return;
        }

        // Clicked a hologram Beacon
        if (holoId != null) {
            Hologram holo = manager.getHologram(holoId);
            if (holo == null) return;

            if (event.getClick() == ClickType.RIGHT) {
                player.teleport(holo.getLocation());
                MessageUtil.sendSuccess(player, "Teleported to '&b" + holo.getId() + "&a'!");
                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.2f);
            } else if (event.getClick() == ClickType.SHIFT_RIGHT) {
                manager.deleteHologram(holo.getId());
                MessageUtil.sendSuccess(player, "Deleted hologram '&b" + holo.getId() + "&a'.");
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
                HoloListGUI.render(event.getInventory(), holder.getPage());
            } else {
                HoloEditorGUI.open(player, holo);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
            }
        }
    }

    private void handleEditorClick(InventoryClickEvent event, Player player, HoloHolders.EditHolder holder) {
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= 54) return;

        Hologram holo = holder.getHologram();
        if (holo == null) {
            player.closeInventory();
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        Inventory inv = event.getInventory();
        String control = GuiItemBuilder.getData(clicked, "control");
        String lineIdxStr = GuiItemBuilder.getData(clicked, "line_index");

        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.5f);

        if ("click_actions".equals(control)) {
            HoloActionGUI.open(player, holo);
            return;
        }

        // --- Page Navigation (In-place update) ---
        if ("page_prev".equals(control)) {
            holo.prevPage(player);
            HoloEditorGUI.render(inv, holo);
            return;
        }

        if ("page_next".equals(control)) {
            holo.nextPage(player);
            HoloEditorGUI.render(inv, holo);
            return;
        }

        if ("page_manage".equals(control)) {
            if (event.getClick() == ClickType.SHIFT_LEFT) {
                holo.createNewPage();
                holo.setPage(holo.getPageCount() - 1);
                holo.addLine("<cyberpunk>✦ Page " + holo.getPageCount() + " ✦</cyberpunk>");
                manager.saveHolograms();
                MessageUtil.sendSuccess(player, "Created & switched to new Page " + holo.getPageCount() + "!");
                HoloEditorGUI.render(inv, holo);
            } else if (event.getClick() == ClickType.SHIFT_RIGHT) {
                if (holo.getPageCount() > 1) {
                    holo.removePage(holo.getActivePageIndex());
                    manager.saveHolograms();
                    MessageUtil.sendSuccess(player, "Removed current page.");
                    HoloEditorGUI.render(inv, holo);
                } else {
                    MessageUtil.sendError(player, "Cannot delete the only remaining page!");
                }
            } else if (event.getClick() == ClickType.RIGHT) {
                int[] intervals = {0, 3, 5, 10, 15};
                int current = holo.getAutoPageIntervalSeconds();
                int next = 0;
                for (int i = 0; i < intervals.length; i++) {
                    if (intervals[i] == current) {
                        next = intervals[(i + 1) % intervals.length];
                        break;
                    }
                }
                holo.setAutoPageIntervalSeconds(next);
                manager.saveHolograms();
                HoloEditorGUI.render(inv, holo);
            } else {
                holo.setClickPageSwitch(!holo.isClickPageSwitch());
                manager.saveHolograms();
                HoloEditorGUI.render(inv, holo);
            }
            return;
        }

        if ("teleport".equals(control)) {
            player.teleport(holo.getLocation());
            MessageUtil.sendSuccess(player, "Teleported to '&b" + holo.getId() + "&a'!");
            return;
        }

        // --- Bottom Row Styling Controls (In-place update - Menu NEVER closes!) ---
        if ("billboard".equals(control)) {
            BillboardMode[] modes = BillboardMode.values();
            int nextIdx = (holo.getBillboard().ordinal() + 1) % modes.length;
            holo.setBillboard(modes[nextIdx]);
            manager.saveHolograms();
            HoloEditorGUI.render(inv, holo);
            return;
        }

        if ("scale".equals(control)) {
            float scale = holo.getScale();
            scale = (event.getClick() == ClickType.RIGHT) ? scale - 0.2f : scale + 0.2f;
            scale = Math.max(0.2f, Math.min(5.0f, Math.round(scale * 10f) / 10f));
            holo.setScale(scale);
            manager.saveHolograms();
            HoloEditorGUI.render(inv, holo);
            return;
        }

        if ("theme_palette".equals(control)) {
            HoloThemeGUI.open(player, holo);
            return;
        }

        if ("background".equals(control)) {
            holo.toggleBackground();
            manager.saveHolograms();
            HoloEditorGUI.render(inv, holo);
            boolean isBlack = !holo.isTransparentBackground();
            MessageUtil.sendSuccess(player, "พื้นหลังสีดำของ '&b" + holo.getId() + "&a': " + (isBlack ? "&eเปิด (ON - มีกล่องดำ)" : "&bปิด (OFF - โปร่งใส)"));
            return;
        }

        if ("seethrough".equals(control)) {
            holo.setSeeThrough(!holo.isSeeThrough());
            manager.saveHolograms();
            HoloEditorGUI.render(inv, holo);
            return;
        }

        if ("shadow".equals(control)) {
            holo.setShadow(!holo.isShadow());
            manager.saveHolograms();
            HoloEditorGUI.render(inv, holo);
            return;
        }

        if ("spacing".equals(control)) {
            double sp = holo.getLineSpacing();
            sp = (event.getClick() == ClickType.RIGHT) ? sp - 0.05 : sp + 0.05;
            sp = Math.max(0.05, Math.min(2.0, Math.round(sp * 100.0) / 100.0));
            holo.setLineSpacing(sp);
            manager.saveHolograms();
            HoloEditorGUI.render(inv, holo);
            return;
        }

        if ("back_to_list".equals(control)) {
            HoloListGUI.open(player, 0);
            return;
        }

        if ("delete_holo".equals(control)) {
            manager.deleteHologram(holo.getId());
            MessageUtil.sendSuccess(player, "Deleted hologram '&b" + holo.getId() + "&a'.");
            HoloListGUI.open(player, 0);
            return;
        }

        if ("add_line".equals(control)) {
            HoloAddLineGUI.open(player, holo);
            return;
        }

        if ("spin".equals(control)) {
            holo.setSpin(!holo.isSpin());
            manager.saveHolograms();
            HoloEditorGUI.render(inv, holo);
            return;
        }

        // --- Line Item Click ---
        if (lineIdxStr != null) {
            try {
                int lineIndex = Integer.parseInt(lineIdxStr);
                if (lineIndex >= 0 && lineIndex < holo.getLineCount()) {
                    if (event.getClick() == ClickType.SHIFT_LEFT) {
                        // Move line UP
                        if (lineIndex > 0) {
                            holo.swapLines(lineIndex, lineIndex - 1);
                            manager.saveHolograms();
                            HoloEditorGUI.render(inv, holo);
                            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.6f);
                        }
                    } else if (event.getClick() == ClickType.SHIFT_RIGHT) {
                        // Move line DOWN
                        if (lineIndex < holo.getLineCount() - 1) {
                            holo.swapLines(lineIndex, lineIndex + 1);
                            manager.saveHolograms();
                            HoloEditorGUI.render(inv, holo);
                            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.2f);
                        }
                    } else if (event.getClick() == ClickType.RIGHT) {
                        holo.removeLine(lineIndex);
                        manager.saveHolograms();
                        HoloEditorGUI.render(inv, holo); // In-place update! Menu does not close!
                    } else {
                        HoloLine line = holo.getLine(lineIndex);
                        ChatInputManager.prompt(player, "Editing Line #" + (lineIndex + 1) + " (Current: " + line.getRawText() + ")", holo.getId(), holo.getActivePageIndex(), lineIndex, false, newText -> {
                            holo.setLine(lineIndex, newText);
                            manager.saveHolograms();
                            MessageUtil.sendSuccess(player, "Line #" + (lineIndex + 1) + " updated!");
                        });
                    }
                }
            } catch (NumberFormatException ignored) {}
        }
    }

    private void handleThemeClick(InventoryClickEvent event, Player player, HoloHolders.ThemeHolder holder) {
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= 36) return;

        Hologram holo = holder.getHologram();
        if (holo == null) {
            player.closeInventory();
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        String action = GuiItemBuilder.getData(clicked, "action");
        String theme = GuiItemBuilder.getData(clicked, "theme_name");

        if ("back_to_editor".equals(action)) {
            HoloEditorGUI.open(player, holo);
            return;
        }

        if (theme != null) {
            for (int i = 0; i < holo.getLineCount(); i++) {
                HoloLine line = holo.getLine(i);
                String raw = line.getRawText().replaceAll("<[a-zA-Z0-9_]+>", "").replaceAll("</[a-zA-Z0-9_]+>", "");
                holo.setLine(i, "<" + theme + ">" + raw + "</" + theme + ">");
            }
            manager.saveHolograms();
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.6f);
            MessageUtil.sendSuccess(player, "Applied theme '&b" + theme + "&a' to current page!");
            HoloEditorGUI.open(player, holo);
        }
    }

    private void handleAddLineClick(InventoryClickEvent event, Player player, HoloHolders.AddLineHolder holder) {
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= 45) return;

        Hologram holo = holder.getHologram();
        if (holo == null) {
            player.closeInventory();
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        String action = GuiItemBuilder.getData(clicked, "action");
        String type = GuiItemBuilder.getData(clicked, "type");

        if ("back_to_editor".equals(action)) {
            HoloEditorGUI.open(player, holo);
            return;
        }

        if ("text".equals(type)) {
            ChatInputManager.prompt(player, "Enter text for new line:", holo.getId(), holo.getActivePageIndex(), holo.getLineCount(), true, newText -> {
                holo.addLine(newText);
                manager.saveHolograms();
                MessageUtil.sendSuccess(player, "Added new text line!");
            });
        } else if ("item".equals(type)) {
            ItemStack hand = player.getInventory().getItemInMainHand();
            String matName = (event.getClick() == ClickType.LEFT && hand != null && hand.getType() != Material.AIR) ?
                    hand.getType().name() : "NETHER_STAR";
            holo.addLine("ITEM:" + matName);
            manager.saveHolograms();
            MessageUtil.sendSuccess(player, "Added floating item: &eITEM:" + matName);
            HoloEditorGUI.open(player, holo);
        } else if ("block".equals(type)) {
            ItemStack hand = player.getInventory().getItemInMainHand();
            String matName = (event.getClick() == ClickType.LEFT && hand != null && hand.getType().isBlock()) ?
                    hand.getType().name() : "BEACON";
            holo.addLine("BLOCK:" + matName);
            manager.saveHolograms();
            MessageUtil.sendSuccess(player, "Added floating block: &eBLOCK:" + matName);
            HoloEditorGUI.open(player, holo);
        } else if ("mob".equals(type)) {
            String mobType = "ALLAY";
            if (event.getClick() == ClickType.RIGHT) mobType = "BLAZE";
            if (event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT) mobType = "WARDEN";
            holo.addLine("MOB:" + mobType);
            manager.saveHolograms();
            MessageUtil.sendSuccess(player, "Added floating creature: &eMOB:" + mobType);
            HoloEditorGUI.open(player, holo);
        }
    }

    private void handleActionClick(InventoryClickEvent event, Player player, HoloHolders.ActionHolder holder) {
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= 45) return;

        Hologram holo = holder.getHologram();
        if (holo == null) {
            player.closeInventory();
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        String actionBtn = GuiItemBuilder.getData(clicked, "action_btn");
        if ("back".equals(actionBtn)) {
            HoloEditorGUI.open(player, holo);
            return;
        }

        if ("clear".equals(actionBtn)) {
            holo.clearClickActions();
            manager.saveHolograms();
            MessageUtil.sendSuccess(player, "Cleared all click actions on Page " + (holo.getActivePageIndex() + 1) + ".");
            HoloActionGUI.open(player, holo);
            return;
        }

        if ("page_jump".equals(actionBtn)) {
            if (event.getClick() == ClickType.RIGHT) {
                holo.addClickAction("PAGE:NEXT");
                manager.saveHolograms();
                MessageUtil.sendSuccess(player, "Added action: &bPAGE:NEXT");
                HoloActionGUI.open(player, holo);
            } else if (event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT) {
                ChatInputManager.prompt(player, "Enter target page number (1, 2, ...):", holo.getId(), holo.getActivePageIndex(), 0, false, targetPage -> {
                    try {
                        int num = Integer.parseInt(targetPage.trim());
                        holo.addClickAction("PAGE:" + num);
                        manager.saveHolograms();
                        MessageUtil.sendSuccess(player, "Added page jump action: &bPAGE:" + num);
                    } catch (NumberFormatException e) {
                        MessageUtil.sendError(player, "Page must be an integer number.");
                    }
                });
            } else {
                holo.addClickAction("PAGE:1");
                manager.saveHolograms();
                MessageUtil.sendSuccess(player, "Added action: &bPAGE:1");
                HoloActionGUI.open(player, holo);
            }
            return;
        }

        if ("player_cmd".equals(actionBtn)) {
            ChatInputManager.prompt(player, "Enter command to run as player (without /):", holo.getId(), holo.getActivePageIndex(), 0, false, cmd -> {
                holo.addClickAction("[player] " + cmd.trim());
                manager.saveHolograms();
                MessageUtil.sendSuccess(player, "Added player command action: &b[player] " + cmd.trim());
            });
            return;
        }

        if ("console_cmd".equals(actionBtn)) {
            ChatInputManager.prompt(player, "Enter command to run as console (without /):", holo.getId(), holo.getActivePageIndex(), 0, false, cmd -> {
                holo.addClickAction("[console] " + cmd.trim());
                manager.saveHolograms();
                MessageUtil.sendSuccess(player, "Added console command action: &b[console] " + cmd.trim());
            });
            return;
        }

        if ("msg_sound".equals(actionBtn)) {
            if (event.getClick() == ClickType.RIGHT) {
                holo.addClickAction("[sound] ENTITY_EXPERIENCE_ORB_PICKUP");
                manager.saveHolograms();
                MessageUtil.sendSuccess(player, "Added sound action: &b[sound] ENTITY_EXPERIENCE_ORB_PICKUP");
                HoloActionGUI.open(player, holo);
            } else {
                ChatInputManager.prompt(player, "Enter chat message to send to player:", holo.getId(), holo.getActivePageIndex(), 0, false, msg -> {
                    holo.addClickAction("[message] " + msg.trim());
                    manager.saveHolograms();
                    MessageUtil.sendSuccess(player, "Added message action: &b[message] " + msg.trim());
                });
            }
        }
    }
}
