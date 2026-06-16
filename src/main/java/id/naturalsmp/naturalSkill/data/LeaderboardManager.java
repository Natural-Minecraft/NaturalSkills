package id.naturalsmp.naturalSkill.data;

import id.naturalsmp.naturalSkill.NaturalSkill;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class LeaderboardManager {

    private final NaturalSkill plugin;
    private final Map<String, List<LeaderboardEntry>> leaderboards;
    private final Map<UUID, Map<String, Integer>> playerRanks;

    public LeaderboardManager(NaturalSkill plugin) {
        this.plugin = plugin;
        this.leaderboards = new ConcurrentHashMap<>();
        this.playerRanks = new ConcurrentHashMap<>();
    }

    public static class LeaderboardEntry {
        private final UUID uuid;
        private final String name;
        private final int score;

        public LeaderboardEntry(UUID uuid, String name, int score) {
            this.uuid = uuid;
            this.name = name;
            this.score = score;
        }

        public UUID getUuid() {
            return uuid;
        }

        public String getName() {
            return name;
        }

        public int getScore() {
            return score;
        }
    }

    private static class LoadedPlayerData {
        final UUID uuid;
        final String name;
        final int points;
        final Set<String> unlockedSkills;

        LoadedPlayerData(UUID uuid, String name, int points, Set<String> unlockedSkills) {
            this.uuid = uuid;
            this.name = name;
            this.points = points;
            this.unlockedSkills = unlockedSkills;
        }
    }

    /**
     * Rebuild the leaderboard for all categories asynchronously.
     */
    public void updateLeaderboardsAsync() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Map<String, List<LeaderboardEntry>> tempLeaderboards = new HashMap<>();
            File dataFolder = new File(plugin.getDataFolder(), "playerdata");
            if (!dataFolder.exists()) return;

            File[] files = dataFolder.listFiles((dir, filename) -> filename.endsWith(".yml"));
            if (files == null) return;

            List<LoadedPlayerData> loadedPlayers = new ArrayList<>();

            for (File file : files) {
                String nameWithoutExt = file.getName().substring(0, file.getName().length() - 4);
                try {
                    UUID uuid = UUID.fromString(nameWithoutExt);
                    YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);

                    String lastKnownName = cfg.getString("name");
                    if (lastKnownName == null || lastKnownName.isEmpty()) {
                        lastKnownName = Bukkit.getOfflinePlayer(uuid).getName();
                        if (lastKnownName == null) {
                            lastKnownName = "Unknown";
                        }
                    }

                    int points = cfg.getInt("points", 0);
                    List<String> unlockedList = cfg.getStringList("unlocked_skills");
                    Set<String> unlockedSet = new HashSet<>(unlockedList);

                    loadedPlayers.add(new LoadedPlayerData(uuid, lastKnownName, points, unlockedSet));
                } catch (Exception e) {
                    // Skip malformed data
                }
            }

            // Calculate categories
            String[] categories = {"intelligence", "strength", "agility", "psychology", "communication"};
            for (String category : categories) {
                List<LeaderboardEntry> entries = new ArrayList<>();
                for (LoadedPlayerData p : loadedPlayers) {
                    int score = calculateScore(p.unlockedSkills, category);
                    entries.add(new LeaderboardEntry(p.uuid, p.name, score));
                }

                // Sort descending by score
                entries.sort((a, b) -> Integer.compare(b.getScore(), a.getScore()));
                tempLeaderboards.put(category, entries);

                // Duplicate intelligence as iq
                if (category.equals("intelligence")) {
                    tempLeaderboards.put("iq", new ArrayList<>(entries));
                }
            }

            // Apply to main collections on primary thread
            Bukkit.getScheduler().runTask(plugin, () -> {
                leaderboards.clear();
                leaderboards.putAll(tempLeaderboards);
                rebuildPlayerRanks();
            });
        });
    }

    /**
     * Get leaderboard for a specific category.
     */
    public List<LeaderboardEntry> getLeaderboard(String categoryId) {
        return leaderboards.getOrDefault(categoryId.toLowerCase(), Collections.emptyList());
    }

    /**
     * Get player's rank (1-based index) in a category. Returns -1 if not ranked.
     */
    public int getPlayerRank(UUID uuid, String categoryId) {
        Map<String, Integer> ranks = playerRanks.get(uuid);
        if (ranks == null) return -1;
        return ranks.getOrDefault(categoryId.toLowerCase(), -1);
    }

    /**
     * Calculate score for a set of unlocked skills and category.
     */
    public int calculateScore(Set<String> unlockedSkills, String categoryId) {
        int score = 0;
        if (categoryId.equalsIgnoreCase("intelligence") || categoryId.equalsIgnoreCase("iq")) {
            score = 100; // Base IQ is 100
        }
        for (String skillId : unlockedSkills) {
            String cat = plugin.getSkillCategoryMap().get(skillId.toLowerCase());
            if (cat != null && (cat.equalsIgnoreCase(categoryId) || (cat.equalsIgnoreCase("intelligence") && categoryId.equalsIgnoreCase("iq")))) {
                score += plugin.getSkillCostMap().getOrDefault(skillId.toLowerCase(), 0);
            }
        }
        return score;
    }

    /**
     * Update a player's score live in memory and resort.
     */
    public void updatePlayerScore(UUID uuid, String name, String categoryId, int newScore) {
        updateSingleCategory(uuid, name, categoryId.toLowerCase(), newScore);
        if (categoryId.equalsIgnoreCase("intelligence")) {
            updateSingleCategory(uuid, name, "iq", newScore);
        }
        rebuildPlayerRanks();
    }

    private void updateSingleCategory(UUID uuid, String name, String categoryId, int newScore) {
        List<LeaderboardEntry> list = leaderboards.get(categoryId);
        if (list == null) {
            list = new ArrayList<>();
            leaderboards.put(categoryId, list);
        }

        list.removeIf(e -> e.getUuid().equals(uuid));
        list.add(new LeaderboardEntry(uuid, name, newScore));
        list.sort((a, b) -> Integer.compare(b.getScore(), a.getScore()));
    }

    private void rebuildPlayerRanks() {
        playerRanks.clear();
        for (Map.Entry<String, List<LeaderboardEntry>> entry : leaderboards.entrySet()) {
            String category = entry.getKey();
            List<LeaderboardEntry> list = entry.getValue();
            for (int i = 0; i < list.size(); i++) {
                LeaderboardEntry le = list.get(i);
                playerRanks.computeIfAbsent(le.getUuid(), k -> new HashMap<>())
                           .put(category, i + 1);
            }
        }
    }
}
