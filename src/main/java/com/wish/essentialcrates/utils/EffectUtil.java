package com.wish.essentialcrates.utils;

import com.wish.essentialcrates.models.Crate;
import org.bukkit.*;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;
import com.wish.essentialcrates.EssentialCrates;

import java.util.List;

public class EffectUtil {
    private static final EssentialCrates plugin = EssentialCrates.getInstance();

    // Método auxiliar para convertir String a Sound
    private static Sound getSound(String soundName) {
        try {
            return Sound.valueOf(soundName.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Sonido inválido: " + soundName + ". Usando sonido por defecto.");
            return Sound.NOTE_PLING;
        }
    }

    public static void playCrateOpenEffect(Location location, Crate crate) {
        Crate.EffectsConfig effects = crate.getEffectsConfig();

        // Sonidos
        if (effects.getSounds().isEnabled()) {
            for (String soundConfig : effects.getSounds().getOpenSounds()) {
                String[] parts = soundConfig.split(":");
                if (parts.length >= 3) {
                    Sound sound = getSound(parts[0]);
                    location.getWorld().playSound(location, sound,
                            Float.parseFloat(parts[1]),
                            Float.parseFloat(parts[2]));
                }
            }
        }

        // Partículas
        if (effects.getParticles().isEnabled()) {
            for (String effectConfig : effects.getParticles().getOpenEffects()) {
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

    public static void playRewardEffect(Player player, Crate crate, boolean rare) {
        Crate.EffectsConfig effects = crate.getEffectsConfig();
        Location location = player.getLocation();

        // Sonidos
        if (effects.getSounds().isEnabled()) {
            List<String> sounds = rare ? effects.getSounds().getRareSounds() :
                    effects.getSounds().getNormalSounds();
            for (String soundConfig : sounds) {
                String[] parts = soundConfig.split(":");
                if (parts.length >= 3) {
                    Sound sound = getSound(parts[0]);
                    player.playSound(location, sound,
                            Float.parseFloat(parts[1]),
                            Float.parseFloat(parts[2]));
                }
            }
        }

        // Partículas
        if (effects.getParticles().isEnabled()) {
            List<String> particleEffects = rare ? effects.getParticles().getRareEffects() :
                    effects.getParticles().getNormalEffects();
            for (String effectConfig : particleEffects) {
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

        // Fuegos artificiales
        if (rare && effects.getFirework().isEnabled()) {
            spawnFirework(location, effects.getFirework());
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

    private static void spawnFirework(Location location, Crate.FireworkConfig config) {
        Firework firework = location.getWorld().spawn(location, Firework.class);
        FireworkMeta meta = firework.getFireworkMeta();

        FireworkEffect.Builder builder = FireworkEffect.builder()
                .with(FireworkEffect.Type.valueOf(config.getType()));

        // Colores
        for (String colorName : config.getColors()) {
            builder.withColor(getColor(colorName));
        }

        // Colores de desvanecimiento
        for (String colorName : config.getFade()) {
            builder.withFade(getColor(colorName));
        }

        if (config.isTrail()) builder.withTrail();
        if (config.isFlicker()) builder.withFlicker();

        meta.addEffect(builder.build());
        meta.setPower(config.getPower());
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