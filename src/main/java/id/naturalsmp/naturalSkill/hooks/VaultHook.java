package id.naturalsmp.naturalSkill.hooks;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

public class VaultHook {
    private static Economy economy = null;

    public static boolean setup() {
        try {
            RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
            if (rsp != null) {
                economy = rsp.getProvider();
                return true;
            }
        } catch (Throwable ignored) {}
        return false;
    }

    public static boolean hasEconomy() {
        return economy != null;
    }

    public static double getBalance(org.bukkit.entity.Player player) {
        return economy != null ? economy.getBalance(player) : 0.0;
    }

    public static void withdraw(org.bukkit.entity.Player player, double amount) {
        if (economy != null) {
            economy.withdrawPlayer(player, amount);
        }
    }

    public static void deposit(org.bukkit.entity.Player player, double amount) {
        if (economy != null) {
            economy.depositPlayer(player, amount);
        }
    }
}
