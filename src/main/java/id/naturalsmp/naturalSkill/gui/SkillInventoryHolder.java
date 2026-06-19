package id.naturalsmp.naturalSkill.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class SkillInventoryHolder implements InventoryHolder {

    private final String menuType; // "main", "category", "admin_main", "admin_category", "admin_cost_edit", "shop"
    private final String categoryId;
    private final String branchId; // for cost editor
    private int page = 1;
    private Inventory inventory;

    public SkillInventoryHolder(String menuType, String categoryId, String branchId) {
        this.menuType = menuType;
        this.categoryId = categoryId;
        this.branchId = branchId;
    }

    public SkillInventoryHolder(String menuType, String categoryId, String branchId, int page) {
        this.menuType = menuType;
        this.categoryId = categoryId;
        this.branchId = branchId;
        this.page = page;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public String getMenuType() {
        return menuType;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public String getBranchId() {
        return branchId;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }


    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
