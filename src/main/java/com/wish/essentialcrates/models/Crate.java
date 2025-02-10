package com.wish.essentialcrates.models;

import lombok.Data;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import com.wish.essentialcrates.utils.ConfigUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Data
public class Crate {
    private final String id;
    private final String displayName;
    private final ItemStack keyItem;
    private final List<Reward> rewards;
    private final HologramConfig hologramConfig;
    private final EffectsConfig effectsConfig;

    public Crate(String id, ConfigurationSection section) {
        this.id = id;
        this.displayName = ConfigUtil.color(section.getString("display-name"));
        this.rewards = new ArrayList<>();
        this.hologramConfig = new HologramConfig(section.getConfigurationSection("hologram"));
        this.effectsConfig = new EffectsConfig(section.getConfigurationSection("effects"));

        // Configurar el item de la llave
        Material keyMaterial = Material.valueOf(section.getString("key-item", "TRIPWIRE_HOOK"));
        this.keyItem = new ItemStack(keyMaterial);
        ItemMeta meta = keyItem.getItemMeta();
        meta.setDisplayName(ConfigUtil.color(section.getString("key-name")));

        List<String> lore = new ArrayList<>();
        for (String line : section.getStringList("key-lore")) {
            lore.add(ConfigUtil.color(line));
        }
        meta.setLore(lore);
        keyItem.setItemMeta(meta);

        // Cargar recompensas
        ConfigurationSection rewardsSection = section.getConfigurationSection("rewards");
        if (rewardsSection != null) {
            for (String key : rewardsSection.getKeys(false)) {
                ConfigurationSection rewardSection = rewardsSection.getConfigurationSection(key);
                rewards.add(new Reward(rewardSection));
            }
        }
    }

    @Data
    public static class HologramConfig {
        private final String titleFormat;
        private final double titleOffset;
        private final boolean rewardsEnabled;
        private final String rewardsHeader;
        private final String rewardsFormat;
        private final String rareFormat;
        private final double rareThreshold;
        private final int maxShown;
        private final double rewardsOffset;
        private final boolean footerEnabled;
        private final List<String> footerLines;
        private final double footerOffset;

        public HologramConfig(ConfigurationSection section) {
            if (section == null) {
                // Valores por defecto
                this.titleFormat = "&b&l%crate%";
                this.titleOffset = 0.0;
                this.rewardsEnabled = true;
                this.rewardsHeader = "&7Recompensas disponibles:";
                this.rewardsFormat = "&f- &e%reward% &7(%chance%%)";
                this.rareFormat = "&f- &6&l%reward% &7(%chance%%)";
                this.rareThreshold = 10.0;
                this.maxShown = 3;
                this.rewardsOffset = 0.3;
                this.footerEnabled = true;
                this.footerLines = Arrays.asList("", "&eClick derecho con llave", "&7Shift + Click para preview");
                this.footerOffset = 0.3;
                return;
            }

            ConfigurationSection title = section.getConfigurationSection("title");
            this.titleFormat = title.getString("text", "&b&l%crate%");
            this.titleOffset = title.getDouble("offset", 0.0);

            ConfigurationSection rewards = section.getConfigurationSection("rewards");
            this.rewardsEnabled = rewards.getBoolean("enabled", true);
            this.rewardsHeader = rewards.getString("header", "&7Recompensas disponibles:");
            this.rewardsFormat = rewards.getString("format", "&f- &e%reward% &7(%chance%%)");
            this.rareFormat = rewards.getString("rare-format", "&f- &6&l%reward% &7(%chance%%)");
            this.rareThreshold = rewards.getDouble("rare-threshold", 10.0);
            this.maxShown = rewards.getInt("max-shown", 3);
            this.rewardsOffset = rewards.getDouble("offset", 0.3);

            ConfigurationSection footer = section.getConfigurationSection("footer");
            this.footerEnabled = footer.getBoolean("enabled", true);
            this.footerLines = footer.getStringList("lines");
            this.footerOffset = footer.getDouble("offset", 0.3);
        }
    }

    @Data
    public static class EffectsConfig {
        private final SoundConfig sounds;
        private final ParticleConfig particles;
        private final FireworkConfig firework;

        public EffectsConfig(ConfigurationSection section) {
            if (section == null) {
                this.sounds = new SoundConfig(null);
                this.particles = new ParticleConfig(null);
                this.firework = new FireworkConfig(null);
                return;
            }

            this.sounds = new SoundConfig(section.getConfigurationSection("sounds"));
            this.particles = new ParticleConfig(section.getConfigurationSection("particles"));
            this.firework = new FireworkConfig(section.getConfigurationSection("firework"));
        }
    }

    @Data
    public static class SoundConfig {
        private final boolean enabled;
        private final List<String> openSounds;
        private final List<String> rareSounds;
        private final List<String> normalSounds;

        public SoundConfig(ConfigurationSection section) {
            if (section == null) {
                this.enabled = true;
                this.openSounds = Collections.singletonList("LEVEL_UP:1.0:1.0");
                this.rareSounds = Arrays.asList("ENDERDRAGON_GROWL:0.5:1.0", "LEVEL_UP:1.0:2.0");
                this.normalSounds = Collections.singletonList("ORB_PICKUP:1.0:1.0");
                return;
            }

            this.enabled = section.getBoolean("enabled", true);
            this.openSounds = section.getStringList("open");
            this.rareSounds = section.getStringList("rare");
            this.normalSounds = section.getStringList("normal");
        }
    }

    @Data
    public static class ParticleConfig {
        private final boolean enabled;
        private final int amount;
        private final List<String> openEffects;
        private final List<String> rareEffects;
        private final List<String> normalEffects;

        public ParticleConfig(ConfigurationSection section) {
            if (section == null) {
                this.enabled = true;
                this.amount = 20;
                this.openEffects = Collections.singletonList("SPIRAL:WITCH_MAGIC:1.5:2.0");
                this.rareEffects = Collections.singletonList("CIRCLE:FLAME:1.5:2");
                this.normalEffects = Collections.singletonList("CIRCLE:HAPPY_VILLAGER:1.5:2");
                return;
            }

            this.enabled = section.getBoolean("enabled", true);
            this.amount = section.getInt("amount", 20);
            this.openEffects = section.getStringList("open");
            this.rareEffects = section.getStringList("rare");
            this.normalEffects = section.getStringList("normal");
        }
    }

    @Data
    public static class FireworkConfig {
        private final boolean enabled;
        private final int power;
        private final String type;
        private final List<String> colors;
        private final List<String> fade;
        private final boolean trail;
        private final boolean flicker;

        public FireworkConfig(ConfigurationSection section) {
            if (section == null) {
                this.enabled = true;
                this.power = 1;
                this.type = "BALL_LARGE";
                this.colors = Arrays.asList("AQUA", "PURPLE");
                this.fade = Collections.singletonList("WHITE");
                this.trail = true;
                this.flicker = true;
                return;
            }

            this.enabled = section.getBoolean("enabled", true);
            this.power = section.getInt("power", 1);
            this.type = section.getString("type", "BALL_LARGE");
            this.colors = section.getStringList("colors");
            this.fade = section.getStringList("fade");
            this.trail = section.getBoolean("trail", true);
            this.flicker = section.getBoolean("flicker", true);
        }
    }
}