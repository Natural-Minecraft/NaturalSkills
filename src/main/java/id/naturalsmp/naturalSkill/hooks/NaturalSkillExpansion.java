package id.naturalsmp.naturalSkill.hooks;

import id.naturalsmp.naturalSkill.NaturalSkill;
import id.naturalsmp.naturalSkill.data.PlayerData;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

public class NaturalSkillExpansion extends PlaceholderExpansion {

    private final NaturalSkill plugin;

    public NaturalSkillExpansion(NaturalSkill plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "nskills";
    }

    @Override
    public @NotNull String getAuthor() {
        return "NaturalTechnologies";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return "";
        }

        String lower = params.toLowerCase();

        // 1. Leaderboard Rank/Top placeholders (Offline-friendly since it reads from cache)
        if (lower.contains("_top_")) {
            String[] parts = lower.split("_top_");
            if (parts.length == 2) {
                String category = parts[0];
                String rest = parts[1];

                String rankStr;
                String option = "";
                if (rest.contains("_")) {
                    int idx = rest.indexOf("_");
                    rankStr = rest.substring(0, idx);
                    option = rest.substring(idx + 1);
                } else {
                    rankStr = rest;
                }

                try {
                    int rank = Integer.parseInt(rankStr);
                    if (rank < 1) return "";

                    List<id.naturalsmp.naturalSkill.data.LeaderboardManager.LeaderboardEntry> list =
                            plugin.getLeaderboardManager().getLeaderboard(category);

                    if (list == null || rank > list.size()) {
                        return option.equalsIgnoreCase("name") ? "-" : (option.equalsIgnoreCase("value") ? "0" : "-");
                    }

                    id.naturalsmp.naturalSkill.data.LeaderboardManager.LeaderboardEntry entry = list.get(rank - 1);
                    if (option.equalsIgnoreCase("name")) {
                        return entry.getName();
                    } else if (option.equalsIgnoreCase("value")) {
                        return String.valueOf(entry.getScore());
                    } else {
                        return entry.getName() + " (" + entry.getScore() + ")";
                    }
                } catch (NumberFormatException e) {
                    return "";
                }
            }
        }

        // 2. Category top me (Offline player rank)
        if (lower.endsWith("_top_me")) {
            String category = lower.substring(0, lower.indexOf("_top_me"));
            int rank = plugin.getLeaderboardManager().getPlayerRank(player.getUniqueId(), category);
            return rank > 0 ? String.valueOf(rank) : "-";
        }

        // 3. Player specific data (Online-safe cache lookup)
        PlayerData data = plugin.getPlayerManager().getPlayerData(player.getUniqueId());
        if (data == null) {
            return "";
        }

        if (lower.equalsIgnoreCase("points")) {
            return String.valueOf(data.getPoints());
        }
        if (lower.equalsIgnoreCase("unlocked_count")) {
            return String.valueOf(data.getUnlockedSkills().size());
        }
        if (lower.equalsIgnoreCase("iq")) {
            return String.valueOf(plugin.getLeaderboardManager().calculateScore(data.getUnlockedSkills(), "iq"));
        }
        if (lower.equalsIgnoreCase("strength")) {
            return String.valueOf(plugin.getLeaderboardManager().calculateScore(data.getUnlockedSkills(), "strength"));
        }
        if (lower.equalsIgnoreCase("agility")) {
            return String.valueOf(plugin.getLeaderboardManager().calculateScore(data.getUnlockedSkills(), "agility"));
        }
        if (lower.equalsIgnoreCase("psychology")) {
            return String.valueOf(plugin.getLeaderboardManager().calculateScore(data.getUnlockedSkills(), "psychology"));
        }
        if (lower.equalsIgnoreCase("communication")) {
            return String.valueOf(plugin.getLeaderboardManager().calculateScore(data.getUnlockedSkills(), "communication"));
        }

        // New Progression Placeholders
        if (lower.startsWith("level_")) {
            String skill = lower.substring("level_".length());
            return String.valueOf(data.getSkillLevel(skill));
        }
        if (lower.startsWith("xp_")) {
            String skill = lower.substring("xp_".length());
            return String.format(Locale.US, "%.1f", data.getSkillXp(skill));
        }
        if (lower.startsWith("reqxp_")) {
            String skill = lower.substring("reqxp_".length());
            int lvl = data.getSkillLevel(skill);
            return String.valueOf(plugin.getProgressionManager().getRequiredXp(lvl));
        }
        if (lower.startsWith("bakat_level_")) {
            String bakat = lower.substring("bakat_level_".length());
            return String.valueOf(data.getBakatLevel(bakat));
        }
        if (lower.startsWith("bakat_xp_")) {
            String bakat = lower.substring("bakat_xp_".length());
            return String.format(Locale.US, "%.1f", data.getBakatXp(bakat));
        }
        if (lower.startsWith("bakat_reqxp_")) {
            String bakat = lower.substring("bakat_reqxp_".length());
            int lvl = data.getBakatLevel(bakat);
            return String.valueOf(plugin.getProgressionManager().getRequiredXp(lvl));
        }

        return null;
    }
}
