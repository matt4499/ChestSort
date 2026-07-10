package de.jeff_media.chestsort.gui;

import de.jeff_media.chestsort.ChestSortPlugin;
import de.jeff_media.chestsort.data.PlayerSetting;
import de.jeff_media.chestsort.gui.tracker.CustomGUITracker;
import de.jeff_media.chestsort.gui.tracker.CustomGUIType;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class GUIListener implements Listener {

    private static final ChestSortPlugin plugin = ChestSortPlugin.getInstance();

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (CustomGUITracker.getType(event.getView()) == CustomGUIType.NEW) {
            event.setCancelled(true);
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        PlayerSetting setting = plugin.getPlayerSetting(player);
        ItemMeta meta = clicked.getItemMeta();

        String function = meta.getPersistentDataContainer().getOrDefault(new NamespacedKey(plugin, "function"), PersistentDataType.STRING, "");
        List<String> userCommands = meta.getPersistentDataContainer().getOrDefault(new NamespacedKey(plugin, "user-commands"), PersistentDataType.LIST.strings(), List.of());
        List<String> adminCommands = meta.getPersistentDataContainer().getOrDefault(new NamespacedKey(plugin, "admin-commands"), PersistentDataType.LIST.strings(), List.of());

        executeCommands(player, player, userCommands);
        executeCommands(player, Bukkit.getConsoleSender(), adminCommands);

        switch (function) {
            case "leftclick" -> setting.toggleLeftClick();
            case "rightclick" -> setting.toggleRightClick();
            case "shiftclick" -> setting.toggleShiftClick();
            case "middleclick" -> setting.toggleMiddleClick();
            case "shiftrightclick" -> setting.toggleShiftRightClick();
            case "doubleclick" -> setting.toggleDoubleClick();
            case "outside" -> setting.toggleLeftClickOutside();
            case "autosorting" -> setting.toggleChestSorting();
            case "autoinvsorting" -> setting.toggleInvSorting();
            default -> {
                return;
            }
        }

        new NewUI(player).showGUI();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClose(InventoryCloseEvent event) {
        CustomGUITracker.close(event.getView());
    }

    private void executeCommands(Player player, CommandSender sender, List<String> commands) {
        for (String command : commands) {
            plugin.getServer().dispatchCommand(sender, command.replace("{player}", player.getName()));
        }
    }
}
