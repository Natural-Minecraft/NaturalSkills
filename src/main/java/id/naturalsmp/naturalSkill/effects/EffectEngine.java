package id.naturalsmp.naturalSkill.effects;

import id.naturalsmp.naturalSkill.NaturalSkill;
import id.naturalsmp.naturalSkill.config.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class EffectEngine {

    private final NaturalSkill plugin;

    public EffectEngine(NaturalSkill plugin) {
        this.plugin = plugin;
    }

    /**
     * Executes a list of custom effects for a player.
     */
    public void executeEffects(Player player, List<String> effects) {
        if (effects == null || effects.isEmpty()) return;

        for (String effect : effects) {
            try {
                executeSingleEffect(player, effect);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error executing effect: " + effect + " for player " + player.getName(), e);
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void executeSingleEffect(Player player, String effect) {
        String trimmed = effect.trim();
        String lower = trimmed.toLowerCase();

        // 1. Console command: [console] give %player% diamond 1
        if (lower.startsWith("[console]")) {
            String cmd = trimmed.substring("[console]".length()).trim();
            cmd = cmd.replace("%player%", player.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        }
        // 2. Player command: [player] say hello
        else if (lower.startsWith("[player]")) {
            String cmd = trimmed.substring("[player]".length()).trim();
            cmd = cmd.replace("%player%", player.getName());
            player.performCommand(cmd);
        }
        // 3. Message: [message] &aYou unlocked a skill!
        else if (lower.startsWith("[message]")) {
            String msg = trimmed.substring("[message]".length()).trim();
            msg = msg.replace("%player%", player.getName());
            player.sendMessage(ConfigManager.color(msg));
        }
        // 4. Broadcast: [broadcast] &a%player% unlocked IQ I!
        else if (lower.startsWith("[broadcast]")) {
            String msg = trimmed.substring("[broadcast]".length()).trim();
            msg = msg.replace("%player%", player.getName());
            Bukkit.broadcastMessage(ConfigManager.color(msg));
        }
        // 5. Sound: [sound] ENTITY_PLAYER_LEVELUP
        else if (lower.startsWith("[sound]")) {
            String soundName = trimmed.substring("[sound]".length()).trim();
            try {
                Sound sound = Sound.valueOf(soundName.toUpperCase());
                player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid sound effect name: " + soundName);
            }
        }
        // 6. Vault: [vault] give 100
        else if (lower.startsWith("[vault]")) {
            if (!plugin.getHookManager().isEconomyEnabled()) {
                plugin.getLogger().warning("Tried to execute [vault] effect but Vault is not loaded!");
                return;
            }
            String content = trimmed.substring("[vault]".length()).trim();
            String[] parts = content.split("\\s+");
            if (parts.length >= 2) {
                String action = parts[0].toLowerCase();
                try {
                    double amount = Double.parseDouble(parts[1]);
                    if (action.equals("give")) {
                        plugin.getHookManager().getEconomy().depositPlayer(player, amount);
                    } else if (action.equals("take")) {
                        plugin.getHookManager().getEconomy().withdrawPlayer(player, amount);
                    }
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("Invalid amount for [vault] effect: " + parts[1]);
                }
            }
        }
        // 7. MMOItems: [mmoitems] SWORD EXCALIBUR 1
        else if (lower.startsWith("[mmoitems]")) {
            if (!plugin.getHookManager().isMMOItemsEnabled()) {
                plugin.getLogger().warning("Tried to execute [mmoitems] effect but MMOItems is not loaded!");
                return;
            }
            String content = trimmed.substring("[mmoitems]".length()).trim();
            String[] parts = content.split("\\s+");
            if (parts.length >= 2) {
                String type = parts[0];
                String id = parts[1];
                int amount = 1;
                if (parts.length >= 3) {
                    try {
                        amount = Integer.parseInt(parts[2]);
                    } catch (NumberFormatException e) {
                        plugin.getLogger().warning("Invalid amount for [mmoitems] effect: " + parts[2]);
                    }
                }
                ItemStack item = plugin.getHookManager().getMMOItem(type, id);
                if (item != null) {
                    item.setAmount(amount);
                    giveItem(player, item);
                } else {
                    plugin.getLogger().warning("MMOItem " + type + ":" + id + " returned null!");
                }
            }
        }
        // 8. McMMO: [mcmmo] herbalism 1000
        else if (lower.startsWith("[mcmmo]")) {
            if (!plugin.getHookManager().isMcMMOEnabled()) {
                plugin.getLogger().warning("Tried to execute [mcmmo] effect but mcMMO is not loaded!");
                return;
            }
            String content = trimmed.substring("[mcmmo]".length()).trim();
            String[] parts = content.split("\\s+");
            if (parts.length >= 2) {
                String skill = parts[0];
                try {
                    int xp = Integer.parseInt(parts[1]);
                    plugin.getHookManager().addMcMmoXp(player, skill, xp);
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("Invalid XP amount for [mcmmo] effect: " + parts[1]);
                }
            }
        }
        // 9. Vanilla Item: [item] DIAMOND 5 &eGift
        else if (lower.startsWith("[item]")) {
            String content = trimmed.substring("[item]".length()).trim();
            String[] parts = content.split("\\s+", 3);
            if (parts.length >= 2) {
                String matName = parts[0].toUpperCase();
                try {
                    Material material = Material.valueOf(matName);
                    int amount = Integer.parseInt(parts[1]);
                    ItemStack item = new ItemStack(material, amount);
                    if (parts.length > 2) {
                        String name = ConfigManager.color(parts[2]);
                        ItemMeta meta = item.getItemMeta();
                        if (meta != null) {
                            meta.setDisplayName(name);
                            item.setItemMeta(meta);
                        }
                    }
                    giveItem(player, item);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid material or amount for [item] effect: " + content);
                }
            }
        }
    }

    /**
     * Safely gives an item stack to the player, dropping overflow on the ground.
     */
    private void giveItem(Player player, ItemStack item) {
        HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(item);
        if (!overflow.isEmpty()) {
            for (ItemStack leftover : overflow.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), leftover);
            }
        }
    }
}
