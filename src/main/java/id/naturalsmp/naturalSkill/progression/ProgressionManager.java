package id.naturalsmp.naturalSkill.progression;

import id.naturalsmp.naturalSkill.NaturalSkill;
import id.naturalsmp.naturalSkill.config.ConfigManager;
import id.naturalsmp.naturalSkill.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;

public class ProgressionManager {

    private final NaturalSkill plugin;

    // Unique UUIDs for each skill and talent attribute modifier to prevent overlaps
    private final Map<String, UUID> attributeUuids = new HashMap<>();

    public ProgressionManager(NaturalSkill plugin) {
        this.plugin = plugin;
        initializeUuids();
    }

    private void initializeUuids() {
        // Core skills UUIDs
        attributeUuids.put("intelligence", UUID.fromString("1f3d5c7b-9a8b-7c6d-5e4f-3a2b1c0d9e8f"));
        attributeUuids.put("strength", UUID.fromString("2f4d6c8b-0a9b-8c7d-6e5f-4a3b2c1d0e9f"));
        attributeUuids.put("agility", UUID.fromString("3f5d7c9b-1a0b-9c8d-7e6f-5a4b3c2d1e0f"));
        attributeUuids.put("psychology", UUID.fromString("4f6d8c0b-2a1b-0c9d-8e7f-6a5b4c3d2e1f"));
        attributeUuids.put("communication", UUID.fromString("5f7d9c1b-3a2b-1c0d-9e8f-7a6b5c4d3e2f"));

        // Bakat UUIDs
        attributeUuids.put("woodcutting", UUID.fromString("6f8d0c2b-4a3b-2c1d-0e9f-8a7b6c5d4e3f"));
        attributeUuids.put("mining", UUID.fromString("7f9d1c3b-5a4b-3c2d-1e0f-9a8b7c6d5e4f"));
        attributeUuids.put("swimming", UUID.fromString("8f0d2c4b-6a5b-4c3d-2e1f-0a9b8c7d6e5f"));
        attributeUuids.put("fishing", UUID.fromString("9f1d3c5b-7a6b-5c4d-3e2f-1a0b9c8d7e6f"));
        attributeUuids.put("farming", UUID.fromString("0f2d4c6b-8a7b-6c5d-4e3f-2a1b0c9d8e7f"));
        attributeUuids.put("arsitek", UUID.fromString("af3e5d7c-9b8a-4d6e-5f4a-3b2c1d0e9f88"));
        attributeUuids.put("teknik_mesin", UUID.fromString("bf4e6d8c-0b9a-4d7e-6f5a-4b3c2d1e0f9a"));
    }

    /**
     * Calculates the required XP for a given level.
     */
    public int getRequiredXp(int level) {
        FileConfiguration config = plugin.getConfigManager().getConfig();
        double base = config.getDouble("progression.xp-curve.base", 100.0);
        double multiplier = config.getDouble("progression.xp-curve.multiplier", 50.0);
        double exponent = config.getDouble("progression.xp-curve.exponent", 1.8);
        return (int) (base + Math.pow(level - 1, exponent) * multiplier);
    }

    /**
     * Add XP to a player's skill or talent.
     */
    public void addXp(Player player, String id, double amount, boolean isBakat) {
        if (amount <= 0) return;

        PlayerData data = plugin.getPlayerManager().getPlayerData(player.getUniqueId());
        if (data == null) return;

        String key = id.toLowerCase();
        int oldLevel = isBakat ? data.getBakatLevel(key) : data.getSkillLevel(key);
        double currentXp = isBakat ? data.getBakatXp(key) : data.getSkillXp(key);

        double newXp = currentXp + amount;
        int level = oldLevel;
        int reqXp = getRequiredXp(level);

        boolean leveledUp = false;
        while (newXp >= reqXp) {
            int maxLevel = plugin.getConfigManager().getConfig().getInt("progression.max-level", 50);
            if (level >= maxLevel) {
                newXp = reqXp; // Cap XP at max level
                break;
            }
            newXp -= reqXp;
            level++;
            reqXp = getRequiredXp(level);
            leveledUp = true;
        }

        // Save new XP and level
        if (isBakat) {
            data.setBakatLevel(key, level);
            data.setBakatXp(key, newXp);
        } else {
            data.setSkillLevel(key, level);
            data.setSkillXp(key, newXp);
        }

        // Notify XP gain via ActionBar
        String displayName = getDisplayName(key, isBakat);
        String actionbarMsg = plugin.getConfigManager().getMessage("xp-gained-actionbar")
                .replace("%amount%", String.format(Locale.US, "%.1f", amount))
                .replace("%name%", displayName)
                .replace("%xp%", String.valueOf((int) newXp))
                .replace("%req_xp%", String.valueOf(reqXp));
        sendActionBar(player, actionbarMsg);

        if (leveledUp) {
            triggerLevelUp(player, key, level, isBakat);
        }
    }

    /**
     * Triggers Level-up effects, messages, points reward, and attribute modifier updates.
     */
    private void triggerLevelUp(Player player, String id, int newLevel, boolean isBakat) {
        String displayName = getDisplayName(id, isBakat);

        // 1. Title and Subtitle
        String title = plugin.getConfigManager().getMessage("level-up-title");
        String subtitle = plugin.getConfigManager().getMessage("level-up-subtitle")
                .replace("%name%", displayName)
                .replace("%level%", String.valueOf(newLevel));
        player.sendTitle(title, subtitle, 10, 50, 10);

        // 2. Play Sound
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);

        // 3. Spawn Spiral Helix Particles around player
        spawnLevelUpParticles(player);

        // 4. BossBar
        showLevelUpBossBar(player, displayName, newLevel);

        // 5. Apply Passive Attribute Modifiers & Custom Level Actions
        PlayerData data = plugin.getPlayerManager().getPlayerData(player.getUniqueId());
        applyAttributes(player, data);
        executeMilestoneRewards(player, id, newLevel, isBakat);

        // 6. Reward Skill Points (+1 SP per level)
        int spPerLevel = plugin.getConfigManager().getConfig().getInt("progression.points-per-level", 1);
        data.addPoints(spPerLevel);
        player.sendMessage(ConfigManager.color("&a&l[+] &fKamu mendapatkan &e" + spPerLevel + " Skill Points &f(Level Up)!"));

        // Save immediately
        plugin.getPlayerManager().savePlayerData(player.getUniqueId());
    }

    /**
     * Spawns a beautiful particle helix ascending around the player.
     */
    private void spawnLevelUpParticles(Player player) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            for (double y = 0; y <= 2.2; y += 0.05) {
                double angle = y * Math.PI * 4; // 2 complete rotations
                double x = 0.6 * Math.sin(angle);
                double z = 0.6 * Math.cos(angle);
                player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(x, y, z), 2, 0, 0, 0, 0);
                try {
                    Thread.sleep(15);
                } catch (InterruptedException ignored) {}
            }
        });
    }

    /**
     * Show level up BossBar to player.
     */
    private void showLevelUpBossBar(Player player, String name, int level) {
        String barTitle = ConfigManager.color("&a&lLEVEL UP! &e" + name + " &fke Level &d" + level);
        BossBar bossBar = Bukkit.createBossBar(barTitle, BarColor.GREEN, BarStyle.SOLID);
        bossBar.addPlayer(player);
        bossBar.setProgress(1.0);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            bossBar.removePlayer(player);
        }, 80L); // 4 seconds
    }

    /**
     * Safely applies attribute modifiers based on current skills and bakat levels.
     */
    public void applyAttributes(Player player, PlayerData data) {
        // Apply modifiers from both skills and bakat
        applySingleSetAttributes(player, data, false); // Skills
        applySingleSetAttributes(player, data, true);  // Bakat
    }

    private void applySingleSetAttributes(Player player, PlayerData data, boolean isBakat) {
        FileConfiguration config = plugin.getConfigManager().getConfig();
        String path = isBakat ? "progression.bakat" : "progression.skills";
        ConfigurationSection section = config.getConfigurationSection(path);
        if (section == null) return;

        for (String id : section.getKeys(false)) {
            int level = isBakat ? data.getBakatLevel(id) : data.getSkillLevel(id);

            // 1. Process custom per_level scaling if configured
            ConfigurationSection scalingSec = config.getConfigurationSection(path + "." + id + ".effects.per_level");
            if (scalingSec != null) {
                for (String attrKey : scalingSec.getKeys(false)) {
                    double perLvlValue = scalingSec.getDouble(attrKey, 0.0);
                    double totalValue = perLvlValue * level;
                    if (totalValue > 0) {
                        applyAttributeModifier(player, id, attrKey, totalValue);
                    }
                }
            }

            // 2. Process milestone levels
            ConfigurationSection milestoneSec = config.getConfigurationSection(path + "." + id + ".effects.levels");
            if (milestoneSec != null) {
                // Find the highest milestone level achieved
                int bestMilestone = 0;
                for (String msKey : milestoneSec.getKeys(false)) {
                    try {
                        int msLvl = Integer.parseInt(msKey);
                        if (level >= msLvl && msLvl > bestMilestone) {
                            bestMilestone = msLvl;
                        }
                    } catch (NumberFormatException ignored) {}
                }

                if (bestMilestone > 0) {
                    ConfigurationSection milestoneLevelSec = milestoneSec.getConfigurationSection(String.valueOf(bestMilestone));
                    if (milestoneLevelSec != null && milestoneLevelSec.contains("attributes")) {
                        ConfigurationSection attrSec = milestoneLevelSec.getConfigurationSection("attributes");
                        if (attrSec != null) {
                            for (String attrKey : attrSec.getKeys(false)) {
                                double val = attrSec.getDouble(attrKey, 0.0);
                                applyAttributeModifier(player, id + "_ms", attrKey, val);
                            }
                        }
                    }
                } else {
                    // Remove any milestone modifier if player hasn't achieved any milestones yet
                    removeAttributeModifier(player, id + "_ms");
                }
            }
        }
    }

    private void applyAttributeModifier(Player player, String id, String attributeName, double value) {
        try {
            Attribute attribute = Attribute.valueOf(attributeName.toUpperCase());
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance == null) return;

            String modifierName = "NSkills_" + id;
            UUID modifierUuid = attributeUuids.computeIfAbsent(id.toLowerCase(), k -> UUID.randomUUID());

            // Remove old modifier
            for (AttributeModifier mod : new ArrayList<>(instance.getModifiers())) {
                if (mod.getName().equalsIgnoreCase(modifierName) || mod.getUniqueId().equals(modifierUuid)) {
                    instance.removeModifier(mod);
                }
            }

            // Add new modifier
            if (value > 0) {
                AttributeModifier modifier = new AttributeModifier(modifierUuid, modifierName, value, AttributeModifier.Operation.ADD_NUMBER);
                instance.addModifier(modifier);
            }
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid attribute name in config: " + attributeName);
        }
    }

    private void removeAttributeModifier(Player player, String id) {
        String modifierName = "NSkills_" + id;
        UUID modifierUuid = attributeUuids.get(id.toLowerCase());

        for (Attribute attribute : Attribute.values()) {
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance != null) {
                for (AttributeModifier mod : new ArrayList<>(instance.getModifiers())) {
                    if (mod.getName().equalsIgnoreCase(modifierName) || (modifierUuid != null && mod.getUniqueId().equals(modifierUuid))) {
                        instance.removeModifier(mod);
                    }
                }
            }
        }
    }

    /**
     * Executes commands or messages defined in milestone rewards.
     */
    private void executeMilestoneRewards(Player player, String id, int level, boolean isBakat) {
        FileConfiguration config = plugin.getConfigManager().getConfig();
        String path = isBakat ? "progression.bakat." + id : "progression.skills." + id;
        ConfigurationSection milestoneSec = config.getConfigurationSection(path + ".effects.levels." + level);
        if (milestoneSec != null) {
            // Send Message
            if (milestoneSec.contains("message")) {
                player.sendMessage(ConfigManager.color(milestoneSec.getString("message")));
            }
            // Run Console Commands
            if (milestoneSec.contains("commands")) {
                List<String> commands = milestoneSec.getStringList("commands");
                for (String cmd : commands) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("%player%", player.getName()));
                }
            }
        }
    }

    private String getDisplayName(String id, boolean isBakat) {
        FileConfiguration config = plugin.getConfigManager().getConfig();
        if (isBakat) {
            return ConfigManager.color(config.getString("progression.bakat." + id + ".name", id));
        } else {
            return ConfigManager.color(config.getString("categories." + id + ".name", id));
        }
    }

    private void sendActionBar(Player player, String message) {
        player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                new net.md_5.bungee.api.chat.TextComponent(ConfigManager.color(message)));
    }
}
