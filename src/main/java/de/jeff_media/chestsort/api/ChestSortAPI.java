package de.jeff_media.chestsort.api;

import de.jeff_media.chestsort.ChestSortPlugin;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

public final class ChestSortAPI {

    private ChestSortAPI() {
    }

    public static void sortInventory(@NotNull Inventory inventory) {
        ChestSortPlugin.getInstance().getOrganizer().sortInventory(inventory);
    }

    public static void sortInventory(@NotNull Inventory inventory, int startSlot, int endSlot) {
        ChestSortPlugin.getInstance().getOrganizer().sortInventory(inventory, startSlot, endSlot);
    }

    public static boolean hasSortingEnabled(@NotNull Player player) {
        return ChestSortPlugin.getInstance().isSortingEnabled(player);
    }

    public static void setSortable(@NotNull Inventory inv) {
        ChestSortPlugin.getInstance().getOrganizer().setSortable(inv);
    }

    public static void setUnsortable(@NotNull Inventory inv) {
        ChestSortPlugin.getInstance().getOrganizer().setUnsortable(inv);
    }
}
