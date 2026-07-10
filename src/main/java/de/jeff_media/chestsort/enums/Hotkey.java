package de.jeff_media.chestsort.enums;

import de.jeff_media.chestsort.ChestSortPlugin;
import de.jeff_media.chestsort.data.PlayerSetting;
import org.bukkit.entity.Player;

import java.util.Locale;

public enum Hotkey {

    AUTO_SORT, AUTO_INV_SORT,
    SHIFT_CLICK, MIDDLE_CLICK, DOUBLE_CLICK, SHIFT_RIGHT_CLICK,
    OUTSIDE, LEFT_CLICK, RIGHT_CLICK;

    public boolean hasPermission(Player player) {
        if (!player.hasPermission(getPermission(this))) {
            return false;
        }
        var config = ChestSortPlugin.getInstance().getConfig();
        return switch (this) {
            case AUTO_SORT -> config.getBoolean("allow-automatic-sorting");
            case AUTO_INV_SORT -> config.getBoolean("allow-automatic-inventory-sorting");
            case SHIFT_CLICK, MIDDLE_CLICK, DOUBLE_CLICK, SHIFT_RIGHT_CLICK -> config.getBoolean("allow-sorting-hotkeys");
            case LEFT_CLICK, RIGHT_CLICK -> config.getBoolean("allow-additional-hotkeys");
            case OUTSIDE -> config.getBoolean("allow-left-click-to-sort");
        };
    }

    public static String getPermission(Hotkey hotkey) {
        if (hotkey == AUTO_SORT) {
            return "chestsort.use";
        }
        if (hotkey == AUTO_INV_SORT) {
            return "chestsort.use.inventory";
        }
        return "chestsort.hotkey." + hotkey.name().toLowerCase(Locale.ROOT).replace("_", "");
    }

    public static Hotkey fromPermission(String permission) {
        if (permission == null) {
            return null;
        }
        return switch (permission) {
            case "shiftclick" -> SHIFT_CLICK;
            case "middleclick" -> MIDDLE_CLICK;
            case "doubleclick" -> DOUBLE_CLICK;
            case "shiftrightclick" -> SHIFT_RIGHT_CLICK;
            case "leftclick" -> LEFT_CLICK;
            case "rightclick" -> RIGHT_CLICK;
            case "outside" -> OUTSIDE;
            case "autosorting" -> AUTO_SORT;
            case "autoinvsorting" -> AUTO_INV_SORT;
            default -> null;
        };
    }

    public boolean hasEnabled(Player player) {
        PlayerSetting setting = ChestSortPlugin.getInstance().getPlayerSetting(player);
        return switch (this) {
            case SHIFT_CLICK -> setting.shiftClick;
            case MIDDLE_CLICK -> setting.middleClick;
            case DOUBLE_CLICK -> setting.doubleClick;
            case SHIFT_RIGHT_CLICK -> setting.shiftRightClick;
            case LEFT_CLICK -> setting.leftClick;
            case RIGHT_CLICK -> setting.rightClick;
            case OUTSIDE -> setting.leftClickOutside;
            case AUTO_INV_SORT -> setting.invSortingEnabled;
            case AUTO_SORT -> setting.sortingEnabled;
        };
    }
}
