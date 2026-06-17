package id.naturalsmp.naturalSkill.data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PlayerData {

    private final UUID uuid;
    private int points;
    private final Set<String> unlockedSkills;
    private String name = "";

    // Skills level & XP
    private final Map<String, Integer> skillLevels = new HashMap<>();
    private final Map<String, Double> skillXp = new HashMap<>();

    // Bakat level & XP
    private final Map<String, Integer> bakatLevels = new HashMap<>();
    private final Map<String, Double> bakatXp = new HashMap<>();

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
        this.points = 0;
        this.unlockedSkills = new HashSet<>();
    }

    public PlayerData(UUID uuid, int points, Set<String> unlockedSkills) {
        this.uuid = uuid;
        this.points = points;
        this.unlockedSkills = new HashSet<>(unlockedSkills);
    }

    public PlayerData(UUID uuid, String name, int points, Set<String> unlockedSkills) {
        this.uuid = uuid;
        this.name = name;
        this.points = points;
        this.unlockedSkills = new HashSet<>(unlockedSkills);
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = Math.max(0, points);
    }

    public void addPoints(int amount) {
        if (amount > 0) {
            this.points += amount;
        }
    }

    public boolean removePoints(int amount) {
        if (amount <= 0) return true;
        if (this.points >= amount) {
            this.points -= amount;
            return true;
        }
        return false;
    }

    public Set<String> getUnlockedSkills() {
        return unlockedSkills;
    }

    public boolean isSkillUnlocked(String skillId) {
        return unlockedSkills.contains(skillId.toLowerCase());
    }

    public void unlockSkill(String skillId) {
        unlockedSkills.add(skillId.toLowerCase());
    }

    public void lockSkill(String skillId) {
        unlockedSkills.remove(skillId.toLowerCase());
    }

    // Skills Getters & Setters
    public int getSkillLevel(String skillId) {
        return skillLevels.getOrDefault(skillId.toLowerCase(), 1);
    }

    public void setSkillLevel(String skillId, int level) {
        skillLevels.put(skillId.toLowerCase(), Math.max(1, level));
    }

    public double getSkillXp(String skillId) {
        return skillXp.getOrDefault(skillId.toLowerCase(), 0.0);
    }

    public void setSkillXp(String skillId, double xp) {
        skillXp.put(skillId.toLowerCase(), Math.max(0.0, xp));
    }

    public Map<String, Integer> getSkillLevels() {
        return skillLevels;
    }

    public Map<String, Double> getSkillXpMap() {
        return skillXp;
    }

    // Bakat Getters & Setters
    public int getBakatLevel(String bakatId) {
        return bakatLevels.getOrDefault(bakatId.toLowerCase(), 1);
    }

    public void setBakatLevel(String bakatId, int level) {
        bakatLevels.put(bakatId.toLowerCase(), Math.max(1, level));
    }

    public double getBakatXp(String bakatId) {
        return bakatXp.getOrDefault(bakatId.toLowerCase(), 0.0);
    }

    public void setBakatXp(String bakatId, double xp) {
        bakatXp.put(bakatId.toLowerCase(), Math.max(0.0, xp));
    }

    public Map<String, Integer> getBakatLevels() {
        return bakatLevels;
    }

    public Map<String, Double> getBakatXpMap() {
        return bakatXp;
    }
}
