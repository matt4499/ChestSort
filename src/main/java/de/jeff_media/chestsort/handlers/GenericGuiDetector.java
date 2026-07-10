package de.jeff_media.chestsort.handlers;

import de.jeff_media.chestsort.ChestSortPlugin;
import org.bukkit.inventory.InventoryHolder;

public final class GenericGuiDetector {

    private final ChestSortPlugin plugin;

    public GenericGuiDetector(ChestSortPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isPluginGui(InventoryHolder holder) {
        if (holder == null) {
            return plugin.getConfig().getBoolean("prevent-sorting-null-inventories");
        }
        String className = holder.getClass().getName().toLowerCase();
        if (className.contains("gui") || className.contains("menu")) {
            plugin.debug("Generic GUI detected by class name containing \"gui\" or \"menu\": " + className);
            return true;
        }
        return false;
    }
}
