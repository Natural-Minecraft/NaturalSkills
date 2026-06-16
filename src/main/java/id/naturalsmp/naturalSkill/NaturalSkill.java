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

    @Override
    public void onEnable() {
        // 1. Initialize Configuration
        this.configManager = new ConfigManager(this);

        // 2. Initialize HookManager (Vault, MMOItems, mcMMO)
        this.hookManager = new HookManager(this);

        // 3. Initialize PlayerManager & Data
        this.playerManager = new PlayerManager(this);

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

        getLogger().info("NaturalSkill has been enabled successfully!");
    }

    @Override
    public void onDisable() {
        // Save all cached player data on disable
        if (playerManager != null) {
            playerManager.saveAll();
        }
        getLogger().info("NaturalSkill has been disabled and player data saved.");
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
}
