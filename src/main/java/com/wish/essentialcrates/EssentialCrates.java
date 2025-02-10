package com.wish.essentialcrates;

import com.wish.essentialcrates.commands.CrateCommand;
import com.wish.essentialcrates.listeners.CrateListener;
import com.wish.essentialcrates.managers.*;
import com.wish.essentialcrates.utils.DebugUtil;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.plugin.java.JavaPlugin;

public class EssentialCrates extends JavaPlugin {

    @Getter
    private static EssentialCrates instance;
    @Getter @Setter
    private DataManager dataManager;
    @Getter @Setter
    private CrateManager crateManager;
    @Getter
    private HologramManager hologramManager;
    @Getter
    private CacheManager cacheManager;
    @Getter
    private PermissionManager permissionManager;
    @Getter
    private CooldownManager cooldownManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        // Banner de inicio
        getLogger().info("§b==========================================");
        getLogger().info("§b          EssentialCrates v" + getDescription().getVersion());
        getLogger().info("§b==========================================");

        // Verificar dependencias
        if (getServer().getPluginManager().getPlugin("DecentHolograms") != null) {
            getLogger().info("§a✔ DecentHolograms encontrado y hooked correctamente");
        } else {
            getLogger().warning("§c✘ DecentHolograms no encontrado - Los hologramas estarán deshabilitados");
        }

        // Inicializar el modo debug según la configuración
        if (getConfig().getBoolean("settings.debug.enabled", false)) {
            DebugUtil.setGlobalDebug(true);
            getLogger().info("§a✔ Modo debug activado globalmente desde la configuración");
        }

        // Inicializar managers
        getLogger().info("§7Inicializando managers...");

        try {
            this.dataManager = new DataManager(this);
            String storageType = getConfig().getString("settings.storage.type", "YAML");
            if (storageType.equalsIgnoreCase("MYSQL")) {
                if (dataManager.getHikari() != null) {
                    getLogger().info("§a✔ Conexión MySQL establecida correctamente");
                    getLogger().info("§7  ├ Host: " + getConfig().getString("settings.storage.mysql.host"));
                    getLogger().info("§7  ├ Database: " + getConfig().getString("settings.storage.mysql.database"));
                    getLogger().info("§7  └ Pool: EssentialCrates-Pool");
                }
            } else {
                getLogger().info("§a✔ Almacenamiento YAML inicializado");
            }

            this.cacheManager = new CacheManager(this);
            getLogger().info("§a✔ Sistema de caché inicializado");

            this.hologramManager = new HologramManager(this);
            if (hologramManager.isEnabled()) {
                getLogger().info("§a✔ Sistema de hologramas inicializado");
            }

            this.crateManager = new CrateManager(this, dataManager);
            int cratesLoaded = crateManager.getCrates().size();
            int locationsLoaded = crateManager.getCrateLocations().size();
            getLogger().info("§a✔ Sistema de crates inicializado");
            getLogger().info("§7  ├ Crates cargadas: " + cratesLoaded);
            getLogger().info("§7  └ Ubicaciones: " + locationsLoaded);

            this.permissionManager = new PermissionManager(this);
            getLogger().info("§a✔ Sistema de permisos inicializado");

        } catch (Exception e) {
            getLogger().severe("§c✘ Error durante la inicialización:");
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Registrar comandos
        CrateCommand crateCommand = new CrateCommand(this);
        getCommand("crate").setExecutor(crateCommand);
        getCommand("crate").setTabCompleter(crateCommand);
        getLogger().info("§a✔ Comandos registrados");

        // Registrar listeners
        getServer().getPluginManager().registerEvents(new CrateListener(this), this);
        getLogger().info("§a✔ Eventos registrados");

        getLogger().info("§b==========================================");
        getLogger().info("§a✔ EssentialCrates ha sido habilitado correctamente!");
        getLogger().info("§b==========================================");
    }

    @Override
    public void onDisable() {
        if (cacheManager != null) {
            cacheManager.invalidateAll();
        }
        if (hologramManager != null) {
            hologramManager.removeAllHolograms();
        }
        if (dataManager != null) {
            dataManager.close();
        }
        CooldownManager.clearAllCooldowns();
        if (permissionManager != null) {
            permissionManager.clearCache();
        }

        getLogger().info("EssentialCrates ha sido deshabilitado correctamente!");
    }
}