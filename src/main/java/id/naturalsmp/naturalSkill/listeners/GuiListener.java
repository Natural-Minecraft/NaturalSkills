package id.naturalsmp.naturalSkill.listeners;

import id.naturalsmp.naturalSkill.NaturalSkill;
import id.naturalsmp.naturalSkill.gui.SkillGui;
import id.naturalsmp.naturalSkill.gui.SkillInventoryHolder;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

public class GuiListener implements Listener {

    private final NaturalSkill plugin;

    public GuiListener(NaturalSkill plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        // Cancel any drag action inside plugin GUIs to prevent item exploit tricks
        if (event.getInventory().getHolder() instanceof SkillInventoryHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        // Clean up the temporary cost edit map when a player closes the GUI (e.g. presses ESC or disconnects)
        // This prevents memory leaks from the static activeCostEdits map in SkillGui
        if (event.getInventory().getHolder() instanceof SkillInventoryHolder) {
            SkillGui.clearCostEdit(event.getPlayer().getUniqueId());
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (!(event.getInventory().getHolder() instanceof SkillInventoryHolder)) return;

        event.setCancelled(true); // Prevent item stealing / moving

        Player player = (Player) event.getWhoClicked();
        SkillInventoryHolder holder = (SkillInventoryHolder) event.getInventory().getHolder();
        int slot = event.getRawSlot();

        // Click outside inventory bounds
        if (slot < 0 || slot >= event.getInventory().getSize()) return;

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        SkillGui gui = new SkillGui(plugin, player);
        String menuType = holder.getMenuType();
        FileConfiguration config = plugin.getConfigManager().getConfig();

        if (menuType.equals("main")) {
            // Main menu: click back button (slot 49)
            if (slot == 49) {
                player.closeInventory();
                return;
            }
            // Check skill shop button click (slot 31)
            if (slot == 31) {
                gui.openShopMenu(1);
                return;
            }
            // Check bakat button click (slot 40)
            if (slot == 40) {
                gui.openBakatMenu();
                return;
            }
            // Main menu: click category
            ConfigurationSection categories = config.getConfigurationSection("categories");
            if (categories != null) {
                for (String catId : categories.getKeys(false)) {
                    if (categories.getInt(catId + ".slot") == slot) {
                        gui.openCategoryMenu(catId, 1);
                        return;
                    }
                }
            }
        } else if (menuType.equals("bakat")) {
            // Bakat menu: click back button (slot 49)
            if (slot == 49) {
                gui.openMainMenu();
                return;
            }
        } else if (menuType.equals("category")) {
            String catId = holder.getCategoryId();
            // Category menu: click back button (slot 49)
            if (slot == 49) {
                gui.openMainMenu();
                return;
            }
            // Pagination check
            if (slot == 27) { // prev page
                int page = holder.getPage();
                if (page > 1) {
                    gui.openCategoryMenu(catId, page - 1);
                }
                return;
            }
            if (slot == 35) { // next page
                int page = holder.getPage();
                if (page < 14) {
                    gui.openCategoryMenu(catId, page + 1);
                }
                return;
            }
        } else if (menuType.equals("shop")) {
            // Shop menu: click back button (slot 49)
            if (slot == 49) {
                gui.openMainMenu();
                return;
            }
            // Pagination check
            if (slot == 27) { // prev page
                int page = holder.getPage();
                if (page > 1) {
                    gui.openShopMenu(page - 1);
                }
                return;
            }
            if (slot == 35) { // next page
                int page = holder.getPage();
                List<SkillGui.ShopItemRecord> allItems = gui.getAllShopItems();
                if (page * 28 < allItems.size()) {
                    gui.openShopMenu(page + 1);
                }
                return;
            }

            // Handle buyable skill click
            int index = SkillGui.CONTENT_SLOTS.indexOf(slot);
            if (index != -1) {
                int page = holder.getPage();
                int itemIndex = (page - 1) * 28 + index;
                List<SkillGui.ShopItemRecord> allItems = gui.getAllShopItems();
                if (itemIndex >= 0 && itemIndex < allItems.size()) {
                    SkillGui.ShopItemRecord item = allItems.get(itemIndex);
                    gui.purchaseSkill(item.getCategoryId(), item.getBranchId());
                }
            }
        } else if (menuType.equals("admin_main")) {
            // Admin main menu: click category
            ConfigurationSection categories = config.getConfigurationSection("categories");
            if (categories != null) {
                for (String catId : categories.getKeys(false)) {
                    if (categories.getInt(catId + ".slot") == slot) {
                        gui.openAdminCategoryMenu(catId);
                        return;
                    }
                }
            }
        } else if (menuType.equals("admin_category")) {
            String catId = holder.getCategoryId();
            // Admin category menu: click branch or back button (slot 49)
            if (slot == 49) {
                gui.openAdminMenu();
                return;
            }

            ConfigurationSection branches = config.getConfigurationSection("categories." + catId + ".branches");
            if (branches != null) {
                for (String branchId : branches.getKeys(false)) {
                    if (branches.getInt(branchId + ".slot") == slot) {
                        gui.openAdminCostEditorMenu(catId, branchId);
                        return;
                    }
                }
            }
        } else if (menuType.equals("admin_cost_edit")) {
            String catId = holder.getCategoryId();
            String branchId = holder.getBranchId();
            gui.handleAdminCostClick(catId, branchId, slot);
        }
    }
}
