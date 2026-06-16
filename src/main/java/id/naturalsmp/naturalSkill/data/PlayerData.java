package id.naturalsmp.naturalSkill.data;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PlayerData {

    private final UUID uuid;
    private int points;
    private final Set<String> unlockedSkills;

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

    public UUID getUuid() {
        return uuid;
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
}
