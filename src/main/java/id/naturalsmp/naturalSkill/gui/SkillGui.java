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
                    case "bakat":
                        openBakatMenu();
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
        String title = ConfigManager.color(plugin.getConfigManager().getMessage("gui-title-main"));
        SkillInventoryHolder holder = new SkillInventoryHolder("main", null, null);
        Inventory inv = Bukkit.createInventory(holder, 54, title);
        holder.setInventory(inv);

        // Fill borders & AIR background
        fillMainMenuBorders(inv);

        // Player Info Book in slot 4 - shows levels as well
        inv.setItem(4, createItem(Material.BOOK, "&e&lStatistik Poin & Keahlian", Arrays.asList(
                "&7Pemain: &a" + player.getName(),
                "&7Skill Points: &e" + playerData.getPoints() + " SP",
                "",
                "&a&lLevel Keahlian Utama:",
                "&7- Strength: &eLvl " + playerData.getSkillLevel("strength"),
                "&7- Agility: &eLvl " + playerData.getSkillLevel("agility"),
                "&7- Intelligence: &eLvl " + playerData.getSkillLevel("intelligence"),
                "&7- Psychology: &eLvl " + playerData.getSkillLevel("psychology"),
                "&7- Communication: &eLvl " + playerData.getSkillLevel("communication")
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

        // Skill Shop Button in slot 31
        String shopName = "&b&lSkill Shop &7[Klik]";
        List<String> shopLore = Arrays.asList(
                "&7Toko pembelian skill pasif",
                "&7menggunakan Skill Points (SP) Anda.",
                "",
                "&eKlik untuk membuka Skill Shop!"
        );
        inv.setItem(31, createItem(Material.EMERALD, shopName, shopLore));

        // Bakat Button in slot 40
        String bakatMat = config.getString("gui.decorations.bakat_button.material", "GOLDEN_HOE");
        String bakatName = config.getString("gui.decorations.bakat_button.name", "&6&lKeahlian Bakat &7[Klik]");
        List<String> bakatLore = config.getStringList("gui.decorations.bakat_button.lore");
        if (bakatLore == null || bakatLore.isEmpty()) {
            bakatLore = Arrays.asList(
                    "&7Bakat khusus yang didapat dari",
                    "&7kegiatan sehari-hari (Tebang Kayu,",
                    "&7Menambang, Berenang, Memancing, dll).",
                    "",
                    "&eKlik untuk melihat perkembangan bakat!"
            );
        }
        inv.setItem(40, createItem(getMaterial(bakatMat, Material.GOLDEN_HOE), bakatName, bakatLore));

        // Back button in slot 49
        inv.setItem(49, getBackButton());

        player.openInventory(inv);
    }

    /**
     * 2. Player Category Submenu (Level Path Snake GUI 1-255)
     */
    public void openCategoryMenu(String categoryId, int page) {
        FileConfiguration config = plugin.getConfigManager().getConfig();
        String catName = config.getString("categories." + categoryId + ".name", categoryId);
        String titleTemplate = plugin.getConfigManager().getMessages().getString("gui-title-category", "&0Skill: %category%");
        String title = ConfigManager.color(titleTemplate
                .replace("%category%", catName)
                .replace("%prefix%", "")) + ConfigManager.color(" &7- Page " + page);

        SkillInventoryHolder holder = new SkillInventoryHolder("category", categoryId, null, page);
        Inventory inv = Bukkit.createInventory(holder, 54, title);
        holder.setInventory(inv);

        // Max levels is 255. Max pages is ceil(255 / 19.0) = 14
        int maxPage = 14;

        // Fill borders & AIR background
        fillOtherMenuBorders(inv, page, maxPage);

        // Player Stats in slot 4
        int playerLevel = playerData.getSkillLevel(categoryId);
        double playerXp = playerData.getSkillXp(categoryId);
        int reqXp = plugin.getProgressionManager().getRequiredXp(playerLevel);

        inv.setItem(4, createItem(Material.BOOK, "&e&lStatistik Level", Arrays.asList(
                "&7Pemain: &a" + player.getName(),
                "&7Skill: " + catName,
                "&7Level Saat Ini: &eLvl " + playerLevel,
                "&7Progress: &b" + String.format(Locale.US, "%.1f", playerXp) + " &7/ &b" + reqXp + " XP"
        )));

        // Back button in slot 49
        inv.setItem(49, getBackButton());

        // Fill snake path level items (19 levels per page)
        int startLvl = (page - 1) * 19 + 1;

        for (int i = 1; i <= 19; i++) {
            int levelNum = startLvl + i - 1;
            if (levelNum > 255) break;

            int slot = getSnakeSlot(i);
            if (slot == -1) continue;

            boolean isUnlocked = levelNum <= playerLevel;
            boolean isActive = levelNum == playerLevel + 1;

            Material material;
            String displayName;
            List<String> lore = new ArrayList<>();

            if (isUnlocked) {
                material = Material.LIME_STAINED_GLASS_PANE;
                displayName = "&a&lLevel " + levelNum + " &7[Terbuka]";
                lore.add("&7Status: &aTerbuka");
            } else if (isActive) {
                material = Material.YELLOW_STAINED_GLASS_PANE;
                displayName = "&e&lLevel " + levelNum + " &7[Progres Aktif]";
                lore.add("&7Status: &eProgres Aktif");
            } else {
                material = Material.RED_STAINED_GLASS_PANE;
                displayName = "&c&lLevel " + levelNum + " &7[Terkunci]";
                lore.add("&7Status: &cTerkunci");
            }

            lore.add("");
            lore.add("&aKeuntungan Pasif:");
            lore.addAll(getLevelBenefits(categoryId, levelNum));
            lore.add("");

            lore.add("&bProgress Ke Level Ini:");
            if (isUnlocked) {
                lore.add(getProgressBar(100, 100, 10, '■', "&a", "&7") + " &f(100.0%)");
            } else if (isActive) {
                double pct = (double) playerXp / reqXp * 100.0;
                lore.add(getProgressBar((int) playerXp, reqXp, 10, '■', "&a", "&7") + " &f(" + String.format(Locale.US, "%.1f", pct) + "%)");
            } else {
                lore.add(getProgressBar(0, 100, 10, '■', "&a", "&7") + " &f(0.0%)");
            }

            inv.setItem(slot, createItem(material, displayName, lore));
        }

        player.openInventory(inv);
    }

    public void openCategoryMenu(String categoryId) {
        openCategoryMenu(categoryId, 1);
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

            // Update live leaderboard score
            int newScore = plugin.getLeaderboardManager().calculateScore(playerData.getUnlockedSkills(), categoryId);
            plugin.getLeaderboardManager().updatePlayerScore(player.getUniqueId(), player.getName(), categoryId, newScore);

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

    /**
     * Player Bakat Submenu
     */
    public void openBakatMenu() {
        FileConfiguration config = plugin.getConfigManager().getConfig();
        String title = ConfigManager.color(config.getString("gui.bakat-title", "&0Bakat Pemain"));

        SkillInventoryHolder holder = new SkillInventoryHolder("bakat", null, null);
        Inventory inv = Bukkit.createInventory(holder, 54, title);
        holder.setInventory(inv);

        fillDecorations(inv);

        // Player stats in slot 4
        inv.setItem(4, createItem(Material.BOOK, "&e&lStatistik Bakat Pemain", Arrays.asList(
                "&7Pemain: &a" + player.getName(),
                "&7Tebang Pohon: &eLvl " + playerData.getBakatLevel("woodcutting"),
                "&7Nambang: &eLvl " + playerData.getBakatLevel("mining"),
                "&7Bertani: &eLvl " + playerData.getBakatLevel("farming"),
                "&7Memancing: &eLvl " + playerData.getBakatLevel("fishing"),
                "&7Berenang: &eLvl " + playerData.getBakatLevel("swimming")
        )));

        // Load bakat configurations
        ConfigurationSection bakatSec = config.getConfigurationSection("progression.bakat");
        if (bakatSec != null) {
            for (String key : bakatSec.getKeys(false)) {
                String name = bakatSec.getString(key + ".name", key);
                String materialName = bakatSec.getString(key + ".icon", "IRON_PICKAXE");
                Material material = getMaterial(materialName, Material.IRON_PICKAXE);
                int slot = bakatSec.getInt(key + ".slot", 22);

                int level = playerData.getBakatLevel(key);
                double xp = playerData.getBakatXp(key);
                int reqXp = plugin.getProgressionManager().getRequiredXp(level);
                int maxLevel = config.getInt("progression.bakat-max-level", 50);

                double percent = (double) xp / reqXp * 100.0;
                String percentStr = String.format(Locale.US, "%.1f", percent);

                // Progress Bar (10 characters)
                String progressBar = getProgressBar((int) xp, reqXp, 10, '■', "&a", "&7");

                List<String> rawLore = bakatSec.getStringList(key + ".lore");
                List<String> formattedLore = new ArrayList<>();
                for (String line : rawLore) {
                    formattedLore.add(line
                            .replace("%level%", String.valueOf(level))
                            .replace("%max_level%", String.valueOf(maxLevel))
                            .replace("%xp%", String.format(Locale.US, "%.1f", xp))
                            .replace("%req_xp%", String.valueOf(reqXp))
                            .replace("%percent%", percentStr)
                            .replace("%progress_bar%", progressBar));
                }

                inv.setItem(slot, createItem(material, name, formattedLore));
            }
        }

        // Back button in slot 49
        inv.setItem(49, getBackButton());

        player.openInventory(inv);
    }

    private String getProgressBar(int currentXp, int reqXp, int barLength, char symbol, String activeColor, String inactiveColor) {
        if (reqXp <= 0) return activeColor + String.valueOf(symbol).repeat(barLength);
        double percent = Math.min(1.0, (double) currentXp / reqXp);
        int activeCount = (int) (percent * barLength);
        int inactiveCount = barLength - activeCount;
        return activeColor + String.valueOf(symbol).repeat(activeCount) + inactiveColor + String.valueOf(symbol).repeat(inactiveCount);
    }

    /* NaturalSkills GUI Remake Helpers */

    private void fillMainMenuBorders(Inventory inv) {
        ItemStack bluePane = createItem(Material.BLUE_STAINED_GLASS_PANE, " ", List.of());
        
        // Row 1: AIR (slots 0-3, 5-8), Stats book in slot 4
        for (int i = 0; i <= 3; i++) inv.setItem(i, null);
        for (int i = 5; i <= 8; i++) inv.setItem(i, null);

        // Row 2: all blue stained glass panes (slots 9-17)
        for (int i = 9; i <= 17; i++) {
            inv.setItem(i, bluePane);
        }

        // Row 3-5: borders are blue stained glass panes
        inv.setItem(18, bluePane);
        inv.setItem(26, bluePane);
        inv.setItem(27, bluePane);
        inv.setItem(35, bluePane);
        inv.setItem(36, bluePane);
        inv.setItem(44, bluePane);

        // Row 6: slots 45-48, 50-53 are blue stained glass panes
        for (int i = 45; i <= 48; i++) {
            inv.setItem(i, bluePane);
        }
        for (int i = 50; i <= 53; i++) {
            inv.setItem(i, bluePane);
        }
    }

    private void fillOtherMenuBorders(Inventory inv, int page, int maxPage) {
        ItemStack bluePane = createItem(Material.BLUE_STAINED_GLASS_PANE, " ", List.of());

        // Row 1: slots 0-3 and 5-8 are blue stained glass panes
        for (int i = 0; i <= 3; i++) inv.setItem(i, bluePane);
        for (int i = 5; i <= 8; i++) inv.setItem(i, bluePane);

        // Row 2: slots 9 and 17 are blue
        inv.setItem(9, bluePane);
        inv.setItem(17, bluePane);

        // Row 3: slots 18 and 26 are blue
        inv.setItem(18, bluePane);
        inv.setItem(26, bluePane);

        // Row 4: slot 27 is Prev page, slot 35 is Next page
        ItemStack prevButton = createItem(Material.PAPER, "&a&l[Halaman Sebelumnya] &7Page " + (page - 1), List.of("&7Klik untuk kembali"));
        ItemStack nextButton = createItem(Material.PAPER, "&a&l[Halaman Berikutnya] &7Page " + (page + 1), List.of("&7Klik untuk lanjut"));
        
        if (page > 1) {
            inv.setItem(27, prevButton);
        } else {
            inv.setItem(27, bluePane);
        }

        if (page < maxPage) {
            inv.setItem(35, nextButton);
        } else {
            inv.setItem(35, bluePane);
        }

        // Row 5: slots 36 and 44 are blue
        inv.setItem(36, bluePane);
        inv.setItem(44, bluePane);

        // Row 6: slots 45-48 and 50-53 are blue
        for (int i = 45; i <= 48; i++) {
            inv.setItem(i, bluePane);
        }
        for (int i = 50; i <= 53; i++) {
            inv.setItem(i, bluePane);
        }
    }

    public static int getSnakeSlot(int relativeLevel) {
        switch (relativeLevel) {
            case 1: return 10;
            case 2: return 19;
            case 3: return 28;
            case 4: return 37;
            case 5: return 38;
            case 6: return 39;
            case 7: return 30;
            case 8: return 21;
            case 9: return 12;
            case 10: return 13;
            case 11: return 14;
            case 12: return 23;
            case 13: return 32;
            case 14: return 41;
            case 15: return 42;
            case 16: return 43;
            case 17: return 34;
            case 18: return 25;
            case 19: return 16;
            default: return -1;
        }
    }

    private List<String> getLevelBenefits(String categoryId, int levelNum) {
        List<String> benefits = new ArrayList<>();
        FileConfiguration config = plugin.getConfigManager().getConfig();

        // 1. Check per_level effects
        ConfigurationSection perLevelSec = config.getConfigurationSection("progression.skills." + categoryId + ".effects.per_level");
        if (perLevelSec != null) {
            for (String key : perLevelSec.getKeys(false)) {
                double perLvlVal = perLevelSec.getDouble(key);
                double totalVal = perLvlVal * levelNum;
                if (perLvlVal > 0) {
                    String displayName = formatAttributeName(key, totalVal);
                    benefits.add("&7- " + displayName);
                }
            }
        }

        // 2. Check milestone levels
        ConfigurationSection milestoneSec = config.getConfigurationSection("progression.skills." + categoryId + ".effects.levels." + levelNum);
        if (milestoneSec != null) {
            if (milestoneSec.contains("attributes")) {
                ConfigurationSection attrSec = milestoneSec.getConfigurationSection("attributes");
                if (attrSec != null) {
                    for (String key : attrSec.getKeys(false)) {
                        double val = attrSec.getDouble(key);
                        String displayName = formatAttributeName(key, val);
                        benefits.add("&e&l[Milestone] &a" + displayName);
                    }
                }
            }
            if (milestoneSec.contains("message")) {
                String msg = milestoneSec.getString("message");
                if (msg != null && !msg.isEmpty()) {
                    benefits.add("&e&l[Reward]: &f" + msg);
                }
            }
        }

        if (benefits.isEmpty()) {
            benefits.add("&7- &oTidak ada efek pasif khusus");
        }

        return benefits;
    }

    private String formatAttributeName(String rawKey, double value) {
        String prettyName = rawKey.toUpperCase();
        if (rawKey.equalsIgnoreCase("damage_percent")) {
            return "+" + String.format(Locale.US, "%.1f", value * 100.0) + "% Attack Damage";
        }
        if (rawKey.equalsIgnoreCase("damage_flat")) {
            return "+" + String.format(Locale.US, "%.1f", value) + " Attack Damage";
        }
        if (rawKey.equalsIgnoreCase("GENERIC_MAX_HEALTH")) {
            return "+" + String.format(Locale.US, "%.1f", value / 2.0) + " Hearts"; // 2.0 = 1 heart
        }
        if (rawKey.equalsIgnoreCase("GENERIC_MOVEMENT_SPEED")) {
            return "+" + String.format(Locale.US, "%.1f", value * 1000.0) + "% Speed";
        }
        return "+" + value + " " + prettyName;
    }

    public static final List<Integer> CONTENT_SLOTS = Arrays.asList(
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34,
        37, 38, 39, 40, 41, 42, 43
    );

    public static class ShopItemRecord {
        private final String categoryId;
        private final String branchId;

        public ShopItemRecord(String categoryId, String branchId) {
            this.categoryId = categoryId;
            this.branchId = branchId;
        }

        public String getCategoryId() {
            return categoryId;
        }

        public String getBranchId() {
            return branchId;
        }
    }

    public List<ShopItemRecord> getAllShopItems() {
        List<ShopItemRecord> list = new ArrayList<>();
        FileConfiguration config = plugin.getConfigManager().getConfig();
        ConfigurationSection categories = config.getConfigurationSection("categories");
        if (categories != null) {
            for (String catId : categories.getKeys(false)) {
                ConfigurationSection branches = config.getConfigurationSection("categories." + catId + ".branches");
                if (branches != null) {
                    for (String branchId : branches.getKeys(false)) {
                        list.add(new ShopItemRecord(catId, branchId));
                    }
                }
            }
        }
        return list;
    }

    public void openShopMenu(int page) {
        String title = ConfigManager.color("&0Skill Shop &7- Page " + page);
        SkillInventoryHolder holder = new SkillInventoryHolder("shop", null, null, page);
        Inventory inv = Bukkit.createInventory(holder, 54, title);
        holder.setInventory(inv);

        List<ShopItemRecord> allItems = getAllShopItems();
        int maxPage = (int) Math.ceil(allItems.size() / 28.0);
        if (maxPage < 1) maxPage = 1;

        fillOtherMenuBorders(inv, page, maxPage);

        // Player Stats in slot 4
        inv.setItem(4, createItem(Material.BOOK, "&e&lStatistik Poin", Arrays.asList(
                "&7Pemain: &a" + player.getName(),
                "&7Skill Points: &e" + playerData.getPoints() + " SP",
                "&7Gunakan SP untuk membeli skill di bawah!"
        )));

        // Back button in slot 49
        inv.setItem(49, getBackButton());

        // Fill content
        int startIdx = (page - 1) * 28;
        FileConfiguration config = plugin.getConfigManager().getConfig();

        for (int i = 0; i < 28; i++) {
            int itemIdx = startIdx + i;
            if (itemIdx >= allItems.size()) break;

            ShopItemRecord shopItem = allItems.get(itemIdx);
            String catId = shopItem.getCategoryId();
            String branchId = shopItem.getBranchId();

            String path = "categories." + catId + ".branches." + branchId;
            String name = config.getString(path + ".name", branchId);
            String materialName = config.getString(path + ".icon", "ENCHANTED_BOOK");
            Material material = getMaterial(materialName, Material.ENCHANTED_BOOK);
            int cost = config.getInt(path + ".cost", 0);
            String prereq = config.getString(path + ".prerequisite", "");

            // Status string translation with dynamic color fixing
            String status;
            String fullKey = catId + "." + branchId;
            if (playerData.isSkillUnlocked(fullKey)) {
                status = ConfigManager.color(plugin.getConfigManager().getMessage("gui-unlocked-status"));
            } else if (!prereq.isEmpty() && !playerData.isSkillUnlocked(catId + "." + prereq)) {
                status = ConfigManager.color(plugin.getConfigManager().getMessage("gui-locked-status"));
            } else if (playerData.getPoints() >= cost) {
                status = ConfigManager.color(plugin.getConfigManager().getMessage("gui-purchasable-status"));
            } else {
                status = ConfigManager.color(plugin.getConfigManager().getMessage("gui-locked-status")) + " &c(Poin kurang)";
            }

            List<String> rawLore = config.getStringList(path + ".lore");
            List<String> formattedLore = new ArrayList<>();
            for (String line : rawLore) {
                formattedLore.add(ConfigManager.color(line
                        .replace("%cost%", String.valueOf(cost))
                        .replace("%status%", status)));
            }

            int slot = CONTENT_SLOTS.get(i);
            inv.setItem(slot, createItem(material, name, formattedLore));
        }

        player.openInventory(inv);
    }
}
