package id.naturalsmp.naturalSkill.data;

import id.naturalsmp.naturalSkill.NaturalSkill;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class PlayerManager {

    private final NaturalSkill plugin;
    private final Map<UUID, PlayerData> dataCache;
    private final File dataFolder;

    public PlayerManager(NaturalSkill plugin) {
        this.plugin = plugin;
        this.dataCache = new HashMap<>();
        this.dataFolder = new File(plugin.getDataFolder(), "playerdata");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }

    /**
     * Get player data from cache, or load it if not cached.
     */
    public PlayerData getPlayerData(UUID uuid) {
        if (dataCache.containsKey(uuid)) {
            return dataCache.get(uuid);
        }
        return loadPlayerData(uuid);
    }

    /**
     * Load player data from file and cache it.
     */
    public PlayerData loadPlayerData(UUID uuid) {
        File file = new File(dataFolder, uuid.toString() + ".yml");
        if (!file.exists()) {
            PlayerData data = new PlayerData(uuid);
            dataCache.put(uuid, data);
            return data;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        int points = config.getInt("points", 0);
        List<String> unlockedList = config.getStringList("unlocked_skills");
        Set<String> unlockedSet = new HashSet<>(unlockedList);

        PlayerData data = new PlayerData(uuid, points, unlockedSet);
        dataCache.put(uuid, data);
        return data;
    }

    /**
     * Save player data to file.
     */
    public void savePlayerData(UUID uuid) {
        PlayerData data = dataCache.get(uuid);
        if (data == null) return;

        File file = new File(dataFolder, uuid.toString() + ".yml");
        FileConfiguration config = new YamlConfiguration();

        config.set("points", data.getPoints());
        config.set("unlocked_skills", new ArrayList<>(data.getUnlockedSkills()));

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save player data for UUID: " + uuid, e);
        }
    }

    /**
     * Unload player data from cache and save to file.
     */
    public void unloadPlayerData(UUID uuid) {
        if (dataCache.containsKey(uuid)) {
            savePlayerData(uuid);
            dataCache.remove(uuid);
        }
    }

    /**
     * Save all cached player data (useful on server shutdown).
     */
    public void saveAll() {
        for (UUID uuid : dataCache.keySet()) {
            savePlayerData(uuid);
        }
    }
}
