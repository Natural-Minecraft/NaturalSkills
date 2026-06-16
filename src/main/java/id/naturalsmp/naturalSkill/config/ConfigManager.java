package id.naturalsmp.naturalSkill.config;

import id.naturalsmp.naturalSkill.NaturalSkill;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class ConfigManager {

    private final NaturalSkill plugin;
    private FileConfiguration config = null;
    private File configFile = null;
    private FileConfiguration messages = null;
    private File messagesFile = null;

    public ConfigManager(NaturalSkill plugin) {
        this.plugin = plugin;
        saveDefaultConfig();
        saveDefaultMessages();
    }

    public void reloadConfigs() {
        if (configFile == null) {
            configFile = new File(plugin.getDataFolder(), "config.yml");
        }
        config = YamlConfiguration.loadConfiguration(configFile);

        // Look for defaults in the jar
        InputStream defConfigStream = plugin.getResource("config.yml");
        if (defConfigStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defConfigStream, StandardCharsets.UTF_8));
            config.setDefaults(defConfig);
        }

        if (messagesFile == null) {
            messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        }
        messages = YamlConfiguration.loadConfiguration(messagesFile);

        InputStream defMessagesStream = plugin.getResource("messages.yml");
        if (defMessagesStream != null) {
            YamlConfiguration defMessages = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defMessagesStream, StandardCharsets.UTF_8));
            messages.setDefaults(defMessages);
        }
    }

    public FileConfiguration getConfig() {
        if (config == null) {
            reloadConfigs();
        }
        return config;
    }

    public FileConfiguration getMessages() {
        if (messages == null) {
            reloadConfigs();
        }
        return messages;
    }

    public void saveConfig() {
        if (config == null || configFile == null) {
            return;
        }
        try {
            getConfig().save(configFile);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Could not save config to " + configFile, ex);
        }
    }

    public void saveMessages() {
        if (messages == null || messagesFile == null) {
            return;
        }
        try {
            getMessages().save(messagesFile);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Could not save messages to " + messagesFile, ex);
        }
    }

    private void saveDefaultConfig() {
        if (configFile == null) {
            configFile = new File(plugin.getDataFolder(), "config.yml");
        }
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
    }

    private void saveDefaultMessages() {
        if (messagesFile == null) {
            messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        }
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
    }

    /**
     * Get message formatted with colors and prefix.
     */
    public String getMessage(String path) {
        String msg = getMessages().getString(path);
        if (msg == null) {
            return ChatColor.RED + "Missing message: " + path;
        }
        String prefix = getMessages().getString("prefix", "&a&l[NaturalSkill] &r");
        return color(msg.replace("%prefix%", prefix));
    }

    /**
     * Translate Alternate Color Codes (& to ChatColor)
     */
    public static String color(String text) {
        if (text == null) return "";
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    /**
     * Translate colors in a list of strings
     */
    public static List<String> color(List<String> textList) {
        if (textList == null) return List.of();
        return textList.stream().map(ConfigManager::color).collect(Collectors.toList());
    }
}
