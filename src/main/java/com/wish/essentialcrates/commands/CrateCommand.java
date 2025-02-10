package com.wish.essentialcrates.commands;

import com.wish.essentialcrates.EssentialCrates;
import com.wish.essentialcrates.managers.CrateManager;
import com.wish.essentialcrates.managers.DataManager;
import com.wish.essentialcrates.models.Crate;
import com.wish.essentialcrates.utils.ConfigUtil;
import com.wish.essentialcrates.utils.DebugUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.*;
import java.util.stream.Collectors;

public class CrateCommand implements CommandExecutor, TabCompleter {
    private final EssentialCrates plugin;

    public CrateCommand(EssentialCrates plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "help":
                sendHelp(sender);
                break;
            case "give":
                handleGive(sender, args);
                break;
            case "create":
                handleCreate(sender, args);
                break;
            case "delete":
                handleDelete(sender, args);
                break;
            case "keyall":
                handleKeyAll(sender, args);
                break;
            case "additem":
                handleAddItem(sender, args);
                break;
            case "debug":
                if (!sender.hasPermission("essentialcrates.admin")) {
                    sender.sendMessage(ConfigUtil.getMessage("no-permission"));
                    return true;
                }
                DebugUtil.toggleDebug(sender);
                break;
            case "reload":
                handleReload(sender);
                break;
            default:
                sendHelp(sender);
                break;
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        if (!sender.hasPermission("essentialcrates.admin")) {
            sender.sendMessage(ConfigUtil.getMessage("no-permission"));
            return;
        }

        sender.sendMessage(ConfigUtil.color("&8&m---------------------"));
        sender.sendMessage(ConfigUtil.color("&b&lEssentialCrates &7- &fAyuda"));
        sender.sendMessage(ConfigUtil.color("&8&m---------------------"));
        sender.sendMessage(ConfigUtil.color("&b/crate help &7- &fMuestra este mensaje"));
        sender.sendMessage(ConfigUtil.color("&b/crate give <jugador> <crate> [cantidad] &7- &fDa llaves"));
        sender.sendMessage(ConfigUtil.color("&b/crate create <nombre> &7- &fCrea una crate"));
        sender.sendMessage(ConfigUtil.color("&b/crate delete <nombre> &7- &fElimina una crate"));
        sender.sendMessage(ConfigUtil.color("&b/crate keyall <crate> [cantidad] &7- &fDa llaves a todos"));
        sender.sendMessage(ConfigUtil.color("&b/crate additem <crate> <probabilidad> &7- &fAgrega el item en mano"));
        sender.sendMessage(ConfigUtil.color("&b/crate reload &7- &fRecarga la configuración"));
        sender.sendMessage(ConfigUtil.color("&8&m---------------------"));
    }

    private void handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("essentialcrates.give")) {
            sender.sendMessage(ConfigUtil.getMessage("no-permission"));
            return;
        }

        if (args.length < 3) {
            sender.sendMessage(ConfigUtil.color("&cUso: /crate give <jugador> <crate> [cantidad]"));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ConfigUtil.getMessage("player-not-found"));
            return;
        }

        Optional<Crate> crateOptional = plugin.getCrateManager().getCrate(args[2]);
        if (crateOptional.isPresent()) {
            Crate crate = crateOptional.get();
            int amount = args.length > 3 ? Integer.parseInt(args[3]) : 1;
            if (amount <= 0) {
                sender.sendMessage(ConfigUtil.getMessage("invalid-amount"));
                return;
            }

            ItemStack keyItem = crate.getKeyItem().clone();
            keyItem.setAmount(amount);
            target.getInventory().addItem(keyItem);

            target.sendMessage(ConfigUtil.getMessage("received-key",
                    "%amount%", amount,
                    "%crate%", crate.getDisplayName()));

            if (sender != target) {
                sender.sendMessage(ConfigUtil.color("&aHas dado &e" + amount + " &allave(s) de " +
                        crate.getDisplayName() + " &aa &e" + target.getName()));
            }
        } else {
            sender.sendMessage(ConfigUtil.getMessage("invalid-crate"));
        }
    }

    private void handleAddItem(CommandSender sender, String[] args) {
        if (!sender.hasPermission("essentialcrates.additem")) {
            sender.sendMessage(ConfigUtil.getMessage("no-permission"));
            return;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(ConfigUtil.color("&cEste comando solo puede ser usado por jugadores."));
            return;
        }

        if (args.length < 3) {
            sender.sendMessage(ConfigUtil.color("&cUso: /crate additem <crate> <probabilidad>"));
            return;
        }

        Player player = (Player) sender;
        ItemStack item = player.getItemInHand();

        if (item == null || item.getType() == Material.AIR) {
            sender.sendMessage(ConfigUtil.color("&cDebes tener un item en la mano!"));
            return;
        }

        String crateId = args[1].toLowerCase();
        Optional<Crate> crateOptional = plugin.getCrateManager().getCrate(crateId);

        if (!crateOptional.isPresent()) {
            sender.sendMessage(ConfigUtil.getMessage("invalid-crate"));
            return;
        }

        double chance;
        try {
            chance = Double.parseDouble(args[2]);
            if (chance <= 0 || chance > 100) {
                sender.sendMessage(ConfigUtil.color("&cLa probabilidad debe estar entre 0 y 100!"));
                return;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(ConfigUtil.color("&cLa probabilidad debe ser un número válido!"));
            return;
        }

        // Guardar el item en la configuración
        FileConfiguration config = plugin.getConfig();
        String path = "crates." + crateId + ".rewards";

        int newIndex = 0;
        if (config.contains(path)) {
            newIndex = config.getConfigurationSection(path).getKeys(false).size();
        }

        path = path + "." + newIndex;
        config.set(path + ".chance", chance);
        config.set(path + ".display-item", item.getType().name());

        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta.hasDisplayName()) {
                config.set(path + ".display-name", meta.getDisplayName());
            }
            if (meta.hasLore()) {
                config.set(path + ".lore", meta.getLore());
            }
        }

        // Agregar comandos basados en el item
        List<String> commands = new ArrayList<>();
        commands.add("give %player% " + item.getType().name().toLowerCase() + " " + item.getAmount());
        config.set(path + ".commands", commands);

        // Guardar y recargar
        plugin.saveConfig();
        plugin.getCrateManager().loadCrates();
        plugin.getHologramManager().reloadAllHolograms();

        sender.sendMessage(ConfigUtil.color("&aItem agregado exitosamente a la crate &e" + crateId +
                " &acon probabilidad &e" + chance + "%"));

        DebugUtil.debug("Nuevo item agregado a " + crateId + " por " + player.getName() +
                " - Tipo: " + item.getType() + ", Probabilidad: " + chance + "%");
    }

    private void handleKeyAll(CommandSender sender, String[] args) {
        if (!sender.hasPermission("essentialcrates.keyall")) {
            sender.sendMessage(ConfigUtil.getMessage("no-permission"));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(ConfigUtil.color("&cUso: /crate keyall <crate> [cantidad]"));
            return;
        }

        Optional<Crate> crateOptional = plugin.getCrateManager().getCrate(args[1]);
        if (!crateOptional.isPresent()) {
            sender.sendMessage(ConfigUtil.getMessage("invalid-crate"));
            return;
        }

        Crate crate = crateOptional.get();
        int amount = args.length > 2 ? Integer.parseInt(args[2]) : 1;

        if (amount <= 0) {
            sender.sendMessage(ConfigUtil.getMessage("invalid-amount"));
            return;
        }

        int playerCount = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            ItemStack keyItem = crate.getKeyItem().clone();
            keyItem.setAmount(amount);
            player.getInventory().addItem(keyItem);
            player.sendMessage(ConfigUtil.getMessage("received-key",
                    "%amount%", amount,
                    "%crate%", crate.getDisplayName()));
            playerCount++;
        }

        sender.sendMessage(ConfigUtil.color("&aHas dado &e" + amount + " &allave(s) de " +
                crate.getDisplayName() + " &aa &e" + playerCount + " &ajugadores."));

        DebugUtil.debug("Llaves distribuidas a " + playerCount + " jugadores - Crate: " +
                crate.getId() + ", Cantidad: " + amount);
    }

    private void handleCreate(CommandSender sender, String[] args) {
        if (!sender.hasPermission("essentialcrates.create")) {
            sender.sendMessage(ConfigUtil.getMessage("no-permission"));
            return;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(ConfigUtil.color("&cEste comando solo puede ser usado por jugadores."));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(ConfigUtil.color("&cUso: /crate create <nombre>"));
            return;
        }

        Player player = (Player) sender;
        String crateId = args[1].toLowerCase();

        // Verificar si la crate ya existe
        if (plugin.getCrateManager().getCrate(crateId).isPresent()) {
            sender.sendMessage(ConfigUtil.getMessage("crate-exists"));
            return;
        }

        // Verificar que el jugador está mirando a un cofre
        Block targetBlock = player.getTargetBlock((Set<Material>) null, 5);
        if (targetBlock == null || targetBlock.getType() != Material.CHEST) {
            sender.sendMessage(ConfigUtil.color("&cDebes estar mirando a un cofre para crear una crate."));
            return;
        }

        // Crear la configuración básica de la crate
        FileConfiguration config = plugin.getConfig();
        String path = "crates." + crateId;

        config.set(path + ".display-name", "&b" + args[1]);
        config.set(path + ".key-item", "TRIPWIRE_HOOK");
        config.set(path + ".key-name", "&bLlave de " + args[1]);
        config.set(path + ".key-lore", Arrays.asList(
                "&7Click derecho en la crate",
                "&7para obtener recompensas!"
        ));

        // Crear una recompensa de ejemplo
        config.set(path + ".rewards.0.chance", 100);
        config.set(path + ".rewards.0.commands", Collections.singletonList("give %player% diamond 1"));
        config.set(path + ".rewards.0.display-item", "DIAMOND");
        config.set(path + ".rewards.0.display-name", "&bDiamante");
        config.set(path + ".rewards.0.lore", Collections.singletonList("&7Probabilidad: &e100%"));

        // Guardar la configuración
        plugin.saveConfig();

        // Registrar la ubicación de la crate
        Location crateLocation = targetBlock.getLocation();
        plugin.getCrateManager().saveCrateLocation(crateLocation, crateId);

        // Recargar la crate y crear el holograma
        plugin.reloadConfig();
        plugin.getCrateManager().loadCrates();

        // Crear el holograma inmediatamente
        plugin.getCrateManager().getCrate(crateId).ifPresent(crate ->
                plugin.getHologramManager().createHologram(crateLocation, crate));

        sender.sendMessage(ConfigUtil.getMessage("crate-created"));
        DebugUtil.debug("Crate creada: " + crateId + " en " + crateLocation);
    }

    private void handleDelete(CommandSender sender, String[] args) {
        if (!sender.hasPermission("essentialcrates.delete")) {
            sender.sendMessage(ConfigUtil.getMessage("no-permission"));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(ConfigUtil.color("&cUso: /crate delete <nombre>"));
            return;
        }

        String crateId = args[1].toLowerCase();

        // Verificar si la crate existe
        Optional<Crate> crateOptional = plugin.getCrateManager().getCrate(crateId);
        if (!crateOptional.isPresent()) {
            sender.sendMessage(ConfigUtil.getMessage("invalid-crate"));
            return;
        }

        // Eliminar hologramas primero
        Map<Location, String> locations = new HashMap<>(plugin.getCrateManager().getCrateLocations());
        for (Map.Entry<Location, String> entry : locations.entrySet()) {
            if (entry.getValue().equals(crateId)) {
                // Remover holograma
                plugin.getHologramManager().removeHologram(entry.getKey());

                // Remover del caché
                plugin.getCacheManager().invalidateLocation(entry.getKey());

                // Remover de la base de datos
                if (plugin.getConfig().getString("settings.storage.type", "YAML").equalsIgnoreCase("MYSQL")) {
                    deleteCrateLocationMySQL(entry.getKey());
                } else {
                    deleteCrateLocationYAML(entry.getKey());
                }

                // Remover del mapa de ubicaciones
                plugin.getCrateManager().getCrateLocations().remove(entry.getKey());

                // Convertir el cofre en un cofre normal
                entry.getKey().getBlock().setType(Material.AIR);
                entry.getKey().getBlock().setType(Material.CHEST);
            }
        }

        // Eliminar la crate de la configuración
        plugin.getConfig().set("crates." + crateId, null);
        plugin.saveConfig();

        // Invalidar caché de la crate
        plugin.getCacheManager().invalidateCrate(crateId);

        // Recargar configuración
        plugin.reloadConfig();
        plugin.getCrateManager().loadCrates();

        sender.sendMessage(ConfigUtil.getMessage("crate-deleted"));
        DebugUtil.debug("Crate eliminada: " + crateId);
    }

    private void deleteCrateLocationYAML(Location location) {
        String key = String.format("%s,%d,%d,%d",
                location.getWorld().getName(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ()
        );
        plugin.getDataManager().getDataConfig().set("locations." + key, null);
        plugin.getDataManager().saveData();
    }

    // Método auxiliar para eliminar ubicaciones de MySQL
    private void deleteCrateLocationMySQL(Location location) {
        try (Connection conn = plugin.getDataManager().getHikari().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "DELETE FROM crate_locations WHERE world = ? AND x = ? AND y = ? AND z = ?"
             )) {
            ps.setString(1, location.getWorld().getName());
            ps.setInt(2, location.getBlockX());
            ps.setInt(3, location.getBlockY());
            ps.setInt(4, location.getBlockZ());
            ps.executeUpdate();
        } catch (Exception e) {
            plugin.getLogger().severe("Error al eliminar ubicación de MySQL!");
            e.printStackTrace();
        }
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("essentialcrates.reload")) {
            sender.sendMessage(ConfigUtil.getMessage("no-permission"));
            return;
        }

        // Mensaje de advertencia
        sender.sendMessage(ConfigUtil.color("&e⚠ &cAlgunos cambios (como el tipo de base de datos) requieren un reinicio completo del servidor."));

        try {
            // Guardar estado actual del debug
            boolean wasDebugEnabled = plugin.getConfig().getBoolean("settings.debug.enabled", false);

            // 1. Limpiar recursos actuales
            plugin.getHologramManager().removeAllHolograms();
            plugin.getCacheManager().invalidateAll();
            plugin.getCooldownManager().clearAllCooldowns();
            plugin.getDataManager().close();

            // 2. Recargar configuración
            plugin.reloadConfig();

            // 3. Reinicializar managers en orden correcto
            plugin.setDataManager(new DataManager(plugin));
            plugin.setCrateManager(new CrateManager(plugin, plugin.getDataManager()));

            // 4. Recargar hologramas
            plugin.getHologramManager().reloadAllHolograms();

            sender.sendMessage(ConfigUtil.getMessage("config-reloaded"));

            // 5. Verificar y notificar el tipo de almacenamiento actual
            String storageType = plugin.getConfig().getString("settings.storage.type", "YAML");
            sender.sendMessage(ConfigUtil.color("&aAlmacenamiento actual: &f" + storageType));

            // Restaurar estado del debug después del reload
            if (wasDebugEnabled) {
                DebugUtil.setGlobalDebug(true);
                sender.sendMessage(ConfigUtil.color("&aDebug restaurado al estado: Activado"));
            }

            sender.sendMessage(ConfigUtil.getMessage("config-reloaded"));

        } catch (Exception e) {
            sender.sendMessage(ConfigUtil.color("&c¡Error al recargar! Verifica la consola para más detalles."));
            plugin.getLogger().severe("Error durante el reload: " + e.getMessage());
            e.printStackTrace();
        }
    }


    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            if (sender.hasPermission("essentialcrates.admin")) {
                completions.addAll(Arrays.asList("help", "give", "create", "delete", "reload", "keyall", "additem"));
            }
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("give") ||
                    args[0].equalsIgnoreCase("keyall") ||
                    args[0].equalsIgnoreCase("additem")) {
                completions.addAll(plugin.getCrateManager().getCrates().keySet());
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("give")) {
                completions.addAll(plugin.getCrateManager().getCrates().keySet());
            }
        }

        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .collect(Collectors.toList());
    }
}