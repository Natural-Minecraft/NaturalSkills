package id.naturalsmp.naturalSkill.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SkillTabCompleter implements TabCompleter {

    private static final List<String> PLAYER_SUBS = Collections.singletonList("help");
    private static final List<String> ADMIN_SUBS = Arrays.asList("give", "take", "set", "reload", "admin", "help");

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String cmdName = command.getName().toLowerCase();
        List<String> completions = new ArrayList<>();

        if (cmdName.equals("skill") || cmdName.equals("skills")) {
            if (args.length == 1) {
                StringUtil.copyPartialMatches(args[0], PLAYER_SUBS, completions);
                Collections.sort(completions);
                return completions;
            }
        } else if (cmdName.equals("nskill") || cmdName.equals("naturalskill")) {
            if (!sender.hasPermission("naturalskill.admin")) {
                return Collections.emptyList();
            }

            if (args.length == 1) {
                StringUtil.copyPartialMatches(args[0], ADMIN_SUBS, completions);
                Collections.sort(completions);
                return completions;
            }

            if (args.length == 2) {
                String sub = args[0].toLowerCase();
                if (sub.equals("give") || sub.equals("take") || sub.equals("set")) {
                    List<String> playerNames = new ArrayList<>();
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        playerNames.add(player.getName());
                    }
                    StringUtil.copyPartialMatches(args[1], playerNames, completions);
                    Collections.sort(completions);
                    return completions;
                }
            }

            if (args.length == 3) {
                String sub = args[0].toLowerCase();
                if (sub.equals("give") || sub.equals("take") || sub.equals("set")) {
                    return Collections.singletonList("<amount>");
                }
            }
        }

        return Collections.emptyList();
    }
}
