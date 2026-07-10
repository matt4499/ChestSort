package de.jeff_media.chestsort.utils;

import org.bukkit.entity.ChestedHorse;
import org.bukkit.entity.Donkey;
import org.bukkit.entity.Llama;
import org.bukkit.entity.Mule;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class LlamaUtils {

    private LlamaUtils() {
    }

    public static int getLlamaChestSize(ChestedHorse llama) {
        if (llama == null || !llama.isCarryingChest()) {
            return -1;
        }
        if (llama instanceof Llama l) {
            return l.getStrength() * 3;
        }
        if (llama instanceof Donkey || llama instanceof Mule) {
            return 15;
        }
        return -1;
    }

    public static boolean belongsToLlama(Inventory inv, InventoryHolder holder) {
        return inv != null && holder instanceof ChestedHorse chestedHorse && chestedHorse.isCarryingChest();
    }
}
