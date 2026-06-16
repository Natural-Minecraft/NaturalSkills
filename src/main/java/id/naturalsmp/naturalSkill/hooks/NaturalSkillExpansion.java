package id.naturalsmp.naturalSkill.hooks;

import id.naturalsmp.naturalSkill.NaturalSkill;
import id.naturalsmp.naturalSkill.data.PlayerData;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

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

        return null;
    }
}
