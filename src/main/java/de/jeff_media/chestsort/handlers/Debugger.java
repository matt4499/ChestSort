package de.jeff_media.chestsort.handlers;

import de.jeff_media.chestsort.ChestSortPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class Debugger implements Listener {

    private final ChestSortPlugin plugin;

    public Debugger(ChestSortPlugin plugin) {
        plugin.getLogger().warning("=======================================");
        plugin.getLogger().warning("    CHESTSORT DEBUG MODE ACTIVATED!");
        plugin.getLogger().warning("Only use this for development purposes!");
        plugin.getLogger().warning("=======================================");
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClickEvent(InventoryClickEvent event) {
        if (!plugin.isDebug()) {
            return;
        }
        plugin.debug(" ");
        plugin.debug("InventoryClickEvent:");
        plugin.debug("- Holder: " + event.getInventory().getHolder());
        if (event.getInventory().getHolder() != null) {
            plugin.debug("- Holder class: " + event.getInventory().getHolder().getClass());
        }
        plugin.debug("- Slot: " + event.getRawSlot());
        plugin.debug("- Left-Click: " + event.isLeftClick());
        plugin.debug("- Right-Click: " + event.isRightClick());
        plugin.debug("- Shift-Click: " + event.isShiftClick());
        plugin.debug(" ");
    }
}
