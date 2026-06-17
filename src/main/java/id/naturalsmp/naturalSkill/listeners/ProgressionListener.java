package id.naturalsmp.naturalSkill.listeners;

import id.naturalsmp.naturalSkill.NaturalSkill;
import id.naturalsmp.naturalSkill.config.ConfigManager;
import id.naturalsmp.naturalSkill.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class ProgressionListener implements Listener {

    private final NaturalSkill plugin;
    private BukkitTask movementTask;
    private final Map<UUID, Location> lastLocations = new HashMap<>();
    private final Map<UUID, Long> lastChatXp = new HashMap<>();

    public ProgressionListener(NaturalSkill plugin) {
        this.plugin = plugin;
        startMovementTask();
    }

    public void cancelTask() {
        if (movementTask != null) {
            movementTask.cancel();
        }
    }

    /**
     * Runnable task to track active movement for Swimming and Agility (sprinting/running) XP.
     */
    private void startMovementTask() {
        movementTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    UUID uuid = player.getUniqueId();
                    Location loc = player.getLocation();
                    Location lastLoc = lastLocations.get(uuid);
                    lastLocations.put(uuid, loc);

                    if (lastLoc == null || !lastLoc.getWorld().equals(loc.getWorld())) {
                        continue;
                    }

                    double distance = loc.distance(lastLoc);
                    if (distance < 0.8) {
                        continue; // Player is mostly stationary (AFK prevention)
                    }

                    // 1. Swimming Bakat
                    if (player.isSwimming() || loc.getBlock().getType() == Material.WATER) {
                        double xp = plugin.getConfigManager().getConfig().getDouble("progression.xp_sources.swimming.xp_per_second", 2.0);
                        plugin.getProgressionManager().addXp(player, "swimming", xp, true);

                        // Agility Skill also gets a smaller fraction
                        plugin.getProgressionManager().addXp(player, "agility", xp * 0.5, false);
                    }
                    // 2. Agility Skill (Sprinting)
                    else if (player.isSprinting()) {
                        double xp = plugin.getConfigManager().getConfig().getDouble("progression.xp_sources.agility.xp_per_second", 1.5);
                        plugin.getProgressionManager().addXp(player, "agility", xp, false);
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L); // every 1 second
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Material mat = block.getType();
        String matName = mat.name().toLowerCase();

        // 1. Mining Bakat
        if (plugin.getConfigManager().getConfig().contains("progression.xp_sources.mining.blocks." + matName)) {
            double xp = plugin.getConfigManager().getConfig().getDouble("progression.xp_sources.mining.blocks." + matName, 1.0);
            plugin.getProgressionManager().addXp(player, "mining", xp, true);

            // Double Drop Logic
            processDoubleDrop(player, block, "mining");
            return;
        }

        // 2. Woodcutting Bakat
        boolean isLog = mat.name().endsWith("_LOG") || mat.name().endsWith("_WOOD");
        if (isLog && plugin.getConfigManager().getConfig().contains("progression.xp_sources.woodcutting.blocks." + matName)) {
            double xp = plugin.getConfigManager().getConfig().getDouble("progression.xp_sources.woodcutting.blocks." + matName, 5.0);
            plugin.getProgressionManager().addXp(player, "woodcutting", xp, true);

            // Double Drop Logic
            processDoubleDrop(player, block, "woodcutting");
            return;
        }

        // 3. Farming Bakat
        if (plugin.getConfigManager().getConfig().contains("progression.xp_sources.farming.crops." + matName)) {
            // Exploit protection: only fully grown crops award XP
            if (block.getBlockData() instanceof Ageable) {
                Ageable ageable = (Ageable) block.getBlockData();
                if (ageable.getAge() < ageable.getMaximumAge()) {
                    return;
                }
            }

            double xp = plugin.getConfigManager().getConfig().getDouble("progression.xp_sources.farming.crops." + matName, 4.0);
            plugin.getProgressionManager().addXp(player, "farming", xp, true);

            // Double Drop Logic
            processDoubleDrop(player, block, "farming");
        }
    }

    private void processDoubleDrop(Player player, Block block, String bakatId) {
        PlayerData data = plugin.getPlayerManager().getPlayerData(player.getUniqueId());
        if (data == null) return;

        int level = data.getBakatLevel(bakatId);
        double chance = level * 0.005; // 0.5% per level

        if (Math.random() < chance) {
            // Drop additional items
            Collection<ItemStack> drops = block.getDrops(player.getInventory().getItemInMainHand());
            for (ItemStack drop : drops) {
                block.getWorld().dropItemNaturally(block.getLocation(), drop);
            }
            player.sendMessage(ConfigManager.color("&a&l[Bakat] &fDouble Drop aktif! (+1 hasil panen/ore)"));
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.5f);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        if (!(event.getCaught() instanceof Item)) return;

        Player player = event.getPlayer();
        double xp = plugin.getConfigManager().getConfig().getDouble("progression.xp_sources.fishing.caught_fish", 25.0);
        plugin.getProgressionManager().addXp(player, "fishing", xp, true);

        // Double catch logic
        PlayerData data = plugin.getPlayerManager().getPlayerData(player.getUniqueId());
        if (data != null) {
            int level = data.getBakatLevel("fishing");
            double chance = level * 0.005; // 0.5% per level
            if (Math.random() < chance) {
                Item caughtItem = (Item) event.getCaught();
                ItemStack stack = caughtItem.getItemStack();
                ItemStack duplicate = stack.clone();
                player.getWorld().dropItemNaturally(player.getLocation(), duplicate);
                player.sendMessage(ConfigManager.color("&a&l[Bakat] &fDouble Catch aktif! (Mendapatkan ikan ekstra)"));
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.5f);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;

        Player player = (Player) event.getDamager();
        PlayerData data = plugin.getPlayerManager().getPlayerData(player.getUniqueId());
        if (data == null) return;

        // 1. Give Strength XP based on damage dealt
        double damage = event.getFinalDamage();
        double xpScale = plugin.getConfigManager().getConfig().getDouble("progression.xp_sources.strength.damage_dealt_scale", 1.2);
        plugin.getProgressionManager().addXp(player, "strength", damage * xpScale, false);

        // 2. Custom Damage Modifier (Power)
        int strengthLevel = data.getSkillLevel("strength");
        double flatBonus = plugin.getConfigManager().getConfig().getDouble("progression.skills.strength.effects.per_level.damage_flat", 0.0) * strengthLevel;
        double percentBonus = plugin.getConfigManager().getConfig().getDouble("progression.skills.strength.effects.per_level.damage_percent", 0.0) * strengthLevel;

        double currentDamage = event.getDamage();
        double newDamage = currentDamage * (1.0 + percentBonus) + flatBonus;
        event.setDamage(newDamage);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEnchant(EnchantItemEvent event) {
        Player player = event.getEnchanter();
        int cost = event.getExpLevelCost();
        double multiplier = plugin.getConfigManager().getConfig().getDouble("progression.xp_sources.intelligence.enchant_level_multiplier", 12.0);
        plugin.getProgressionManager().addXp(player, "intelligence", cost * multiplier, false);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBrewingStandClick(InventoryClickEvent event) {
        if (event.getInventory().getType() != InventoryType.BREWING) return;
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;

        // Only award XP when taking custom potions out
        if (event.getRawSlot() >= 0 && event.getRawSlot() <= 2) { // Potion slots
            Player player = (Player) event.getWhoClicked();
            double xp = plugin.getConfigManager().getConfig().getDouble("progression.xp_sources.intelligence.potion_brewed", 15.0);
            plugin.getProgressionManager().addXp(player, "intelligence", xp, false);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        long lastTime = lastChatXp.getOrDefault(uuid, 0L);

        // Anti-spam chat cooldown (e.g. 8 seconds)
        long cooldown = plugin.getConfigManager().getConfig().getLong("progression.xp_sources.communication.chat_cooldown_ms", 8000L);
        if (now - lastTime >= cooldown) {
            lastChatXp.put(uuid, now);
            double xp = plugin.getConfigManager().getConfig().getDouble("progression.xp_sources.communication.chat", 3.0);
            // Must award XP on the main Bukkit thread because AsyncTask cannot safely trigger plugins APIs
            Bukkit.getScheduler().runTask(plugin, () -> {
                plugin.getProgressionManager().addXp(player, "communication", xp, false);
            });
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMerchantClick(InventoryClickEvent event) {
        if (!(event.getInventory() instanceof MerchantInventory)) return;
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (event.getRawSlot() != 2) return; // Trade result slot

        Player player = (Player) event.getWhoClicked();
        ItemStack result = event.getCurrentItem();
        if (result == null || result.getType() == Material.AIR) return;

        // Double check they are taking the item out
        if (event.getClick() != ClickType.LEFT && event.getClick() != ClickType.RIGHT && event.getClick() != ClickType.SHIFT_LEFT && event.getClick() != ClickType.SHIFT_RIGHT) {
            return;
        }

        double xp = plugin.getConfigManager().getConfig().getDouble("progression.xp_sources.communication.trade", 10.0);
        plugin.getProgressionManager().addXp(player, "communication", xp, false);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        Material mat = event.getItem().getType();

        // Psychology XP from eating food (scale with food value)
        int foodLevel = 1;
        if (mat.isEdible()) {
            foodLevel = 4; // standard fallback
        }
        double multiplier = plugin.getConfigManager().getConfig().getDouble("progression.xp_sources.psychology.eat_food_multiplier", 4.0);
        plugin.getProgressionManager().addXp(player, "psychology", foodLevel * multiplier, false);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Material mat = event.getBlock().getType();
        String matName = mat.name().toLowerCase();

        // 1. Arsitek Bakat - placing building blocks
        if (plugin.getConfigManager().getConfig().contains("progression.xp_sources.arsitek.blocks." + matName)) {
            double xp = plugin.getConfigManager().getConfig().getDouble("progression.xp_sources.arsitek.blocks." + matName, 1.0);
            plugin.getProgressionManager().addXp(player, "arsitek", xp, true);
            return;
        }
        // Fallback: any solid building block gives a small amount
        if (isBuildingBlock(mat)) {
            double xp = plugin.getConfigManager().getConfig().getDouble("progression.xp_sources.arsitek.default_block_xp", 0.8);
            plugin.getProgressionManager().addXp(player, "arsitek", xp, true);
            return;
        }

        // 2. Teknik Mesin Bakat - placing redstone components
        if (isRedstoneComponent(mat)) {
            double xp;
            String teknikKey = "progression.xp_sources.teknik_mesin.blocks." + matName;
            if (plugin.getConfigManager().getConfig().contains(teknikKey)) {
                xp = plugin.getConfigManager().getConfig().getDouble(teknikKey, 5.0);
            } else {
                xp = plugin.getConfigManager().getConfig().getDouble("progression.xp_sources.teknik_mesin.redstone_place", 5.0);
            }
            plugin.getProgressionManager().addXp(player, "teknik_mesin", xp, true);
        }
    }

    private boolean isBuildingBlock(Material mat) {
        String name = mat.name();
        return name.endsWith("_PLANKS") || name.endsWith("_SLAB") || name.endsWith("_STAIRS")
                || name.endsWith("_BRICKS") || name.endsWith("_WALL") || name.endsWith("_FENCE")
                || name.endsWith("_GLASS") || name.endsWith("_GLASS_PANE")
                || name.contains("STONE") || name.contains("CONCRETE") || name.contains("TERRACOTTA")
                || name.contains("SANDSTONE") || name.contains("COBBLESTONE")
                || mat == Material.STONE || mat == Material.DIRT || mat == Material.GRASS_BLOCK
                || mat == Material.SAND || mat == Material.GRAVEL;
    }

    private boolean isRedstoneComponent(Material mat) {
        switch (mat) {
            case REDSTONE_WIRE:
            case REPEATER:
            case COMPARATOR:
            case PISTON:
            case STICKY_PISTON:
            case OBSERVER:
            case DROPPER:
            case DISPENSER:
            case HOPPER:
            case LEVER:
            case STONE_BUTTON:
            case OAK_BUTTON:
            case SPRUCE_BUTTON:
            case BIRCH_BUTTON:
            case JUNGLE_BUTTON:
            case ACACIA_BUTTON:
            case DARK_OAK_BUTTON:
            case MANGROVE_BUTTON:
            case CHERRY_BUTTON:
            case CRIMSON_BUTTON:
            case WARPED_BUTTON:
            case POLISHED_BLACKSTONE_BUTTON:
            case TRIPWIRE_HOOK:
            case TARGET:
            case LIGHTNING_ROD:
            case NOTE_BLOCK:
            case DAYLIGHT_DETECTOR:
            case TRAPPED_CHEST:
            case TNT:
            case REDSTONE_LAMP:
            case REDSTONE_TORCH:
                return true;
            default:
                return false;
        }
    }
}
