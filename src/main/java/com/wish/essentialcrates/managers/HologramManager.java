package com.wish.essentialcrates.managers;

import com.wish.essentialcrates.EssentialCrates;
import com.wish.essentialcrates.models.Crate;
import com.wish.essentialcrates.utils.ConfigUtil;
import com.wish.essentialcrates.utils.DebugUtil;
import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class HologramManager {
    private final EssentialCrates plugin;
    private final Map<Location, String> holograms;
    private final Map<String, HologramAnimation> animations;
    private boolean enabled;

    public HologramManager(EssentialCrates plugin) {
        this.plugin = plugin;
        this.holograms = new HashMap<>();
        this.animations = new HashMap<>();
        this.enabled = plugin.getServer().getPluginManager().getPlugin("DecentHolograms") != null;

        if (!enabled) {
            plugin.getLogger().warning("DecentHolograms no encontrado! Los hologramas están deshabilitados.");
        } else {
            startUpdateTask();
        }
    }

    private void startUpdateTask() {
        int interval = plugin.getConfig().getInt("settings.holograms.update-interval", 300);
        new BukkitRunnable() {
            @Override
            public void run() {
                updateAllHolograms();
            }
        }.runTaskTimer(plugin, interval * 20L, interval * 20L);
    }

    public void createHologram(Location location, Crate crate) {
        if (!enabled) return;
        long startTime = System.nanoTime();

        Crate.HologramConfig config = crate.getHologramConfig();
        List<String> lines = new ArrayList<>();
        double currentOffset = 0.0;

        // Título
        lines.add(ConfigUtil.color(config.getTitleFormat().replace("%crate%", crate.getDisplayName())));
        currentOffset += config.getTitleOffset();

        // Recompensas
        if (config.isRewardsEnabled()) {
            lines.add(ConfigUtil.color(config.getRewardsHeader()));
            currentOffset += config.getRewardsOffset();

            crate.getRewards().stream()
                    .sorted((r1, r2) -> Double.compare(r2.getChance(), r1.getChance()))
                    .limit(config.getMaxShown())
                    .forEach(reward -> {
                        boolean isRare = reward.getChance() <= config.getRareThreshold();
                        String format = isRare ? config.getRareFormat() : config.getRewardsFormat();
                        lines.add(ConfigUtil.color(format
                                .replace("%reward%", reward.getDisplayItem().getItemMeta().getDisplayName())
                                .replace("%chance%", String.format("%.1f", reward.getChance()))));
                    });
        }

        // Pie de página
        if (config.isFooterEnabled()) {
            currentOffset += config.getFooterOffset();
            for (String line : config.getFooterLines()) {
                lines.add(ConfigUtil.color(line));
            }
        }

        // Calcular ubicación final
        double heightOffset = plugin.getConfig().getDouble("settings.holograms.height", 2.5);
        Location holoLoc = location.clone().add(0.5, heightOffset, 0.5);

        // Crear holograma
        String holoId = "crate_" + crate.getId() + "_" + location.hashCode();
        Hologram hologram = DHAPI.createHologram(holoId, holoLoc, lines);

        // Guardar referencia
        holograms.put(location, holoId);
        DebugUtil.performance("Creación de holograma", startTime);
    }

    private void updateAllHolograms() {
        long startTime = System.nanoTime();
        int count = 0;
        Map<Location, String> currentHolograms = new HashMap<>(holograms);

        for (Map.Entry<Location, String> entry : currentHolograms.entrySet()) {
            Optional<Crate> optionalCrate = plugin.getCrateManager().getCrateAtLocation(entry.getKey());
            if (optionalCrate.isPresent()) {
                updateHologram(entry.getKey(), optionalCrate.get());
                count++;
            }
        }

        DebugUtil.debug("Actualizados " + count + " hologramas");
        DebugUtil.performance("Actualización de hologramas", startTime);
    }

    public void updateHologram(Location location, Crate crate) {
        removeHologram(location);
        createHologram(location, crate);
    }

    public void removeHologram(Location location) {
        if (!enabled) return;

        String holoId = holograms.remove(location);
        if (holoId != null) {
            // Detener animación si existe
            HologramAnimation animation = animations.remove(holoId);
            if (animation != null) {
                animation.stop();
            }

            // Eliminar holograma
            Hologram hologram = DHAPI.getHologram(holoId);
            if (hologram != null) {
                hologram.delete();
            }
        }
    }

    public void removeAllHolograms() {
        if (!enabled) return;

        // Detener todas las animaciones
        animations.values().forEach(HologramAnimation::stop);
        animations.clear();

        // Eliminar todos los hologramas
        for (String holoId : new ArrayList<>(holograms.values())) {
            Hologram hologram = DHAPI.getHologram(holoId);
            if (hologram != null) {
                hologram.delete();
            }
        }
        holograms.clear();
    }

    public boolean isEnabled() {
        return enabled;
    }

    private enum AnimationType {
        WAVE,
        BOUNCE,
        ROTATE
    }

    public void reloadAllHolograms() {
        removeAllHolograms();
        plugin.getCrateManager().getCrateLocations().forEach((location, crateId) -> {
            plugin.getCrateManager().getCrate(crateId).ifPresent(crate ->
                    createHologram(location, crate));
        });
    }

    private class HologramAnimation {
        private final Hologram hologram;
        private final AnimationType type;
        private final double speed;
        private final double amplitude;
        private BukkitRunnable task;
        private double tick = 0;

        public HologramAnimation(Hologram hologram, AnimationType type, double speed, double amplitude) {
            this.hologram = hologram;
            this.type = type;
            this.speed = speed;
            this.amplitude = amplitude;
        }

        public void start() {
            task = new BukkitRunnable() {
                @Override
                public void run() {
                    tick += speed * 0.1;
                    Location loc = hologram.getLocation();

                    switch (type) {
                        case WAVE:
                            double y = Math.sin(tick) * amplitude;
                            hologram.setLocation(loc.clone().add(0, y, 0));
                            break;
                        case BOUNCE:
                            double bounce = Math.abs(Math.sin(tick)) * amplitude;
                            hologram.setLocation(loc.clone().add(0, bounce, 0));
                            break;
                        case ROTATE:
                            double x = Math.cos(tick) * amplitude;
                            double z = Math.sin(tick) * amplitude;
                            hologram.setLocation(loc.clone().add(x, 0, z));
                            break;
                    }
                }
            };
            task.runTaskTimer(plugin, 0L, 1L);
        }

        public void stop() {
            if (task != null) {
                task.cancel();
                task = null;
            }
        }
    }
}