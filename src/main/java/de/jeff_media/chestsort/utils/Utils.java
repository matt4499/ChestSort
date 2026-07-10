package de.jeff_media.chestsort.utils;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public final class Utils {

    private Utils() {
    }

    public static ItemStack[] getStorageContents(Inventory inv) {
        return inv.getStorageContents();
    }

    public static String shortToStringWithLeadingZeroes(short number) {
        return String.format("%05d", number);
    }
}
