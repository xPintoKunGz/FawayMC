package com.bloomerymc.bloomeryx.commands;

import com.bloomerymc.bloomeryx.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TimeWeatherCommands implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase();

        World world = null;
        if (sender instanceof Player) {
            world = ((Player) sender).getWorld();
        } else if (args.length > 0) {
            world = Bukkit.getWorld(args[args.length - 1]);
        }
        if (world == null && !Bukkit.getWorlds().isEmpty()) {
            world = Bukkit.getWorlds().get(0);
        }

        if (world == null) {
            MessageUtil.sendMessage(sender, "&#FF4B4B✕ &cWorld not found.");
            return true;
        }

        // Time commands
        if (name.equals("day")) {
            return setTime(sender, world, 1000, "Day");
        } else if (name.equals("night")) {
            return setTime(sender, world, 13000, "Night");
        } else if (name.equals("noon")) {
            return setTime(sender, world, 6000, "Noon");
        } else if (name.equals("midnight")) {
            return setTime(sender, world, 18000, "Midnight");
        } else if (name.equals("time")) {
            if (args.length < 2) {
                MessageUtil.sendMessage(sender, "&#FF4B4B✕ &cUsage: &f/time <set|add> <number|day|night>");
                return true;
            }
            long value = 0;
            if (args[1].equalsIgnoreCase("day")) value = 1000;
            else if (args[1].equalsIgnoreCase("night")) value = 13000;
            else {
                try {
                    value = Long.parseLong(args[1]);
                } catch (NumberFormatException e) {
                    MessageUtil.sendMessage(sender, "&#FF4B4B✕ &cPlease enter a valid time number.");
                    return true;
                }
            }
            if (args[0].equalsIgnoreCase("set")) {
                world.setTime(value);
            } else if (args[0].equalsIgnoreCase("add")) {
                world.setTime(world.getTime() + value);
            }
            MessageUtil.sendConfigMessage(sender, "time-changed", "{world}", world.getName(), "{time}", String.valueOf(value));
            return true;
        }

        // Weather commands
        if (name.equals("sun") || name.equals("clearweather")) {
            return setWeather(sender, world, false, false, "Clear/Sun");
        } else if (name.equals("rain")) {
            return setWeather(sender, world, true, false, "Rain");
        } else if (name.equals("storm") || name.equals("thunder")) {
            return setWeather(sender, world, true, true, "Thunderstorm");
        } else if (name.equals("weather")) {
            if (args.length == 0) {
                MessageUtil.sendMessage(sender, "&#FF4B4B✕ &cUsage: &f/weather <clear|rain|thunder>");
                return true;
            }
            String mode = args[0].toLowerCase();
            if (mode.equals("clear") || mode.equals("sun")) {
                return setWeather(sender, world, false, false, "Clear");
            } else if (mode.equals("rain")) {
                return setWeather(sender, world, true, false, "Rain");
            } else if (mode.equals("thunder") || mode.equals("storm")) {
                return setWeather(sender, world, true, true, "Thunderstorm");
            }
        }

        return true;
    }

    private boolean setTime(CommandSender sender, World world, long ticks, String display) {
        if (!sender.hasPermission("bloomeryx.command.time")) {
            MessageUtil.sendConfigMessage(sender, "no-permission");
            return true;
        }
        world.setTime(ticks);
        MessageUtil.sendConfigMessage(sender, "time-changed", "{world}", world.getName(), "{time}", display);
        if (sender instanceof Player) MessageUtil.playSuccess((Player) sender);
        return true;
    }

    private boolean setWeather(CommandSender sender, World world, boolean storm, boolean thunder, String display) {
        if (!sender.hasPermission("bloomeryx.command.weather")) {
            MessageUtil.sendConfigMessage(sender, "no-permission");
            return true;
        }
        world.setStorm(storm);
        world.setThundering(thunder);
        MessageUtil.sendConfigMessage(sender, "weather-changed", "{world}", world.getName(), "{weather}", display);
        if (sender instanceof Player) MessageUtil.playSuccess((Player) sender);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> list = new ArrayList<>();
        if (command.getName().equalsIgnoreCase("weather") && args.length == 1) {
            for (String s : Arrays.asList("clear", "rain", "thunder")) {
                if (s.startsWith(args[0].toLowerCase())) list.add(s);
            }
        } else if (command.getName().equalsIgnoreCase("time") && args.length == 1) {
            for (String s : Arrays.asList("set", "add")) {
                if (s.startsWith(args[0].toLowerCase())) list.add(s);
            }
        }
        return list;
    }
}

