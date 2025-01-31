package com.wish.essentialcrates.managers;

import com.wish.essentialcrates.EssentialCrates;
import com.wish.essentialcrates.utils.DebugUtil;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PermissionManager {
    private final EssentialCrates plugin;
    private final Map<String, Long> permissionCache;
    private static final long CACHE_DURATION = 5000; // 5 segundos de caché

    public PermissionManager(EssentialCrates plugin) {
        this.plugin = plugin;
        this.permissionCache = new ConcurrentHashMap<>();
    }

    public boolean hasPermission(Player player, String permission) {
        long startTime = System.nanoTime();
        String cacheKey = player.getUniqueId() + ":" + permission;
        long currentTime = System.currentTimeMillis();

        // Verificar caché
        if (permissionCache.containsKey(cacheKey)) {
            long cachedTime = permissionCache.get(cacheKey);
            if (currentTime - cachedTime < CACHE_DURATION) {
                DebugUtil.debug("Permiso encontrado en caché para " + player.getName() + ": " + permission);
                return true;
            }
            permissionCache.remove(cacheKey);
        }

        // Verificar permisos
        boolean hasPermission = player.hasPermission(permission) ||
                player.hasPermission("essentialcrates.admin");

        // Cachear si tiene permiso
        if (hasPermission) {
            permissionCache.put(cacheKey, currentTime);
            DebugUtil.debug("Nuevo permiso cacheado para " + player.getName() + ": " + permission);
        }

        DebugUtil.performance("Verificación de permiso", startTime);
        return hasPermission;
    }

    public void clearCache(Player player) {
        String prefix = player.getUniqueId() + ":";
        permissionCache.entrySet().removeIf(entry -> entry.getKey().startsWith(prefix));
    }

    public void clearCache() {
        permissionCache.clear();
    }
}