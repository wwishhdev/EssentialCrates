package com.wish.essentialcrates;

import com.wish.essentialcrates.commands.CrateCommand;
import com.wish.essentialcrates.listeners.CrateListener;
import com.wish.essentialcrates.managers.*;
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

        // Inicializar managers
        this.dataManager = new DataManager(this);
        this.cacheManager = new CacheManager(this);
        this.hologramManager = new HologramManager(this);
        this.crateManager = new CrateManager(this, dataManager);
        this.permissionManager = new PermissionManager(this);

        // Registrar comandos
        CrateCommand crateCommand = new CrateCommand(this);
        getCommand("crate").setExecutor(crateCommand);
        getCommand("crate").setTabCompleter(crateCommand);

        // Registrar listeners
        getServer().getPluginManager().registerEvents(new CrateListener(this), this);

        getLogger().info("EssentialCrates ha sido habilitado correctamente!");
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