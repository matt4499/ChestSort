package de.jeff_media.chestsort.api;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Public class that can be used as InventoryHolder to tell ChestSort that the associated inventory is sortable.
 */
public class Sortable implements ISortable {

    private Inventory inv;
    private InventoryHolder holder;

    public Sortable() {
    }

    public Sortable(@Nullable InventoryHolder holder) {
        this.holder = holder;
    }

    public void setHolder(@NotNull InventoryHolder holder) {
        this.holder = holder;
    }

    public void removeHolder() {
        this.holder = null;
    }

    @Nullable
    public InventoryHolder getHolder() {
        return holder;
    }

    @Override
    @Nullable
    public Inventory getInventory() {
        return inv;
    }

    public void setInventory(@NotNull Inventory inv) {
        this.inv = inv;
    }
}
