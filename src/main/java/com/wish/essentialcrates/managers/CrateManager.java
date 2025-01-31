package com.wish.essentialcrates.managers;

import com.wish.essentialcrates.EssentialCrates;
import com.wish.essentialcrates.models.Crate;
import com.wish.essentialcrates.utils.DebugUtil;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class CrateManager {
    private final EssentialCrates plugin;
    private final DataManager dataManager;
    @Getter
    private final Map<String, Crate> crates;
    @Getter
    private final Map<Location, String> crateLocations;

    public CrateManager(EssentialCrates plugin, DataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.crates = new HashMap<>();
        this.crateLocations = new HashMap<>();
        loadCrates();
        loadLocations();
    }

    public void loadCrates() {
        long startTime = System.nanoTime();
        int loadedCount = 0;

        ConfigurationSection cratesSection = plugin.getConfig().getConfigurationSection("crates");
        if (cratesSection != null) {
            for (String id : cratesSection.getKeys(false)) {
                ConfigurationSection crateSection = cratesSection.getConfigurationSection(id);
                if (crateSection != null) {
                    crates.put(id, new Crate(id, crateSection));
                    loadedCount++;
                }
            }
        }

        DebugUtil.debug("Cargadas " + loadedCount + " crates");
        DebugUtil.performance("Carga de crates", startTime);
    }

    private void loadLocations() {
        if (plugin.getConfig().getString("settings.storage.type", "YAML").equalsIgnoreCase("MYSQL")) {
            loadLocationsMySQL();
        } else {
            loadLocationsYAML();
        }

        // Crear hologramas para todas las crates cargadas
        if (plugin.getHologramManager().isEnabled()) {
            crateLocations.forEach((location, crateId) ->
                    getCrate(crateId).ifPresent(crate ->
                            plugin.getHologramManager().createHologram(location, crate)));
        }
    }

    private void loadLocationsMySQL() {
        try (Connection conn = dataManager.getHikari().getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM crate_locations")) {
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Location loc = new Location(
                        plugin.getServer().getWorld(rs.getString("world")),
                        rs.getInt("x"),
                        rs.getInt("y"),
                        rs.getInt("z")
                );
                crateLocations.put(loc, rs.getString("crate_id"));
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error al cargar ubicaciones de MySQL!");
            e.printStackTrace();
        }
    }

    private void loadLocationsYAML() {
        ConfigurationSection locSection = dataManager.getDataConfig().getConfigurationSection("locations");
        if (locSection == null) return;

        for (String key : locSection.getKeys(false)) {
            ConfigurationSection section = locSection.getConfigurationSection(key);
            if (section != null) {
                Location loc = new Location(
                        plugin.getServer().getWorld(section.getString("world")),
                        section.getInt("x"),
                        section.getInt("y"),
                        section.getInt("z")
                );
                crateLocations.put(loc, section.getString("crate-id"));
            }
        }
    }

    public Optional<Crate> getCrate(String id) {
        // Intentar obtener del caché primero
        Crate cachedCrate = plugin.getCacheManager().getCachedCrate(id);
        if (cachedCrate != null) {
            return Optional.of(cachedCrate);
        }

        // Si no está en caché, obtener del mapa y cachear
        Crate crate = crates.get(id);
        if (crate != null) {
            plugin.getCacheManager().cacheCrate(id, crate);
        }
        return Optional.ofNullable(crate);
    }

    public Optional<Crate> getCrateAtLocation(Location location) {
        // Intentar obtener del caché primero
        String cachedCrateId = plugin.getCacheManager().getCachedCrateId(location);
        if (cachedCrateId != null) {
            return getCrate(cachedCrateId);
        }

        // Si no está en caché, obtener del mapa y cachear
        String crateId = crateLocations.get(location);
        if (crateId != null) {
            plugin.getCacheManager().cacheLocation(location, crateId);
        }
        return crateId == null ? Optional.empty() : getCrate(crateId);
    }

    public void saveCrateLocation(Location location, String crateId) {
        long startTime = System.nanoTime();
        crateLocations.put(location, crateId);
        plugin.getCacheManager().cacheLocation(location, crateId);

        // Crear holograma
        getCrate(crateId).ifPresent(crate ->
                plugin.getHologramManager().createHologram(location, crate));

        // Guardar en almacenamiento
        if (plugin.getConfig().getString("settings.storage.type", "YAML").equalsIgnoreCase("MYSQL")) {
            saveCrateLocationMySQL(location, crateId);
        } else {
            saveCrateLocationYAML(location, crateId);
        }
        DebugUtil.debug("Guardada ubicación de crate " + crateId + " en " + location);
        DebugUtil.performance("Guardado de ubicación de crate", startTime);
    }

    private void saveCrateLocationMySQL(Location location, String crateId) {
        try (Connection conn = dataManager.getHikari().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO crate_locations (world, x, y, z, crate_id) VALUES (?, ?, ?, ?, ?)"
             )) {
            ps.setString(1, location.getWorld().getName());
            ps.setInt(2, location.getBlockX());
            ps.setInt(3, location.getBlockY());
            ps.setInt(4, location.getBlockZ());
            ps.setString(5, crateId);
            ps.executeUpdate();
        } catch (Exception e) {
            plugin.getLogger().severe("Error al guardar ubicación en MySQL!");
            e.printStackTrace();
        }
    }

    private void saveCrateLocationYAML(Location location, String crateId) {
        String key = String.format("%s,%d,%d,%d",
                location.getWorld().getName(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ()
        );

        ConfigurationSection section = dataManager.getDataConfig().createSection("locations." + key);
        section.set("world", location.getWorld().getName());
        section.set("x", location.getBlockX());
        section.set("y", location.getBlockY());
        section.set("z", location.getBlockZ());
        section.set("crate-id", crateId);

        dataManager.saveData();
    }
}