package com.wish.essentialcrates.managers;

import com.wish.essentialcrates.EssentialCrates;
import com.wish.essentialcrates.models.Crate;
import com.wish.essentialcrates.utils.DebugUtil;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class CacheManager {
    private final EssentialCrates plugin;

    @Getter
    private final Map<String, CrateCache> crateCache;
    @Getter
    private final Map<Location, LocationCache> locationCache;

    private static final long CACHE_CLEANUP_INTERVAL = TimeUnit.MINUTES.toMillis(5);
    private static final long CACHE_EXPIRY = TimeUnit.MINUTES.toMillis(30);

    public CacheManager(EssentialCrates plugin) {
        this.plugin = plugin;
        this.crateCache = new ConcurrentHashMap<>();
        this.locationCache = new ConcurrentHashMap<>();
        startCleanupTask();
    }

    private void startCleanupTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                cleanup();
            }
        }.runTaskTimerAsynchronously(plugin, CACHE_CLEANUP_INTERVAL / 50, CACHE_CLEANUP_INTERVAL / 50);
    }

    public void cleanup() {
        long startTime = System.nanoTime();
        long currentTime = System.currentTimeMillis();
        int cratesBefore = crateCache.size();
        int locationsBefore = locationCache.size();

        // Limpiar caché de crates
        crateCache.entrySet().removeIf(entry ->
                currentTime - entry.getValue().getLastAccessed() > CACHE_EXPIRY);

        // Limpiar caché de ubicaciones
        locationCache.entrySet().removeIf(entry ->
                currentTime - entry.getValue().getLastAccessed() > CACHE_EXPIRY);

        DebugUtil.debug("Limpieza de caché completada - Crates: " + cratesBefore + " -> " +
                crateCache.size() + ", Ubicaciones: " + locationsBefore + " -> " + locationCache.size());
        DebugUtil.performance("Limpieza de caché", startTime);
    }

    public void cacheCrate(String id, Crate crate) {
        crateCache.put(id, new CrateCache(crate));
    }

    public void cacheLocation(Location location, String crateId) {
        locationCache.put(location, new LocationCache(crateId));
    }

    public Crate getCachedCrate(String id) {
        CrateCache cache = crateCache.get(id);
        if (cache != null) {
            cache.updateAccess();
            return cache.getCrate();
        }
        return null;
    }

    public String getCachedCrateId(Location location) {
        LocationCache cache = locationCache.get(location);
        if (cache != null) {
            cache.updateAccess();
            return cache.getCrateId();
        }
        return null;
    }

    public void invalidateCrate(String id) {
        crateCache.remove(id);
    }

    public void invalidateLocation(Location location) {
        locationCache.remove(location);
    }

    public void invalidateAll() {
        crateCache.clear();
        locationCache.clear();
    }

    @Getter
    private static class CrateCache {
        private final Crate crate;
        private long lastAccessed;

        public CrateCache(Crate crate) {
            this.crate = crate;
            this.lastAccessed = System.currentTimeMillis();
        }

        public void updateAccess() {
            this.lastAccessed = System.currentTimeMillis();
        }
    }

    @Getter
    private static class LocationCache {
        private final String crateId;
        private long lastAccessed;

        public LocationCache(String crateId) {
            this.crateId = crateId;
            this.lastAccessed = System.currentTimeMillis();
        }

        public void updateAccess() {
            this.lastAccessed = System.currentTimeMillis();
        }
    }
}