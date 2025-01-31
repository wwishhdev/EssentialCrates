package com.wish.essentialcrates.models;

import lombok.Data;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import com.wish.essentialcrates.utils.ConfigUtil;

import java.util.ArrayList;
import java.util.List;

@Data
public class Crate {
    private final String id;
    private final String displayName;
    private final ItemStack keyItem;
    private final List<Reward> rewards;

    public Crate(String id, ConfigurationSection section) {
        this.id = id;
        this.displayName = ConfigUtil.color(section.getString("display-name"));
        this.rewards = new ArrayList<>();

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
}
