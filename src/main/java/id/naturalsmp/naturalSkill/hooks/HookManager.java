package id.naturalsmp.naturalSkill.hooks;

import id.naturalsmp.naturalSkill.NaturalSkill;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.lang.reflect.Method;
import java.util.logging.Level;

public class HookManager {

    private final NaturalSkill plugin;
    private Economy economy = null;
    private boolean mmoItemsEnabled = false;
    private boolean mcMMOEnabled = false;

    public HookManager(NaturalSkill plugin) {
        this.plugin = plugin;
        setupEconomy();
        setupMMOItems();
        setupMcMMO();
    }

    private void setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
            RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
            if (rsp != null) {
                economy = rsp.getProvider();
                plugin.getLogger().info("Successfully hooked into Vault Economy!");
            }
        } else {
            plugin.getLogger().warning("Vault not found. Economy features will not be available.");
        }
    }

    private void setupMMOItems() {
        if (Bukkit.getPluginManager().getPlugin("MMOItems") != null) {
            mmoItemsEnabled = true;
            plugin.getLogger().info("Successfully hooked into MMOItems!");
        }
    }

    private void setupMcMMO() {
        if (Bukkit.getPluginManager().getPlugin("mcMMO") != null) {
            mcMMOEnabled = true;
            plugin.getLogger().info("Successfully hooked into mcMMO!");
        }
    }

    public Economy getEconomy() {
        return economy;
    }

    public boolean isEconomyEnabled() {
        return economy != null;
    }

    public boolean isMMOItemsEnabled() {
        return mmoItemsEnabled;
    }

    public boolean isMcMMOEnabled() {
        return mcMMOEnabled;
    }

    /**
     * Dynamically load MMOItem using reflection.
     * This avoids hard-coding MMOItems dependency and prevents compiling/runtime issues if it's missing.
     */
    public ItemStack getMMOItem(String typeId, String itemId) {
        if (!mmoItemsEnabled) {
            plugin.getLogger().warning("Tried to get MMOItem but MMOItems is not enabled!");
            return null;
        }
        try {
            // MMOItems.plugin.getItem(Type, ID)
            Class<?> mmoItemsClass = Class.forName("net.IndyICE.mmoitems.MMOItems");
            Object mmoItemsInstance = mmoItemsClass.getField("plugin").get(null);

            Class<?> typeClass = Class.forName("net.IndyICE.mmoitems.api.Type");
            Method getMethod = typeClass.getMethod("get", String.class);
            Object type = getMethod.invoke(null, typeId.toUpperCase());

            if (type == null) {
                plugin.getLogger().warning("MMOItems Type " + typeId + " not found!");
                return null;
            }

            // MMOItems.plugin.getItem(Type type, String id)
            Method getItemMethod = mmoItemsClass.getMethod("getItem", typeClass, String.class);
            Object result = getItemMethod.invoke(mmoItemsInstance, type, itemId.toUpperCase());

            if (result instanceof ItemStack) {
                return (ItemStack) result;
            }

            // Fallback for newer MMOItems versions (getItem returns a custom class or item)
            // Some versions might have net.IndyICE.mmoitems.api.item.mmoitem.MMOItem
            // and we can build it, or they return ItemStack.
            plugin.getLogger().warning("MMOItems getItem did not return a valid ItemStack.");
            return null;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to retrieve MMOItem using reflection: " + typeId + " - " + itemId, e);
            return null;
        }
    }

    /**
     * Give McMMO XP using reflection.
     */
    public boolean addMcMmoXp(Player player, String skillName, int xp) {
        if (!mcMMOEnabled) {
            return false;
        }
        try {
            // com.gmail.nossr50.api.ExperienceAPI.addXP(Player, String, int, String)
            Class<?> experienceAPI = Class.forName("com.gmail.nossr50.api.ExperienceAPI");
            
            try {
                // newer mcMMO versions
                Method addXP = experienceAPI.getMethod("addXP", Player.class, String.class, int.class, String.class);
                addXP.invoke(null, player, skillName, xp, "NaturalSkill Unlock");
                return true;
            } catch (NoSuchMethodException e) {
                // older mcMMO versions
                Method addXP = experienceAPI.getMethod("addXP", Player.class, String.class, int.class);
                addXP.invoke(null, player, skillName, xp);
                return true;
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to give mcMMO XP using reflection to " + player.getName(), e);
            return false;
        }
    }
}
