package id.naturalsmp.naturalSkill;

import id.naturalsmp.naturalSkill.commands.SkillCommand;
import id.naturalsmp.naturalSkill.commands.SkillTabCompleter;
import id.naturalsmp.naturalSkill.config.ConfigManager;
import id.naturalsmp.naturalSkill.data.PlayerManager;
import id.naturalsmp.naturalSkill.effects.EffectEngine;
import id.naturalsmp.naturalSkill.hooks.HookManager;
import id.naturalsmp.naturalSkill.listeners.GuiListener;
import id.naturalsmp.naturalSkill.listeners.PlayerListener;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class NaturalSkill extends JavaPlugin {

    private ConfigManager configManager;
    private HookManager hookManager;
    private PlayerManager playerManager;
    private EffectEngine effectEngine;
    private id.naturalsmp.naturalSkill.data.LeaderboardManager leaderboardManager;

    private final java.util.Map<String, String> skillCategoryMap = new java.util.HashMap<>();
    private final java.util.Map<String, Integer> skillCostMap = new java.util.HashMap<>();

    @Override
    public void onEnable() {
        // 1. Initialize Configuration
        this.configManager = new ConfigManager(this);
        loadSkillConfigMapping();

        // 2. Initialize HookManager (Vault, MMOItems, mcMMO)
        this.hookManager = new HookManager(this);

        // 3. Initialize PlayerManager & Data
        this.playerManager = new PlayerManager(this);

        // 3.5 Initialize LeaderboardManager
        this.leaderboardManager = new id.naturalsmp.naturalSkill.data.LeaderboardManager(this);

        // 4. Initialize Effects Engine
        this.effectEngine = new EffectEngine(this);

        // 5. Register Listeners
        Bukkit.getPluginManager().registerEvents(new PlayerListener(this), this);
        Bukkit.getPluginManager().registerEvents(new GuiListener(this), this);

        // 6. Register Commands and Tab Completers
        SkillCommand skillCommand = new SkillCommand(this);
        SkillTabCompleter tabCompleter = new SkillTabCompleter();

        registerCommand("skill", skillCommand, tabCompleter);
        registerCommand("nskill", skillCommand, tabCompleter);

        // 7. Support Hot-loading (in case reload/plugman used)
        for (Player p : Bukkit.getOnlinePlayers()) {
            playerManager.loadPlayerData(p.getUniqueId());
        }

        // 8. Register PlaceholderAPI Expansion
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new id.naturalsmp.naturalSkill.hooks.NaturalSkillExpansion(this).register();
            getLogger().info("PlaceholderAPI Expansion registered successfully!");
        }

        // 9. Start Leaderboard Updates
        Bukkit.getScheduler().runTaskLater(this, () -> {
            leaderboardManager.updateLeaderboardsAsync();
        }, 100L);

        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            leaderboardManager.updateLeaderboardsAsync();
        }, 6000L, 6000L); // every 5 minutes

        Bukkit.getConsoleSender().sendMessage(
                id.naturalsmp.naturalSkill.config.ConfigManager.color(
                    "\n&a===============\n" +
                    "&a _   _       _                  _     &e ____  _    _ _ _ \n" +
                    "&a| \ | | __ _| |_ _   _ _ __ __ _| |   &e/ ___|| | _(_) | |\n" +
                    "&a|  \| |/ _` | __| | | | '__/ _` | |   &e\___ \| |/ / | | |\n" +
                    "&a| |\  | (_| | |_| |_| | | | (_| | |    &e___) |   <| | | |\n" +
                    "&a|_| \_|\__,_|\__|\__,_|_|  \__,_|_|   &e|____/|_|\_\_|_|_|\n" +
                    "&f       >> &bNaturalSkills v" + getDescription().getVersion() + " Enabled! <<\n" +
                    "&a===============\n"
                )
        );
                )
        );
    }

    @Override
    public void onDisable() {
        // Save all cached player data on disable
        if (playerManager != null) {
            playerManager.saveAll();
        }
        getLogger().info("NaturalSkill has been disabled and player data saved.");
    }

    public void loadSkillConfigMapping() {
        skillCategoryMap.clear();
        skillCostMap.clear();
        org.bukkit.configuration.file.FileConfiguration cfg = configManager.getConfig();
        if (cfg.contains("categories")) {
            for (String categoryId : cfg.getConfigurationSection("categories").getKeys(false)) {
                String branchPath = "categories." + categoryId + ".branches";
                if (cfg.contains(branchPath)) {
                    for (String skillId : cfg.getConfigurationSection(branchPath).getKeys(false)) {
                        int cost = cfg.getInt(branchPath + "." + skillId + ".cost", 0);
                        skillCategoryMap.put(skillId.toLowerCase(), categoryId.toLowerCase());
                        skillCostMap.put(skillId.toLowerCase(), cost);
                    }
                }
            }
        }
    }

    private void registerCommand(String name, SkillCommand executor, SkillTabCompleter completer) {
        PluginCommand cmd = getCommand(name);
        if (cmd != null) {
            cmd.setExecutor(executor);
            cmd.setTabCompleter(completer);
        } else {
            getLogger().warning("Command '" + name + "' could not be registered! Check paper-plugin.yml or plugin.yml.");
        }
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public HookManager getHookManager() {
        return hookManager;
    }

    public PlayerManager getPlayerManager() {
        return playerManager;
    }

    public EffectEngine getEffectEngine() {
        return effectEngine;
    }

    public id.naturalsmp.naturalSkill.data.LeaderboardManager getLeaderboardManager() {
        return leaderboardManager;
    }

    public java.util.Map<String, String> getSkillCategoryMap() {
        return skillCategoryMap;
    }

    public java.util.Map<String, Integer> getSkillCostMap() {
        return skillCostMap;
    }
}
