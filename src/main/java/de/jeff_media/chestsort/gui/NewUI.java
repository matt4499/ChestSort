package de.jeff_media.chestsort.gui;

import de.jeff_media.chestsort.ChestSortPlugin;
import de.jeff_media.chestsort.enums.Hotkey;
import de.jeff_media.chestsort.gui.tracker.CustomGUITracker;
import de.jeff_media.chestsort.gui.tracker.CustomGUIType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class NewUI {

    private static final ChestSortPlugin plugin = ChestSortPlugin.getInstance();
    private final YamlConfiguration conf = plugin.getGuiConfig();
    private final Player player;

    public NewUI(Player player) {
        this.player = player;
    }

    private ItemStack getItem(int slot) {
        String slotPath = "slots." + slot;
        if (conf.isConfigurationSection(slotPath)) {
            return GuiItemFactory.fromConfigurationSection(conf.getConfigurationSection(slotPath));
        }
        if (!conf.isString(slotPath)) {
            return null;
        }

        String buttonName = conf.getString(slotPath);
        Hotkey key = Hotkey.fromPermission(buttonName);

        if (key != null && !key.hasPermission(player)) {
            buttonName = buttonName + "-nopermission";
        } else if (key != null) {
            buttonName = buttonName + (key.hasEnabled(player) ? "-enabled" : "-disabled");
        }

        plugin.debug("Button name: " + buttonName);
        ItemStack button = GuiItemFactory.fromConfigurationSection(conf.getConfigurationSection("items." + buttonName));
        if (button == null || !button.hasItemMeta() || buttonName.endsWith("-nopermission")) {
            return button;
        }

        ItemMeta meta = button.getItemMeta();
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "function"), PersistentDataType.STRING, buttonName.split("-")[0]);

        List<String> userCommands = conf.getStringList("items." + buttonName + ".commands.player");
        List<String> adminCommands = conf.getStringList("items." + buttonName + ".commands.console");
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "user-commands"), PersistentDataType.LIST.strings(), userCommands);
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "admin-commands"), PersistentDataType.LIST.strings(), adminCommands);
        button.setItemMeta(meta);

        return button;
    }

    public void showGUI() {
        int size = conf.getInt("size");
        Component title = MiniMessage.miniMessage().deserialize(conf.getString("title", ""));

        Inventory inv = Bukkit.createInventory(null, size, title);

        for (int i = 0; i < size; i++) {
            inv.setItem(i, getItem(i));
        }

        CustomGUITracker.open(player, inv, CustomGUIType.NEW);
    }
}
