package com.wish.essentialcrates.utils;

import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;
import com.wish.essentialcrates.EssentialCrates;

import java.util.List;

public class EffectUtil {
    private static final EssentialCrates plugin = EssentialCrates.getInstance();

    public static void playCrateOpenEffect(Location location) {
        if (!plugin.getConfig().getBoolean("settings.effects.enabled", true)) return;

        // Efectos de sonido básicos
        location.getWorld().playEffect(location, Effect.ENDER_SIGNAL, 0);
        location.getWorld().playEffect(location, Effect.CLICK1, 0);

        // Cargar efectos de partículas configurados
        ConfigurationSection particleSection = plugin.getConfig().getConfigurationSection("settings.effects.particles.types.open");
        if (particleSection != null) {
            for (String effectConfig : particleSection.getStringList("")) {
                String[] parts = effectConfig.split(":");
                if (parts.length >= 3) {
                    String type = parts[0];
                    Effect effect = Effect.valueOf(parts[1]);
                    double radius = Double.parseDouble(parts[2]);
                    double duration = parts.length > 3 ? Double.parseDouble(parts[3]) : 2.0;

                    switch (type.toUpperCase()) {
                        case "SPIRAL":
                            playSpiral(location, effect, radius, duration);
                            break;
                        case "HELIX":
                            playHelix(location, effect, radius, duration);
                            break;
                    }
                }
            }
        }
    }

    public static void playRewardEffect(Player player, boolean rare) {
        if (!plugin.getConfig().getBoolean("settings.effects.enabled", true)) return;

        Location location = player.getLocation();

        // Reproducir sonidos configurados
        List<String> sounds = plugin.getConfig().getStringList("settings.effects.sounds." +
                (rare ? "rare-sounds" : "normal-sounds"));

        for (String soundConfig : sounds) {
            String[] parts = soundConfig.split(":");
            if (parts.length >= 3) {
                player.playSound(location, parts[0],
                        Float.parseFloat(parts[1]),
                        Float.parseFloat(parts[2]));
            }
        }

        // Efectos de partículas
        ConfigurationSection particleSection = plugin.getConfig()
                .getConfigurationSection("settings.effects.particles.types." + (rare ? "rare" : "normal"));

        if (particleSection != null) {
            for (String effectConfig : particleSection.getStringList("")) {
                String[] parts = effectConfig.split(":");
                if (parts.length >= 3) {
                    String type = parts[0];
                    Effect effect = Effect.valueOf(parts[1]);
                    double radius = Double.parseDouble(parts[2]);
                    double duration = parts.length > 3 ? Double.parseDouble(parts[3]) : 2.0;

                    switch (type.toUpperCase()) {
                        case "CIRCLE":
                            playCircle(location, effect, radius, duration);
                            break;
                        case "BURST":
                            playBurst(location, effect, radius);
                            break;
                    }
                }
            }
        }

        // Fuegos artificiales para recompensas raras
        if (rare && plugin.getConfig().getBoolean("settings.effects.fireworks.enabled", true)) {
            spawnFirework(location);
        }
    }

    private static void playSpiral(Location location, Effect effect, double radius, double duration) {
        new BukkitRunnable() {
            double phi = 0;
            final double maxPhi = duration * Math.PI;

            public void run() {
                phi += Math.PI/16;
                for (double theta = 0; theta <= 2*Math.PI; theta += Math.PI/8) {
                    double x = radius * Math.cos(theta) * Math.sin(phi);
                    double y = radius * Math.cos(phi) + 1.5;
                    double z = radius * Math.sin(theta) * Math.sin(phi);

                    location.getWorld().playEffect(location.clone().add(x, y, z), effect, 0);
                }

                if (phi >= maxPhi) {
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    private static void playHelix(Location location, Effect effect, double radius, double duration) {
        new BukkitRunnable() {
            double y = 0;
            final double maxY = duration * 2;

            public void run() {
                y += 0.1;
                double x = radius * Math.cos(y * 5);
                double z = radius * Math.sin(y * 5);

                location.getWorld().playEffect(location.clone().add(x, y, z), effect, 0);
                location.getWorld().playEffect(location.clone().add(-x, y, -z), effect, 0);

                if (y >= maxY) {
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    private static void playCircle(Location location, Effect effect, double radius, double duration) {
        new BukkitRunnable() {
            double angle = 0;
            int ticks = 0;
            final int maxTicks = (int)(duration * 20);

            public void run() {
                angle += Math.PI/8;
                double x = radius * Math.cos(angle);
                double z = radius * Math.sin(angle);

                location.getWorld().playEffect(location.clone().add(x, 1, z), effect, 0);

                ticks++;
                if (ticks >= maxTicks) {
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    private static void playBurst(Location location, Effect effect, double radius) {
        for (double phi = 0; phi <= Math.PI; phi += Math.PI/8) {
            for (double theta = 0; theta <= 2*Math.PI; theta += Math.PI/8) {
                double x = radius * Math.cos(theta) * Math.sin(phi);
                double y = radius * Math.cos(phi);
                double z = radius * Math.sin(theta) * Math.sin(phi);

                location.getWorld().playEffect(location.clone().add(x, y + 1, z), effect, 0);
            }
        }
    }

    private static void spawnFirework(Location location) {
        ConfigurationSection fw = plugin.getConfig().getConfigurationSection("settings.effects.fireworks.rare");
        if (fw == null) return;

        Firework firework = location.getWorld().spawn(location, Firework.class);
        FireworkMeta meta = firework.getFireworkMeta();

        FireworkEffect.Builder builder = FireworkEffect.builder()
                .with(FireworkEffect.Type.valueOf(fw.getString("type", "BALL_LARGE")));

        // Colores
        for (String colorName : fw.getStringList("colors")) {
            builder.withColor(getColor(colorName));
        }

        // Colores de desvanecimiento
        for (String colorName : fw.getStringList("fade")) {
            builder.withFade(getColor(colorName));
        }

        if (fw.getBoolean("trail", true)) builder.withTrail();
        if (fw.getBoolean("flicker", true)) builder.withFlicker();

        meta.addEffect(builder.build());
        meta.setPower(plugin.getConfig().getInt("settings.effects.fireworks.power", 1));
        firework.setFireworkMeta(meta);
    }

    private static Color getColor(String name) {
        switch (name.toUpperCase()) {
            case "AQUA": return Color.AQUA;
            case "BLACK": return Color.BLACK;
            case "BLUE": return Color.BLUE;
            case "FUCHSIA": return Color.FUCHSIA;
            case "GRAY": return Color.GRAY;
            case "GREEN": return Color.GREEN;
            case "LIME": return Color.LIME;
            case "MAROON": return Color.MAROON;
            case "NAVY": return Color.NAVY;
            case "OLIVE": return Color.OLIVE;
            case "ORANGE": return Color.ORANGE;
            case "PURPLE": return Color.PURPLE;
            case "RED": return Color.RED;
            case "SILVER": return Color.SILVER;
            case "TEAL": return Color.TEAL;
            case "WHITE": return Color.WHITE;
            case "YELLOW": return Color.YELLOW;
            default: return Color.WHITE;
        }
    }
}