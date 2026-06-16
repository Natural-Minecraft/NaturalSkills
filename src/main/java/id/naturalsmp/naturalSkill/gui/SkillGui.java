package id.naturalsmp.naturalSkill.gui;

import id.naturalsmp.naturalSkill.NaturalSkill;
import id.naturalsmp.naturalSkill.config.ConfigManager;
import id.naturalsmp.naturalSkill.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class SkillGui {

    private final NaturalSkill plugin;
    private final Player player;
    private final PlayerData playerData;

    // Track active cost edits temporarily: Player UUID -> Map of (SkillKey -> TempCost)
    private static final Map<UUID, Integer> activeCostEdits = new HashMap<>();

    /**
     * Clears the temporary cost edit entry for a player.
     * Called externally (e.g., from GuiListener on inventory close) to prevent memory leaks.
     */
    public static void clearCostEdit(UUID uuid) {
        activeCostEdits.remove(uuid);
    }

    public SkillGui(NaturalSkill plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.playerData = plugin.getPlayerManager().getPlayerData(player.getUniqueId());
    }

    public static boolean isGuiOpen(Player player) {
        if (player.getOpenInventory() != null && player.getOpenInventory().getTopInventory() != null) {
            return player.getOpenInventory().getTopInventory().getHolder() instanceof SkillInventoryHolder;
        }
        return false;
    }

    /**
     * Refresh the current open GUI if it belongs to this plugin.
     */
    public void refreshCurrentGui() {
        if (player.getOpenInventory() != null && player.getOpenInventory().getTopInventory() != null) {
            Inventory open = player.getOpenInventory().getTopInventory();
            if (open.getHolder() instanceof SkillInventoryHolder) {
                SkillInventoryHolder holder = (SkillInventoryHolder) open.getHolder();
                switch (holder.getMenuType()) {
                    case "main":
                        openMainMenu();
                        break;
                    case "category":
                        openCategoryMenu(holder.getCategoryId());
                        break;
                    case "admin_main":
                        openAdminMenu();
                        break;
                    case "admin_category":
                        openAdminCategoryMenu(holder.getCategoryId());
                        break;
                    case "admin_cost_edit":
                        openAdminCostEditorMenu(holder.getCategoryId(), holder.getBranchId());
                        break;
                }
            }
        }
    }

    /**
     * 1. Player Main Menu
     */
    public void openMainMenu() {
        String title = plugin.getConfigManager().getMessage("gui-title-main");
        SkillInventoryHolder holder = new SkillInventoryHolder("main", null, null);
        Inventory inv = Bukkit.createInventory(holder, 54, title);
        holder.setInventory(inv);

        fillDecorations(inv);

        // Player Info Book in slot 4
        inv.setItem(4, createItem(Material.BOOK, "&e&lStatistik Poin", Arrays.asList(
                "&7Pemain: &a" + player.getName(),
                "&7Skill Points: &e" + playerData.getPoints() + " SP"
        )));

        // Load categories
        FileConfiguration config = plugin.getConfigManager().getConfig();
        ConfigurationSection categories = config.getConfigurationSection("categories");
        if (categories != null) {
            for (String key : categories.getKeys(false)) {
                String name = categories.getString(key + ".name", key);
                String materialName = categories.getString(key + ".icon", "BOOK");
                Material material = getMaterial(materialName, Material.BOOK);
                int slot = categories.getInt(key + ".slot", 10);
                List<String> lore = categories.getStringList(key + ".lore");

                inv.setItem(slot, createItem(material, name, lore));
            }
        }

        player.openInventory(inv);
    }

    /**
     * 2. Player Category Submenu
     */
    public void openCategoryMenu(String categoryId) {
        FileConfiguration config = plugin.getConfigManager().getConfig();
        String catName = config.getString("categories." + categoryId + ".name", categoryId);
        String title = plugin.getConfigManager().getMessage("gui-title-category")
                .replace("%category%", catName);

        SkillInventoryHolder holder = new SkillInventoryHolder("category", categoryId, null);
        Inventory inv = Bukkit.createInventory(holder, 54, title);
        holder.setInventory(inv);

        fillDecorations(inv);

        // Player Stats in slot 4
        inv.setItem(4, createItem(Material.BOOK, "&e&lStatistik Poin", Arrays.asList(
                "&7Pemain: &a" + player.getName(),
                "&7Skill Points: &e" + playerData.getPoints() + " SP",
                "&7Kategori: " + catName
        )));

        // Load branches
        ConfigurationSection branches = config.getConfigurationSection("categories." + categoryId + ".branches");
        if (branches != null) {
            for (String branchId : branches.getKeys(false)) {
                String name = branches.getString(branchId + ".name", branchId);
                String materialName = branches.getString(branchId + ".icon", "ENCHANTED_BOOK");
                Material material = getMaterial(materialName, Material.ENCHANTED_BOOK);
                int slot = branches.getInt(branchId + ".slot", 22);
                int cost = branches.getInt(branchId + ".cost", 0);
                String prereq = branches.getString(branchId + ".prerequisite", "");

                // Determine status
                String status;
                String fullKey = categoryId + "." + branchId;
                if (playerData.isSkillUnlocked(fullKey)) {
                    status = plugin.getConfigManager().getMessage("gui-unlocked-status");
                } else if (!prereq.isEmpty() && !playerData.isSkillUnlocked(categoryId + "." + prereq)) {
                    status = plugin.getConfigManager().getMessage("gui-locked-status");
                } else if (playerData.getPoints() >= cost) {
                    status = plugin.getConfigManager().getMessage("gui-purchasable-status");
                } else {
                    status = plugin.getConfigManager().getMessage("gui-locked-status") + " &c(Poin kurang)";
                }

                List<String> rawLore = branches.getStringList(branchId + ".lore");
                List<String> formattedLore = new ArrayList<>();
                for (String line : rawLore) {
                    formattedLore.add(line
                            .replace("%cost%", String.valueOf(cost))
                            .replace("%status%", status));
                }

                inv.setItem(slot, createItem(material, name, formattedLore));
            }
        }

        // Back button in slot 49
        inv.setItem(49, getBackButton());

        player.openInventory(inv);
    }

    /**
     * 3. Admin Main Menu
     */
    public void openAdminMenu() {
        String title = "&cAdmin Editor - Pohon Skill";
        SkillInventoryHolder holder = new SkillInventoryHolder("admin_main", null, null);
        Inventory inv = Bukkit.createInventory(holder, 54, title);
        holder.setInventory(inv);

        fillDecorations(inv);

        // Info Book in slot 4
        inv.setItem(4, createItem(Material.BARRIER, "&c&lMODE EDITOR ADMIN", Arrays.asList(
                "&7Pilih kategori di bawah untuk",
                "&7mengedit harga/biaya cabang skill."
        )));

        // Load categories
        FileConfiguration config = plugin.getConfigManager().getConfig();
        ConfigurationSection categories = config.getConfigurationSection("categories");
        if (categories != null) {
            for (String key : categories.getKeys(false)) {
                String name = categories.getString(key + ".name", key);
                String materialName = categories.getString(key + ".icon", "BOOK");
                Material material = getMaterial(materialName, Material.BOOK);
                int slot = categories.getInt(key + ".slot", 10);
                List<String> lore = new ArrayList<>(categories.getStringList(key + ".lore"));
                lore.add("");
                lore.add("&c[Admin] Klik untuk mengedit cabang!");

                inv.setItem(slot, createItem(material, name, lore));
            }
        }

        player.openInventory(inv);
    }

    /**
     * 4. Admin Category Menu
     */
    public void openAdminCategoryMenu(String categoryId) {
        FileConfiguration config = plugin.getConfigManager().getConfig();
        String catName = config.getString("categories." + categoryId + ".name", categoryId);
        String title = "&cAdmin: %category%".replace("%category%", catName);

        SkillInventoryHolder holder = new SkillInventoryHolder("admin_category", categoryId, null);
        Inventory inv = Bukkit.createInventory(holder, 54, title);
        holder.setInventory(inv);

        fillDecorations(inv);

        inv.setItem(4, createItem(Material.BARRIER, "&c&lMODE EDITOR ADMIN", Arrays.asList(
                "&7Kategori: " + catName,
                "&7Klik cabang skill untuk mengedit harganya."
        )));

        // Load branches
        ConfigurationSection branches = config.getConfigurationSection("categories." + categoryId + ".branches");
        if (branches != null) {
            for (String branchId : branches.getKeys(false)) {
                String name = branches.getString(branchId + ".name", branchId);
                String materialName = branches.getString(branchId + ".icon", "ENCHANTED_BOOK");
                Material material = getMaterial(materialName, Material.ENCHANTED_BOOK);
                int slot = branches.getInt(branchId + ".slot", 22);
                int cost = branches.getInt(branchId + ".cost", 0);

                List<String> rawLore = branches.getStringList(branchId + ".lore");
                List<String> formattedLore = new ArrayList<>();
                for (String line : rawLore) {
                    formattedLore.add(line
                            .replace("%cost%", String.valueOf(cost))
                            .replace("%status%", "&e[Edit Cost]"));
                }
                formattedLore.add("");
                formattedLore.add("&cKlik untuk mengedit biaya (SP)!");

                inv.setItem(slot, createItem(material, name, formattedLore));
            }
        }

        // Back button in slot 49
        inv.setItem(49, createItem(Material.ARROW, "&cKembali ke Kategori (Admin)", List.of()));

        player.openInventory(inv);
    }

    /**
     * 5. Admin Cost Editor Submenu
     */
    public void openAdminCostEditorMenu(String categoryId, String branchId) {
        FileConfiguration config = plugin.getConfigManager().getConfig();
        String branchName = config.getString("categories." + categoryId + ".branches." + branchId + ".name", branchId);
        String title = "&cEdit Cost: " + branchName;

        SkillInventoryHolder holder = new SkillInventoryHolder("admin_cost_edit", categoryId, branchId);
        Inventory inv = Bukkit.createInventory(holder, 54, title);
        holder.setInventory(inv);

        fillDecorations(inv);

        // Get current cost (temp or config)
        int currentCost = activeCostEdits.computeIfAbsent(player.getUniqueId(), k ->
                config.getInt("categories." + categoryId + ".branches." + branchId + ".cost", 0));

        // Display current state in slot 4 (Barrier)
        inv.setItem(4, createItem(Material.BARRIER, "&c&lEDITOR BIAYA SKILL", Arrays.asList(
                "&7Cabang: " + branchName,
                "&7Biaya Sementara: &e" + currentCost + " SP"
        )));

        // Center item (represent the skill item)
        String materialName = config.getString("categories." + categoryId + ".branches." + branchId + ".icon", "ENCHANTED_BOOK");
        Material material = getMaterial(materialName, Material.ENCHANTED_BOOK);
        inv.setItem(22, createItem(material, branchName, Arrays.asList(
                "&7Biaya Saat Ini: &e" + currentCost + " SP",
                "",
                "&7Gunakan tombol di samping untuk",
                "&7menambah atau mengurangi biaya."
        )));

        // Red Glass Panes on Left (Decrease)
        inv.setItem(20, createItem(Material.RED_STAINED_GLASS_PANE, "&c-5 SP", List.of()));
        inv.setItem(21, createItem(Material.RED_STAINED_GLASS_PANE, "&c-1 SP", List.of()));

        // Green Glass Panes on Right (Increase)
        inv.setItem(23, createItem(Material.GREEN_STAINED_GLASS_PANE, "&a+1 SP", List.of()));
        inv.setItem(24, createItem(Material.GREEN_STAINED_GLASS_PANE, "&a+5 SP", List.of()));

        // Action Buttons
        inv.setItem(40, createItem(Material.GREEN_WOOL, "&a&lSIMPAN &7(Terapkan ke Config)", Arrays.asList(
                "&7Klik untuk menyimpan nilai",
                "&e" + currentCost + " SP &7ke config.yml"
        )));
        inv.setItem(49, createItem(Material.RED_WOOL, "&c&lBATAL", Arrays.asList(
                "&7Kembali tanpa menyimpan."
        )));

        player.openInventory(inv);
    }

    /**
     * Handles logic for player purchasing/unlocking a skill branch.
     */
    public void purchaseSkill(String categoryId, String branchId) {
        FileConfiguration config = plugin.getConfigManager().getConfig();
        String path = "categories." + categoryId + ".branches." + branchId;
        String skillName = config.getString(path + ".name", branchId);
        int cost = config.getInt(path + ".cost", 0);
        String prereq = config.getString(path + ".prerequisite", "");

        String fullKey = categoryId + "." + branchId;

        // Check already unlocked
        if (playerData.isSkillUnlocked(fullKey)) {
            player.sendMessage(plugin.getConfigManager().getMessage("skill-already-unlocked"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        // Check prerequisite
        if (!prereq.isEmpty()) {
            String prereqFullKey = categoryId + "." + prereq;
            if (!playerData.isSkillUnlocked(prereqFullKey)) {
                String prereqName = config.getString("categories." + categoryId + ".branches." + prereq + ".name", prereq);
                player.sendMessage(plugin.getConfigManager().getMessage("prerequisite-missing")
                        .replace("%prerequisite%", prereqName));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }
        }

        // Check points
        if (playerData.getPoints() < cost) {
            player.sendMessage(plugin.getConfigManager().getMessage("not-enough-points")
                    .replace("%cost%", String.valueOf(cost))
                    .replace("%points%", String.valueOf(playerData.getPoints())));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        // Purchase success
        if (playerData.removePoints(cost)) {
            playerData.unlockSkill(fullKey);
            plugin.getPlayerManager().savePlayerData(player.getUniqueId());

            // Run effects
            List<String> effects = config.getStringList(path + ".effects");
            plugin.getEffectEngine().executeEffects(player, effects);

            player.sendMessage(plugin.getConfigManager().getMessage("skill-unlocked")
                    .replace("%skill%", skillName));

            // Refresh GUI
            refreshCurrentGui();
        }
    }

    /**
     * Handles clicking in Admin Cost Editor Submenu
     */
    public void handleAdminCostClick(String categoryId, String branchId, int slot) {
        UUID uuid = player.getUniqueId();
        int currentCost = activeCostEdits.getOrDefault(uuid, 0);

        if (slot == 21) { // -1 SP
            activeCostEdits.put(uuid, Math.max(0, currentCost - 1));
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            refreshCurrentGui();
        } else if (slot == 20) { // -5 SP
            activeCostEdits.put(uuid, Math.max(0, currentCost - 5));
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            refreshCurrentGui();
        } else if (slot == 23) { // +1 SP
            activeCostEdits.put(uuid, currentCost + 1);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            refreshCurrentGui();
        } else if (slot == 24) { // +5 SP
            activeCostEdits.put(uuid, currentCost + 5);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            refreshCurrentGui();
        } else if (slot == 40) { // SAVE & EXIT
            // Write to config.yml
            FileConfiguration config = plugin.getConfigManager().getConfig();
            config.set("categories." + categoryId + ".branches." + branchId + ".cost", currentCost);
            plugin.getConfigManager().saveConfig();
            plugin.getConfigManager().reloadConfigs();

            activeCostEdits.remove(uuid);
            player.sendMessage(ConfigManager.color("&a&l[NaturalSkill] &fBiaya skill berhasil disimpan!"));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

            // Go back to admin category menu
            openAdminCategoryMenu(categoryId);
        } else if (slot == 49) { // CANCEL
            activeCostEdits.remove(uuid);
            player.playSound(player.getLocation(), Sound.BLOCK_DISPENSER_FAIL, 1.0f, 1.0f);
            openAdminCategoryMenu(categoryId);
        }
    }

    /* Helper Methods */

    private void fillDecorations(Inventory inv) {
        String fillerMat = plugin.getConfigManager().getConfig().getString("gui.decorations.filler.material", "GRAY_STAINED_GLASS_PANE");
        String fillerName = plugin.getConfigManager().getConfig().getString("gui.decorations.filler.name", " ");
        ItemStack filler = createItem(getMaterial(fillerMat, Material.GRAY_STAINED_GLASS_PANE), fillerName, List.of());

        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, filler);
        }
    }

    private ItemStack getBackButton() {
        String mat = plugin.getConfigManager().getConfig().getString("gui.decorations.back_button.material", "ARROW");
        String name = plugin.getConfigManager().getConfig().getString("gui.decorations.back_button.name", "&cBack");
        return createItem(getMaterial(mat, Material.ARROW), name, List.of());
    }

    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ConfigManager.color(name));
            if (lore != null && !lore.isEmpty()) {
                meta.setLore(ConfigManager.color(lore));
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private Material getMaterial(String name, Material fallback) {
        try {
            return Material.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid material name in config: " + name + ". Falling back to " + fallback.name());
            return fallback;
        }
    }
}
