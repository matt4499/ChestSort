package de.jeff_media.chestsort.data;

import de.jeff_media.chestsort.ChestSortPlugin;
import de.jeff_media.chestsort.utils.SchedulerUtils;
import org.bukkit.inventory.Inventory;

public class PlayerSetting {

    public boolean sortingEnabled;
    public boolean invSortingEnabled;
    public boolean middleClick;
    public boolean shiftClick;
    public boolean doubleClick;
    public boolean shiftRightClick;
    public boolean leftClick;
    public boolean rightClick;
    public boolean leftClickOutside;
    public boolean hasSeenMessage;
    public boolean changed;
    public Inventory guiInventory = null;

    DoubleClickType currentDoubleClick = DoubleClickType.NONE;

    public enum DoubleClickType {
        NONE, RIGHT_CLICK, LEFT_CLICK
    }

    public PlayerSetting(boolean sortingEnabled, boolean invSortingEnabled, boolean middleClick, boolean shiftClick,
                          boolean doubleClick, boolean shiftRightClick, boolean leftClick, boolean rightClick,
                          boolean leftClickOutside, boolean changed, boolean hasSeenMessage) {
        this.sortingEnabled = sortingEnabled;
        this.invSortingEnabled = invSortingEnabled;
        this.middleClick = middleClick;
        this.shiftClick = shiftClick;
        this.doubleClick = doubleClick;
        this.shiftRightClick = shiftRightClick;
        this.leftClick = leftClick;
        this.rightClick = rightClick;
        this.leftClickOutside = leftClickOutside;
        this.changed = changed;
        this.hasSeenMessage = hasSeenMessage;
    }

    public DoubleClickType getCurrentDoubleClick(ChestSortPlugin plugin, DoubleClickType click) {
        if (click == DoubleClickType.NONE) {
            return DoubleClickType.NONE;
        }
        if (currentDoubleClick == click) {
            currentDoubleClick = DoubleClickType.NONE;
            return click;
        }
        currentDoubleClick = click;
        SchedulerUtils.runTaskLater(null, () -> currentDoubleClick = DoubleClickType.NONE, 10);
        return DoubleClickType.NONE;
    }

    public void toggleMiddleClick() {
        middleClick = !middleClick;
        changed = true;
    }

    public void toggleShiftClick() {
        shiftClick = !shiftClick;
        changed = true;
    }

    public void toggleDoubleClick() {
        doubleClick = !doubleClick;
        changed = true;
    }

    public void toggleShiftRightClick() {
        shiftRightClick = !shiftRightClick;
        changed = true;
    }

    public void toggleLeftClickOutside() {
        leftClickOutside = !leftClickOutside;
        changed = true;
    }

    public void toggleLeftClick() {
        leftClick = !leftClick;
        changed = true;
    }

    public void toggleRightClick() {
        rightClick = !rightClick;
        changed = true;
    }

    public void enableChestSorting() {
        sortingEnabled = true;
        changed = true;
    }

    public void disableChestSorting() {
        sortingEnabled = false;
        changed = true;
    }

    public void toggleChestSorting() {
        sortingEnabled = !sortingEnabled;
        changed = true;
    }

    public void enableInvSorting() {
        invSortingEnabled = true;
        changed = true;
    }

    public void disableInvSorting() {
        invSortingEnabled = false;
        changed = true;
    }

    public void toggleInvSorting() {
        invSortingEnabled = !invSortingEnabled;
        changed = true;
    }
}
