package com.wish.essentialcrates.utils;

import com.wish.essentialcrates.EssentialCrates;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

public class DebugUtil {
    private static final EssentialCrates plugin = EssentialCrates.getInstance();
    private static final Set<UUID> debugMode = new HashSet<>();
    private static boolean globalDebug = false;

    public static void toggleDebug(CommandSender sender) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            UUID uuid = player.getUniqueId();
            if (debugMode.contains(uuid)) {
                debugMode.remove(uuid);
                sender.sendMessage(ConfigUtil.color("&cModo debug desactivado."));
            } else {
                debugMode.add(uuid);
                sender.sendMessage(ConfigUtil.color("&aModo debug activado."));
            }
        } else {
            globalDebug = !globalDebug;
            sender.sendMessage(ConfigUtil.color(globalDebug ? "&aModo debug global activado." : "&cModo debug global desactivado."));
        }
    }

    public static void debug(String message) {
        if (globalDebug) {
            plugin.getLogger().info("[Debug] " + message);
        }

        for (UUID uuid : debugMode) {
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null && player.isOnline()) {
                player.sendMessage(ChatColor.GRAY + "[Debug] " + message);
            }
        }
    }

    public static void debug(String message, Level level) {
        if (globalDebug) {
            plugin.getLogger().log(level, "[Debug] " + message);
        }

        String prefix;
        ChatColor color;

        switch (level.getName()) {
            case "WARNING":
                prefix = "[⚠]";
                color = ChatColor.GOLD;
                break;
            case "SEVERE":
                prefix = "[❌]";
                color = ChatColor.RED;
                break;
            case "INFO":
            default:
                prefix = "[ℹ]";
                color = ChatColor.GRAY;
                break;
        }

        for (UUID uuid : debugMode) {
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null && player.isOnline()) {
                player.sendMessage(color + prefix + " " + message);
            }
        }
    }

    public static void performance(String action, long startTime) {
        long endTime = System.nanoTime();
        double ms = (endTime - startTime) / 1_000_000.0;
        debug("Performance - " + action + ": " + String.format("%.2f", ms) + "ms");
    }

    public static void error(String message, Throwable error) {
        debug("Error: " + message, Level.SEVERE);
        if (globalDebug) {
            error.printStackTrace();
        }

        for (UUID uuid : debugMode) {
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null && player.isOnline() && player.hasPermission("essentialcrates.admin")) {
                player.sendMessage(ChatColor.RED + "[Debug] Error: " + message);
                player.sendMessage(ChatColor.RED + "[Debug] Causa: " + error.getMessage());
            }
        }
    }

    public static boolean isDebugEnabled(CommandSender sender) {
        if (globalDebug) return true;
        if (sender instanceof Player) {
            return debugMode.contains(((Player) sender).getUniqueId());
        }
        return false;
    }
}