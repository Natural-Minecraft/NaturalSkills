package id.naturalsmp.naturalSkill.commands;

import id.naturalsmp.naturalSkill.NaturalSkill;
import id.naturalsmp.naturalSkill.config.ConfigManager;
import id.naturalsmp.naturalSkill.data.PlayerData;
import id.naturalsmp.naturalSkill.gui.SkillGui;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.UUID;

public class SkillCommand implements CommandExecutor {

    private final NaturalSkill plugin;

    public SkillCommand(NaturalSkill plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String cmdName = command.getName().toLowerCase();

        if (cmdName.equals("skill") || cmdName.equals("skills")) {
            return handlePlayerCommand(sender, args);
        } else if (cmdName.equals("nskill") || cmdName.equals("naturalskill")) {
            return handleAdminCommand(sender, args);
        }

        return false;
    }

    private boolean handlePlayerCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("player-only"));
            return true;
        }

        Player player = (Player) sender;

        if (args.length > 0 && args[0].equalsIgnoreCase("help")) {
            sendPlayerHelp(player);
            return true;
        }

        // Default behavior: open player GUI
        new SkillGui(plugin, player).openMainMenu();
        return true;
    }

    private boolean handleAdminCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("naturalskill.admin")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendAdminHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("reload")) {
            plugin.getConfigManager().reloadConfigs();
            sender.sendMessage(plugin.getConfigManager().getMessage("reload"));
            return true;
        }

        if (sub.equals("admin")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(plugin.getConfigManager().getMessage("player-only"));
                return true;
            }
            Player player = (Player) sender;
            new SkillGui(plugin, player).openAdminMenu();
            return true;
        }

        // commands: give <player> <amount>, take <player> <amount>, set <player> <amount>
        if (sub.equals("give") || sub.equals("take") || sub.equals("set")) {
            if (args.length < 3) {
                sender.sendMessage("&cUsage: /nskill " + sub + " <player> <amount>");
                return true;
            }

            String targetName = args[1];
            int amount;
            try {
                amount = Integer.parseInt(args[2]);
                if (amount < 0) {
                    sender.sendMessage(plugin.getConfigManager().getMessage("invalid-number"));
                    return true;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(plugin.getConfigManager().getMessage("invalid-number"));
                return true;
            }

            // First check if the player is currently online (cheapest lookup)
            Player onlineCheck = Bukkit.getPlayerExact(targetName);
            OfflinePlayer target;
            if (onlineCheck != null) {
                target = onlineCheck;
            } else {
                // Use cache-only lookup to avoid blocking the main thread with a Mojang API call
                target = Bukkit.getOfflinePlayerIfCached(targetName);
            }
             if (target == null || (!target.hasPlayedBefore() && !target.isOnline())) {
                sender.sendMessage(ConfigManager.color(plugin.getConfigManager().getMessage("player-not-found").replace("%player%", targetName)));
                return true;
            }

            UUID uuid = target.getUniqueId();
            PlayerData data = plugin.getPlayerManager().getPlayerData(uuid);
            boolean isOnline = target.isOnline();

            int finalPoints = 0;
            if (sub.equals("give")) {
                data.addPoints(amount);
                finalPoints = data.getPoints();
                sender.sendMessage(ConfigManager.color(plugin.getConfigManager().getMessage("points-given")
                        .replace("%player%", targetName)
                        .replace("%amount%", String.valueOf(amount))
                        .replace("%total%", String.valueOf(finalPoints))));
            } else if (sub.equals("take")) {
                data.removePoints(amount);
                finalPoints = data.getPoints();
                sender.sendMessage(ConfigManager.color(plugin.getConfigManager().getMessage("points-taken")
                        .replace("%player%", targetName)
                        .replace("%amount%", String.valueOf(amount))
                        .replace("%total%", String.valueOf(finalPoints))));
            } else if (sub.equals("set")) {
                data.setPoints(amount);
                finalPoints = data.getPoints();
                sender.sendMessage(ConfigManager.color(plugin.getConfigManager().getMessage("points-set")
                        .replace("%player%", targetName)
                        .replace("%amount%", String.valueOf(amount))));
            }

            // Save data
            if (!isOnline) {
                // Save and unload immediately if player is offline to prevent memory leak
                plugin.getPlayerManager().savePlayerData(uuid);
                plugin.getPlayerManager().unloadPlayerData(uuid);
            } else {
                // Just save to disk, keep in cache
                plugin.getPlayerManager().savePlayerData(uuid);
                // If player is online, we might want to update any open GUI
                Player onlineTarget = target.getPlayer();
                if (onlineTarget != null && SkillGui.isGuiOpen(onlineTarget)) {
                    // Refresh GUI
                    new SkillGui(plugin, onlineTarget).refreshCurrentGui();
                }
            }

            return true;
        }

        sendAdminHelp(sender);
        return true;
    }

    private void sendPlayerHelp(Player player) {
        player.sendMessage(ConfigManager.color("&a&m================&e&l NaturalSkill Help &a&m================"));
        player.sendMessage(ConfigManager.color("&e/skill &7- Membuka menu GUI pohon skill."));
        player.sendMessage(ConfigManager.color("&e/skill help &7- Menampilkan bantuan ini."));
        player.sendMessage(ConfigManager.color("&a&m================================================"));
    }

    private void sendAdminHelp(CommandSender sender) {
        sender.sendMessage(ConfigManager.color("&4&m================&c&l NaturalSkill Admin &4&m================"));
        sender.sendMessage(ConfigManager.color("&c/nskill give <player> <amount> &7- Memberikan Skill Points."));
        sender.sendMessage(ConfigManager.color("&c/nskill take <player> <amount> &7- Mengambil Skill Points."));
        sender.sendMessage(ConfigManager.color("&c/nskill set <player> <amount> &7- Mengatur Skill Points pemain."));
        sender.sendMessage(ConfigManager.color("&c/nskill admin &7- Membuka Admin Editor GUI."));
        sender.sendMessage(ConfigManager.color("&c/nskill reload &7- Memuat ulang config dan messages."));
        sender.sendMessage(ConfigManager.color("&c/nskill help &7- Menampilkan bantuan ini."));
        sender.sendMessage(ConfigManager.color("&4&m=================================================="));
    }
}
