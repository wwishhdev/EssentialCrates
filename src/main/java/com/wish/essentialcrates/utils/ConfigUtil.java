package com.wish.essentialcrates.utils;

import lombok.experimental.UtilityClass;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import com.wish.essentialcrates.EssentialCrates;

@UtilityClass
public class ConfigUtil {

    public String getPrefix() {
        return color(getConfig().getString("messages.prefix", "&8[&bEssentialCrates&8] "));
    }

    public String getMessage(String path) {
        return color(getConfig().getString("messages." + path, "&cMensaje no encontrado: " + path));
    }

    public String getMessage(String path, Object... args) {
        String message = getMessage(path);
        for (int i = 0; i < args.length; i += 2) {
            if (i + 1 < args.length) {
                message = message.replace(String.valueOf(args[i]), String.valueOf(args[i + 1]));
            }
        }
        return message;
    }

    public String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    private FileConfiguration getConfig() {
        return EssentialCrates.getInstance().getConfig();
    }
}