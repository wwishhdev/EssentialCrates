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
public class Reward {
    private final double chance;
    private final List<String> commands;
    private final ItemStack displayItem;
    private final int slot;

    public Reward(ConfigurationSection section) {
        this.chance = section.getDouble("chance");
        this.commands = section.getStringList("commands");
        this.slot = section.getInt("slot", -1);

        // Configurar el item de display
        Material material = Material.valueOf(section.getString("display-item", "STONE"));
        this.displayItem = new ItemStack(material);
        ItemMeta meta = displayItem.getItemMeta();
        meta.setDisplayName(ConfigUtil.color(section.getString("display-name")));

        List<String> lore = new ArrayList<>();
        for (String line : section.getStringList("lore")) {
            lore.add(ConfigUtil.color(line));
        }
        meta.setLore(lore);
        displayItem.setItemMeta(meta);
    }
}