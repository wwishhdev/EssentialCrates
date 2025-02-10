package com.wish.essentialcrates.listeners;

import com.wish.essentialcrates.EssentialCrates;
import com.wish.essentialcrates.managers.CooldownManager;
import com.wish.essentialcrates.models.Crate;
import com.wish.essentialcrates.models.Reward;
import com.wish.essentialcrates.utils.ConfigUtil;
import com.wish.essentialcrates.utils.DebugUtil;
import com.wish.essentialcrates.utils.EffectUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

public class CrateListener implements Listener {
    private final EssentialCrates plugin;
    private final Random random;

    public CrateListener(EssentialCrates plugin) {
        this.plugin = plugin;
        this.random = new Random();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCrateInteract(PlayerInteractEvent event) {
        long startTime = System.nanoTime();

        // Solo procesar el evento si es un click derecho en un bloque
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK ||
                event.getClickedBlock() == null ||
                event.getClickedBlock().getType() != Material.CHEST) {
            return;
        }

        Block block = event.getClickedBlock();
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        DebugUtil.debug("Jugador " + player.getName() + " interactuó con un cofre en " +
                block.getLocation().toString());

        // Usar el caché para verificar la ubicación
        Optional<Crate> optionalCrate = plugin.getCrateManager().getCrateAtLocation(block.getLocation());
        if (!optionalCrate.isPresent()) {
            DebugUtil.debug("No se encontró crate en la ubicación " + block.getLocation());
            return;
        }

        event.setCancelled(true);
        Crate crate = optionalCrate.get();
        DebugUtil.debug("Crate encontrada: " + crate.getId());

        // Verificar cooldown
        if (CooldownManager.isOnCooldown(player, "crate_open")) {
            DebugUtil.debug("Jugador " + player.getName() + " en cooldown para " + crate.getId());
            player.sendMessage(ConfigUtil.getMessage("cooldown",
                    "%time%", CooldownManager.getRemainingTime(player, "crate_open")));
            return;
        }

        // Verificar llave
        if (item == null || !isValidKey(item, crate)) {
            DebugUtil.debug("Llave inválida para " + crate.getId() + " usada por " + player.getName());
            player.sendMessage(ConfigUtil.getMessage("no-key"));
            return;
        }

        DebugUtil.performance("Procesamiento de interacción con crate", startTime);

        // Procesar la apertura de la crate
        processReward(player, crate, item);
    }

    private void processReward(Player player, Crate crate, ItemStack item) {
        // Consumir la llave de manera segura
        if (!consumeKey(player, item)) {
            return;
        }

        // Dar recompensa
        giveReward(player, crate);
    }

    private boolean consumeKey(Player player, ItemStack item) {
        // Verificar que el item sigue siendo válido
        if (item.getAmount() <= 0) {
            return false;
        }

        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().setItem(player.getInventory().getHeldItemSlot(), null);
        }
        return true;
    }

    @EventHandler
    public void onCratePreview(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (!event.getPlayer().isSneaking()) return;

        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.CHEST) return;

        Player player = event.getPlayer();
        Optional<Crate> optionalCrate = plugin.getCrateManager().getCrateAtLocation(block.getLocation());

        if (!optionalCrate.isPresent()) return;

        event.setCancelled(true);
        Crate crate = optionalCrate.get();
        openPreviewMenu(player, crate);
    }

    private void openPreviewMenu(Player player, Crate crate) {
        int size = (int) Math.ceil(crate.getRewards().size() / 9.0) * 9;
        size = Math.min(54, Math.max(27, size)); // Mínimo 3 filas, máximo 6

        Inventory inventory = Bukkit.createInventory(null, size,
                ConfigUtil.color(crate.getDisplayName() + " &8- &7Preview"));

        // Información de la crate
        ItemStack info = new ItemStack(Material.PAPER);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.setDisplayName(ConfigUtil.color("&b&lInformación"));
        List<String> infoLore = new ArrayList<>();
        infoLore.add(ConfigUtil.color("&7Nombre: &f" + crate.getDisplayName()));
        infoLore.add(ConfigUtil.color("&7Recompensas: &f" + crate.getRewards().size()));
        infoLore.add("");
        infoLore.add(ConfigUtil.color("&7Shift + Click Derecho para preview"));
        infoLore.add(ConfigUtil.color("&7Click Derecho con llave para abrir"));
        infoMeta.setLore(infoLore);
        info.setItemMeta(infoMeta);
        inventory.setItem(size - 5, info);

        // Mostrar la llave
        inventory.setItem(size - 4, crate.getKeyItem());

        // Agregar recompensas
        int slot = 0;
        for (Reward reward : crate.getRewards()) {
            ItemStack display = reward.getDisplayItem().clone();
            ItemMeta meta = display.getItemMeta();
            List<String> lore = meta.getLore() != null ? meta.getLore() : new ArrayList<>();

            // Agregar información adicional
            lore.add("");
            lore.add(ConfigUtil.color("&7Probabilidad: &e" + reward.getChance() + "%"));
            lore.add(ConfigUtil.color("&7Comandos:"));
            for (String command : reward.getCommands()) {
                lore.add(ConfigUtil.color("&8- &7" + command));
            }

            meta.setLore(lore);
            display.setItemMeta(meta);
            inventory.setItem(slot++, display);
        }

        // Rellenar espacios vacíos
        ItemStack filler = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 15);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.setDisplayName(" ");
        filler.setItemMeta(fillerMeta);

        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, filler);
            }
        }

        player.openInventory(inventory);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().contains("Preview")) {
            event.setCancelled(true);
        }
    }

    private boolean isValidKey(ItemStack item, Crate crate) {
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return false;

        ItemStack keyItem = crate.getKeyItem();
        return item.getType() == keyItem.getType() &&
                item.getItemMeta().getDisplayName().equals(keyItem.getItemMeta().getDisplayName()) &&
                item.getItemMeta().getLore().equals(keyItem.getItemMeta().getLore());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCrateBreak(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_BLOCK ||
                event.getClickedBlock() == null ||
                event.getClickedBlock().getType() != Material.CHEST) {
            return;
        }

        Block block = event.getClickedBlock();
        Player player = event.getPlayer();
        Location location = block.getLocation();

        // Verificar si es una crate
        Optional<Crate> optionalCrate = plugin.getCrateManager().getCrateAtLocation(location);
        if (!optionalCrate.isPresent()) {
            return;
        }

        event.setCancelled(true);

        // Si no tiene permiso, cancelar
        if (!plugin.getPermissionManager().hasPermission(player, "essentialcrates.admin")) {
            player.sendMessage(ConfigUtil.getMessage("no-permission"));
            return;
        }

        // Si no está en shift, mostrar mensaje de ayuda
        if (!player.isSneaking()) {
            player.sendMessage(ConfigUtil.color("&e⚠ &cPara remover esta crate, usa SHIFT + Click Izquierdo"));
            return;
        }

        // Remover la crate de esta ubicación
        Crate crate = optionalCrate.get();

        // Remover el holograma
        plugin.getHologramManager().removeHologram(location);

        // Remover la ubicación del almacenamiento
        if (plugin.getConfig().getString("settings.storage.type", "YAML").equalsIgnoreCase("MYSQL")) {
            removeCrateLocationMySQL(location);
        } else {
            removeCrateLocationYAML(location);
        }

        // Remover del caché y del mapa de ubicaciones
        plugin.getCacheManager().invalidateLocation(location);
        plugin.getCrateManager().getCrateLocations().remove(location);

        // Convertir el cofre en un cofre normal
        block.setType(Material.AIR);
        block.setType(Material.CHEST);

        player.sendMessage(ConfigUtil.color("&aHas removido la crate &e" + crate.getDisplayName() + " &ade esta ubicación."));
        DebugUtil.debug("Crate removida en " + location + " por " + player.getName());
    }

    private void removeCrateLocationMySQL(Location location) {
        try (Connection conn = plugin.getDataManager().getHikari().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "DELETE FROM crate_locations WHERE world = ? AND x = ? AND y = ? AND z = ?")) {
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

    private void removeCrateLocationYAML(Location location) {
        String key = String.format("%s,%d,%d,%d",
                location.getWorld().getName(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ());
        plugin.getDataManager().getDataConfig().set("locations." + key, null);
        plugin.getDataManager().saveData();
    }

    private void giveReward(Player player, Crate crate) {
        List<Reward> rewards = crate.getRewards();
        double totalChance = rewards.stream().mapToDouble(Reward::getChance).sum();
        double randomValue = random.nextDouble() * totalChance;
        final double[] currentSum = {0};

        // Efecto al abrir la crate
        EffectUtil.playCrateOpenEffect(player.getLocation());

        // Retrasar la entrega de la recompensa para crear suspense
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Reward reward : rewards) {
                    currentSum[0] += reward.getChance();
                    if (randomValue <= currentSum[0]) {
                        // Ejecutar comandos de recompensa
                        for (String command : reward.getCommands()) {
                            String finalCommand = command.replace("%player%", player.getName());
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand);
                        }

                        // Mostrar mensaje de recompensa
                        player.sendMessage(ConfigUtil.getMessage("reward-received",
                                "%crate%", crate.getDisplayName(),
                                "%reward%", reward.getDisplayItem().getItemMeta().getDisplayName()));

                        // Efectos visuales/sonoros basados en la rareza (probabilidad < 10%)
                        boolean isRare = reward.getChance() < 10;
                        EffectUtil.playRewardEffect(player, isRare);

                        break;
                    }
                }
            }
        }.runTaskLater(plugin, 20L); // 1 segundo de retraso
    }
}