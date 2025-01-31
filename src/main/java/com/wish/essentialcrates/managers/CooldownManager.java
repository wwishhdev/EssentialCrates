package com.wish.essentialcrates.managers;

import com.wish.essentialcrates.utils.DebugUtil;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CooldownManager {
    private static final Map<String, Map<UUID, Long>> cooldowns = new ConcurrentHashMap<>();

    public static void setCooldown(Player player, String type, int seconds) {
        long startTime = System.nanoTime();
        cooldowns.computeIfAbsent(type, k -> new ConcurrentHashMap<>())
                .put(player.getUniqueId(), System.currentTimeMillis() + (seconds * 1000L));

        DebugUtil.debug("Cooldown establecido para " + player.getName() +
                " - Tipo: " + type + ", Duración: " + seconds + "s");
        DebugUtil.performance("Establecer cooldown", startTime);
    }

    public static boolean isOnCooldown(Player player, String type) {
        Map<UUID, Long> cooldownMap = cooldowns.get(type);
        if (cooldownMap == null) return false;

        Long cooldownTime = cooldownMap.get(player.getUniqueId());
        if (cooldownTime == null) return false;

        if (cooldownTime > System.currentTimeMillis()) {
            DebugUtil.debug("Jugador " + player.getName() + " en cooldown para " + type +
                    " - Tiempo restante: " + getRemainingTime(player, type) + "s");
            return true;
        }

        cooldownMap.remove(player.getUniqueId());
        DebugUtil.debug("Cooldown expirado para " + player.getName() + " - Tipo: " + type);
        return false;
    }

    public static int getRemainingTime(Player player, String type) {
        Map<UUID, Long> cooldownMap = cooldowns.get(type);
        if (cooldownMap == null) return 0;

        Long cooldownTime = cooldownMap.get(player.getUniqueId());
        if (cooldownTime == null) return 0;

        return (int) ((cooldownTime - System.currentTimeMillis()) / 1000);
    }

    public static void clearCooldowns(Player player) {
        for (Map<UUID, Long> cooldownMap : cooldowns.values()) {
            cooldownMap.remove(player.getUniqueId());
        }
    }

    public static void clearAllCooldowns() {
        cooldowns.clear();
    }
}