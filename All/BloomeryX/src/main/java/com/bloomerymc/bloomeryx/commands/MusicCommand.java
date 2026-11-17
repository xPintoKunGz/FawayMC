package com.bloomerymc.bloomeryx.commands;

import com.bloomerymc.bloomeryx.BloomeryX;
import com.bloomerymc.bloomeryx.manager.MusicManager;
import com.bloomerymc.bloomeryx.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MusicCommand implements CommandExecutor, TabCompleter {

    private final BloomeryX plugin;

    public MusicCommand(BloomeryX plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                MessageUtil.sendConfigMessage(sender, "player-only");
                return true;
            }
            Player player = (Player) sender;
            if (!player.hasPermission("bloomeryx.command.music")) {
                MessageUtil.sendConfigMessage(sender, "no-permission");
                return true;
            }
            plugin.getMusicManager().openJukeboxGui(player, 0);
            return true;
        }

        String sub = args[0].toLowerCase();

        // Admin controls
        if (sub.equals("admin")) {
            if (!sender.hasPermission("bloomeryx.admin")) {
                MessageUtil.sendConfigMessage(sender, "no-permission");
                return true;
            }

            if (args.length < 2) {
                MessageUtil.sendMessage(sender, "&#FF4B4B✕ &cUsage: &f/music admin <autoplay <on|off> | default <song>>");
                return true;
            }

            if (args[1].equalsIgnoreCase("autoplay")) {
                if (args.length < 3) {
                    boolean current = plugin.getMusicManager().isGlobalAutoplayEnabled();
                    MessageUtil.sendMessage(sender, "&7Global join autoplay: " + (current ? "&aENABLED" : "&cDISABLED"));
                    return true;
                }

                boolean enable = args[2].equalsIgnoreCase("on") || args[2].equalsIgnoreCase("true") || args[2].equalsIgnoreCase("enable");
                plugin.getMusicManager().setGlobalAutoplay(enable);
                MessageUtil.sendMessage(sender, enable ?
                        "&#70E000✔ &a[Admin] Global music autoplay on join has been ENABLED." :
                        "&#FF758C✕ &c[Admin] Global music autoplay on join has been DISABLED.");
                return true;
            }

            if (args[1].equalsIgnoreCase("default")) {
                if (args.length < 3) {
                    MessageUtil.sendMessage(sender, "&#FF4B4B✕ &cUsage: &f/music admin default <song_id>");
                    return true;
                }
                String songId = args[2].toLowerCase();
                if (plugin.getMusicManager().getSong(songId) == null) {
                    MessageUtil.sendMessage(sender, "&#FF4B4B✕ &cSong not found: &f" + songId);
                    return true;
                }
                plugin.getMusicManager().setDefaultSong(songId);
                MessageUtil.sendMessage(sender, "&#70E000✔ &a[Admin] Set default server song to: &#B2EBF2" + songId);
                return true;
            }
        }

        // GUI / Menu
        if (sub.equals("gui") || sub.equals("menu")) {
            if (sender instanceof Player) {
                plugin.getMusicManager().openJukeboxGui((Player) sender, 0);
            }
            return true;
        }

        // Pause
        if (sub.equals("pause")) {
            Player target = (sender instanceof Player) ? (Player) sender : null;
            if (args.length > 1 && sender.hasPermission("bloomeryx.command.music.others")) {
                target = Bukkit.getPlayer(args[1]);
            }
            if (target != null) {
                plugin.getMusicManager().pauseSong(target);
            }
            return true;
        }

        // Resume
        if (sub.equals("resume")) {
            Player target = (sender instanceof Player) ? (Player) sender : null;
            if (args.length > 1 && sender.hasPermission("bloomeryx.command.music.others")) {
                target = Bukkit.getPlayer(args[1]);
            }
            if (target != null) {
                plugin.getMusicManager().resumeSong(target);
            }
            return true;
        }

        // Next
        if (sub.equals("next")) {
            Player target = (sender instanceof Player) ? (Player) sender : null;
            if (args.length > 1 && sender.hasPermission("bloomeryx.command.music.others")) {
                target = Bukkit.getPlayer(args[1]);
            }
            if (target != null) {
                plugin.getMusicManager().nextSong(target);
            }
            return true;
        }

        // Previous
        if (sub.equals("prev") || sub.equals("previous")) {
            Player target = (sender instanceof Player) ? (Player) sender : null;
            if (args.length > 1 && sender.hasPermission("bloomeryx.command.music.others")) {
                target = Bukkit.getPlayer(args[1]);
            }
            if (target != null) {
                plugin.getMusicManager().previousSong(target);
            }
            return true;
        }

        // Random
        if (sub.equals("random")) {
            Player target = (sender instanceof Player) ? (Player) sender : null;
            if (args.length > 1 && sender.hasPermission("bloomeryx.command.music.others")) {
                target = Bukkit.getPlayer(args[1]);
            }
            if (target != null) {
                plugin.getMusicManager().playRandomSong(target);
            }
            return true;
        }

        // Radio
        if (sub.equals("radio")) {
            if (args.length > 1 && args[1].equalsIgnoreCase("skip")) {
                if (!sender.hasPermission("bloomeryx.admin")) {
                    MessageUtil.sendConfigMessage(sender, "no-permission");
                    return true;
                }
                plugin.getMusicManager().getRadio().skipSong();
                MessageUtil.sendMessage(sender, "&#70E000✔ &aSkipped current song on Bloomery Radio.");
                return true;
            }

            if (!(sender instanceof Player)) {
                MessageUtil.sendConfigMessage(sender, "player-only");
                return true;
            }
            plugin.getMusicManager().getRadio().toggleListener((Player) sender);
            return true;
        }

        // Volume
        if (sub.equals("volume") || sub.equals("vol")) {
            if (!(sender instanceof Player)) {
                MessageUtil.sendConfigMessage(sender, "player-only");
                return true;
            }
            Player p = (Player) sender;
            if (args.length < 2) {
                int cur = plugin.getPlayerDataManager().getMusicVolume(p.getUniqueId());
                MessageUtil.sendMessage(p, "&#B2EBF2🔊 Current volume: &#70E000" + cur + "% &7(Type &f/music volume <10-100>&7)");
                return true;
            }
            try {
                int vol = Integer.parseInt(args[1]);
                vol = Math.max(10, Math.min(100, vol));
                plugin.getPlayerDataManager().setMusicVolume(p.getUniqueId(), vol);
                MessageUtil.sendMessage(p, "&#70E000✔ Set your music volume to: &#B2EBF2" + vol + "%");
                MessageUtil.playSuccess(p);
            } catch (NumberFormatException e) {
                MessageUtil.sendMessage(p, "&#FF4B4B✕ &cPlease enter a number between 10 and 100.");
            }
            return true;
        }

        // Reload
        if (sub.equals("reload")) {
            if (!sender.hasPermission("bloomeryx.admin")) {
                MessageUtil.sendConfigMessage(sender, "no-permission");
                return true;
            }
            plugin.getMusicManager().loadMusic();
            int count = plugin.getMusicManager().getSongs().size();
            MessageUtil.sendMessage(sender, "&#70E000✔ &aReloaded songs from &fsongs/ &aand &fmusic.yml! (Found " + count + " songs)");
            return true;
        }

        // Stop
        if (sub.equals("stop")) {
            if (args.length > 1 && sender.hasPermission("bloomeryx.command.music.others")) {
                if (args[1].equalsIgnoreCase("all")) {
                    for (Player online : Bukkit.getOnlinePlayers()) {
                        plugin.getMusicManager().muteAndStopMusic(online);
                    }
                    MessageUtil.sendMessage(sender, "&#70E000✔ &aStopped music playback for all online players.");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    MessageUtil.sendConfigMessage(sender, "player-not-found", "{target}", args[1]);
                    return true;
                }
                plugin.getMusicManager().muteAndStopMusic(target);
                MessageUtil.sendMessage(sender, "&#70E000✔ &aStopped music for &f" + target.getName());
                return true;
            }

            if (sender instanceof Player) {
                plugin.getMusicManager().muteAndStopMusic((Player) sender);
            }
            return true;
        }

        // Play
        if (sub.equals("play")) {
            if (args.length < 2) {
                MessageUtil.sendMessage(sender, "&#FF4B4B✕ &cUsage: &f/music play <song_id> [player|all]");
                return true;
            }

            String songId = args[1].toLowerCase();
            MusicManager.Song song = plugin.getMusicManager().getSong(songId);
            if (song == null) {
                MessageUtil.sendMessage(sender, "&#FF4B4B✕ &cSong not found: &f" + songId + " &7(Type &f/music &7to browse all songs)");
                return true;
            }

            if (args.length > 2 && sender.hasPermission("bloomeryx.command.music.others")) {
                if (args[2].equalsIgnoreCase("all")) {
                    for (Player online : Bukkit.getOnlinePlayers()) {
                        plugin.getMusicManager().playSong(online, song);
                    }
                    MessageUtil.sendMessage(sender, "&#70E000✔ &aPlaying &f" + song.getTitle() + " &afor all players!");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[2]);
                if (target == null) {
                    MessageUtil.sendConfigMessage(sender, "player-not-found", "{target}", args[2]);
                    return true;
                }
                plugin.getMusicManager().playSong(target, song);
                MessageUtil.sendMessage(sender, "&#70E000✔ &aPlaying &f" + song.getTitle() + " &afor &f" + target.getName());
                return true;
            }

            if (!(sender instanceof Player)) {
                MessageUtil.sendConfigMessage(sender, "player-only");
                return true;
            }
            plugin.getMusicManager().playSong((Player) sender, song);
            return true;
        }

        MessageUtil.sendMessage(sender, "&#FF4B4B✕ &cUsage: &f/music &7[play|pause|resume|next|prev|random|radio|volume|stop|reload]");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 1) {
            for (String sub : Arrays.asList("play", "pause", "resume", "stop", "next", "prev", "random", "radio", "volume", "gui", "reload", "admin")) {
                if (sub.startsWith(args[0].toLowerCase())) list.add(sub);
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("admin")) {
            for (String sub : Arrays.asList("autoplay", "default")) {
                if (sub.startsWith(args[1].toLowerCase())) list.add(sub);
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("autoplay")) {
            for (String sub : Arrays.asList("on", "off")) {
                if (sub.startsWith(args[2].toLowerCase())) list.add(sub);
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("default")) {
            for (String sId : plugin.getMusicManager().getSongs().keySet()) {
                if (sId.startsWith(args[2].toLowerCase())) list.add(sId);
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("radio")) {
            if (sender.hasPermission("bloomeryx.admin") && "skip".startsWith(args[1].toLowerCase())) {
                list.add("skip");
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("play")) {
            for (String songId : plugin.getMusicManager().getSongs().keySet()) {
                if (songId.startsWith(args[1].toLowerCase())) list.add(songId);
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("play")) {
            if ("all".startsWith(args[2].toLowerCase())) list.add("all");
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(args[2].toLowerCase())) list.add(p.getName());
            }
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("pause") || args[0].equalsIgnoreCase("resume") || args[0].equalsIgnoreCase("next") || args[0].equalsIgnoreCase("prev") || args[0].equalsIgnoreCase("random"))) {
            if (sender.hasPermission("bloomeryx.command.music.others")) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) list.add(p.getName());
                }
            }
        }
        return list;
    }
}
