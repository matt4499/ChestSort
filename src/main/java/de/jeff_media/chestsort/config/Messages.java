package de.jeff_media.chestsort.config;

import de.jeff_media.chestsort.ChestSortPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.FileConfiguration;

public final class Messages {

    public static Component CONTAINER_SORTED;
    public static Component ACTIVATED, DEACTIVATED, INV_ACTIVATED, INV_DEACTIVATED,
            COMMAND_HINT_ENABLE, COMMAND_HINT_DISABLE, PLAYERS_ONLY, PLAYER_INVENTORY_SORTED;

    public static Component GUI_ENABLED, GUI_DISABLED, GUI_MIDDLE_CLICK, GUI_SHIFT_CLICK, GUI_DOUBLE_CLICK,
            GUI_LEFT_CLICK_OUTSIDE, GUI_SHIFT_RIGHT_CLICK, GUI_LEFT_CLICK, GUI_RIGHT_CLICK;

    public static Component HOTKEYS_DISABLED = Component.text("[ChestSort] Hotkeys have been disabled by the admin.", NamedTextColor.RED);

    private static String invalidOptionsTemplate = "&cError: Unknown option %s. Valid options are %s.";

    private Messages() {
    }

    public static void reload() {
        FileConfiguration config = ChestSortPlugin.getInstance().getConfig();

        CONTAINER_SORTED = legacy(config.getString("message-container-sorted", "&aContainer sorted!"));
        ACTIVATED = legacy(config.getString("message-sorting-enabled", "&7Automatic chest sorting has been &aenabled&7.&r"));
        DEACTIVATED = legacy(config.getString("message-sorting-disabled", "&7Automatic chest sorting has been &cdisabled&7.&r"));
        INV_ACTIVATED = legacy(config.getString("message-inv-sorting-enabled", "&7Automatic inventory sorting has been &aenabled&7.&r"));
        INV_DEACTIVATED = legacy(config.getString("message-inv-sorting-disabled", "&7Automatic inventory sorting has been &cdisabled&7.&r"));
        COMMAND_HINT_ENABLE = legacy(config.getString("message-when-using-chest", "&7Hint: Type &6/chestsort&7 to enable automatic chest sorting."));
        COMMAND_HINT_DISABLE = legacy(config.getString("message-when-using-chest2", "&7Hint: Type &6/chestsort&7 to disable automatic chest sorting."));
        PLAYERS_ONLY = legacy(config.getString("message-error-players-only", "&cError: This command can only be run by players.&r"));
        PLAYER_INVENTORY_SORTED = legacy(config.getString("message-player-inventory-sorted", "&7Your inventory has been sorted."));

        GUI_ENABLED = legacy(config.getString("message-gui-enabled", "&aEnabled"));
        GUI_DISABLED = legacy(config.getString("message-gui-disabled", "&cDisabled"));
        GUI_MIDDLE_CLICK = legacy(config.getString("message-gui-middle-click", "Middle-Click"));
        GUI_SHIFT_CLICK = legacy(config.getString("message-gui-shift-click", "Shift + Click"));
        GUI_DOUBLE_CLICK = legacy(config.getString("message-gui-double-click", "Double-Click"));
        GUI_LEFT_CLICK_OUTSIDE = legacy(config.getString("message-gui-left-click-outside", "Left-Click"));
        GUI_SHIFT_RIGHT_CLICK = legacy(config.getString("message-gui-shift-right-click", "Shift + Right-Click"));
        GUI_LEFT_CLICK = legacy(config.getString("message-gui-left-click", "Fill Chest (Left-Click/Double-Left-Click)"));
        GUI_RIGHT_CLICK = legacy(config.getString("message-gui-right-click", "Unload Chest (Right-Click/Double-Right-Click)"));

        invalidOptionsTemplate = config.getString("message-error-invalid-options", "&cError: Unknown option %s. Valid options are %s.");
    }

    public static Component invalidOptions(String given, String validOptions) {
        String withGiven = replaceFirstPlaceholder(invalidOptionsTemplate, given);
        String withBoth = replaceFirstPlaceholder(withGiven, validOptions);
        return legacy(withBoth);
    }

    private static String replaceFirstPlaceholder(String template, String value) {
        int index = template.indexOf("%s");
        if (index < 0) {
            return template;
        }
        return template.substring(0, index) + value + template.substring(index + 2);
    }

    private static Component legacy(String text) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }
}
