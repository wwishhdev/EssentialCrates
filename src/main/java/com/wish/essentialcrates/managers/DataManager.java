package com.wish.essentialcrates.managers;

import com.wish.essentialcrates.EssentialCrates;
import com.wish.essentialcrates.utils.DebugUtil;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;

public class DataManager {
    private final EssentialCrates plugin;
    private final String storageType;
    @Getter
    private HikariDataSource hikari;
    private File dataFile;
    private FileConfiguration dataConfig;

    public DataManager(EssentialCrates plugin) {
        this.plugin = plugin;
        this.storageType = plugin.getConfig().getString("settings.storage.type", "YAML");

        if (storageType.equalsIgnoreCase("MYSQL")) {
            setupMySQL();
        } else {
            setupYAML();
        }
    }

    private void setupMySQL() {
        long startTime = System.nanoTime();
        FileConfiguration config = plugin.getConfig();
        String host = config.getString("settings.storage.mysql.host", "localhost");
        int port = config.getInt("settings.storage.mysql.port", 3306);
        String database = config.getString("settings.storage.mysql.database", "essentialcrates");
        String username = config.getString("settings.storage.mysql.username", "root");
        String password = config.getString("settings.storage.mysql.password", "");

        plugin.getLogger().info("§7Conectando a MySQL...");
        plugin.getLogger().info("§7  ├ Host: " + host);
        plugin.getLogger().info("§7  ├ Puerto: " + port);
        plugin.getLogger().info("§7  └ Database: " + database);

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database);
        hikariConfig.setUsername(username);
        hikariConfig.setPassword(password);
        hikariConfig.setPoolName("EssentialCrates-Pool");

        try {
            hikari = new HikariDataSource(hikariConfig);
            createTables();
            DebugUtil.debug("Conexión MySQL establecida - Host: " + host + ", DB: " + database);
            DebugUtil.performance("Configuración MySQL", startTime);
        } catch (Exception e) {
            plugin.getLogger().severe("§c✘ Error al conectar con MySQL!");
            plugin.getLogger().severe("§c  Causa: " + e.getMessage());
            plugin.getLogger().warning("§e⚠ Cambiando a almacenamiento YAML...");
            setupYAML();
        }
    }

    private void setupYAML() {
        dataFile = new File(plugin.getDataFolder(), "data.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.getParentFile().mkdirs();
                dataFile.createNewFile();
                // Crear configuración básica
                dataConfig = new YamlConfiguration();
                dataConfig.createSection("locations");
                dataConfig.save(dataFile);
            } catch (IOException e) {
                plugin.getLogger().severe("Error al crear data.yml!");
                e.printStackTrace();
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    private void createTables() {
        try (Connection connection = hikari.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "CREATE TABLE IF NOT EXISTS crate_locations (" +
                             "id INT AUTO_INCREMENT PRIMARY KEY, " +
                             "world VARCHAR(64) NOT NULL, " +
                             "x INT NOT NULL, " +
                             "y INT NOT NULL, " +
                             "z INT NOT NULL, " +
                             "crate_id VARCHAR(64) NOT NULL" +
                             ")"
             )) {
            statement.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error al crear las tablas!", e);
        }
    }

    public void saveData() {
        if (storageType.equalsIgnoreCase("YAML")) {
            long startTime = System.nanoTime();
            try {
                dataConfig.save(dataFile);
                DebugUtil.performance("Guardado de datos YAML", startTime);
            } catch (Exception e) {
                DebugUtil.error("Error al guardar data.yml", e);
            }
        }
    }

    public void close() {
        if (hikari != null && !hikari.isClosed()) {
            hikari.close();
        }
        saveData();
    }

    public FileConfiguration getDataConfig() {
        return dataConfig;
    }
}