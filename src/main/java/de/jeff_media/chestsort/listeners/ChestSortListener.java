package de.jeff_media.chestsort.listeners;

import de.jeff_media.chestsort.ChestSortPlugin;
import de.jeff_media.chestsort.api.ChestSortEvent;
import de.jeff_media.chestsort.api.ChestSortPostSortEvent;
import de.jeff_media.chestsort.api.ISortable;
import de.jeff_media.chestsort.config.Messages;
import de.jeff_media.chestsort.data.PlayerSetting;
import de.jeff_media.chestsort.enums.Hotkey;
import de.jeff_media.chestsort.events.ChestSortLeftClickHotkeyEvent;
import de.jeff_media.chestsort.handlers.Logger;
import de.jeff_media.chestsort.utils.LlamaUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Barrel;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.Container;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.ChestedHorse;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ChestBoat;
import org.bukkit.entity.minecart.StorageMinecart;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChestSortListener implements org.bukkit.event.Listener {

    final ChestSortPlugin plugin;

    public ChestSortListener(ChestSortPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean isPossiblyBlacklisted(InventoryView view, InventoryHolder topHolder) {
        return isPossiblyBlacklisted(topHolder, view.getBottomInventory().getHolder());
    }

    private boolean isPossiblyBlacklisted(InventoryHolder topHolder, InventoryHolder bottomHolder) {
        Set<String> toCheck = new HashSet<>();
        if (topHolder != null) {
            toCheck.add(topHolder.getClass().getName());
        }
        if (bottomHolder != null) {
            toCheck.add(bottomHolder.getClass().getName());
        }

        for (String className : toCheck) {
            for (Pattern pattern : plugin.blacklistedInventoryHolderClassNames) {
                Matcher matcher = pattern.matcher(className);
                if (matcher.matches()) {
                    plugin.debug("Blacklisted holder found: " + className);
                    return true;
                }
            }
        }
        return false;
    }

    @EventHandler
    public void onLeftClickChest(PlayerInteractEvent event) {
        if (event instanceof ChestSortLeftClickHotkeyEvent) {
            return;
        }
        if (plugin.getDisabledWorlds().contains(event.getPlayer().getWorld().getName().toLowerCase())) {
            return;
        }
        if (!event.getPlayer().hasPermission("chestsort.use")) {
            return;
        }
        if (!event.getPlayer().hasPermission(Hotkey.getPermission(Hotkey.OUTSIDE))) {
            return;
        }
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (event.getAction() != Action.LEFT_CLICK_BLOCK) {
            return;
        }
        if (!plugin.getConfig().getBoolean("allow-left-click-to-sort")) {
            return;
        }
        Block clickedBlock = event.getClickedBlock();
        BlockState state = clickedBlock.getState();

        if (!(state instanceof Container container)) {
            return;
        }
        if (!belongsToChestLikeBlock(container.getInventory())) {
            return;
        }

        plugin.registerPlayerIfNeeded(event.getPlayer());
        PlayerSetting playerSetting = plugin.getPlayerSetting(event.getPlayer());
        if (!playerSetting.leftClickOutside) {
            return;
        }

        if (plugin.getConfig().getBoolean("mute-protection-plugins")) {
            if (!canBreak(event.getPlayer(), clickedBlock)) {
                return;
            }
        } else {
            ChestSortLeftClickHotkeyEvent testEvent = new ChestSortLeftClickHotkeyEvent(event.getPlayer(),
                    Action.RIGHT_CLICK_BLOCK,
                    event.getPlayer().getInventory().getItemInMainHand(),
                    clickedBlock,
                    BlockFace.UP,
                    EquipmentSlot.HAND);
            Bukkit.getPluginManager().callEvent(testEvent);
            if (testEvent.isCancelled() || testEvent.useInteractedBlock() == Event.Result.DENY) {
                return;
            }
        }

        plugin.getOrganizer().sortInventory(container.getInventory());
        event.getPlayer().sendActionBar(Messages.CONTAINER_SORTED);
    }

    private boolean canBreak(Player player, Block block) {
        BlockBreakEvent probe = new BlockBreakEvent(block, player);
        Bukkit.getPluginManager().callEvent(probe);
        return !probe.isCancelled();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.getPermissionsHandler().addPermissions(event.getPlayer());
        plugin.registerPlayerIfNeeded(event.getPlayer());
        plugin.getLgr().logPlayerJoin(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getPermissionsHandler().removePermissions(event.getPlayer());
        plugin.unregisterPlayer(event.getPlayer());
    }

    @EventHandler
    public void onPlayerInventoryClose(InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();
        InventoryHolder holder = inventory.getHolder();

        plugin.debug("Attempt to automatically sort a player inventory");

        if (isPossiblyBlacklisted(event.getView(), holder)) {
            plugin.debug("Abort: holder is blacklisted");
            return;
        }
        if (inventory.getType() != InventoryType.CRAFTING) {
            return;
        }
        if (!(holder instanceof Player p)) {
            return;
        }
        if (!plugin.getConfig().getBoolean("allow-automatic-inventory-sorting")) {
            return;
        }
        if (!p.hasPermission("chestsort.use.inventory") || !p.hasPermission("chestsort.automatic")) {
            return;
        }
        plugin.registerPlayerIfNeeded(p);

        PlayerSetting setting = plugin.getPerPlayerSettings().get(p.getUniqueId().toString());
        if (!setting.invSortingEnabled) {
            return;
        }

        plugin.getLgr().logSort(p, Logger.SortCause.INV_CLOSE);
        plugin.getOrganizer().sortInventory(p.getInventory(), 9, 35);
    }

    @EventHandler
    public void onChestClose(InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();

        if (!plugin.getConfig().getBoolean("allow-automatic-sorting")) {
            return;
        }

        InventoryHolder holder = inventory.getHolder();
        if (isPossiblyBlacklisted(event.getView(), holder)) {
            plugin.debug("Abort: holder is blacklisted");
            return;
        }

        String sortTime = plugin.getConfig().getString("sort-time");
        if (!("close".equalsIgnoreCase(sortTime) || "both".equalsIgnoreCase(sortTime))) {
            return;
        }
        if (!(event.getPlayer() instanceof Player p)) {
            return;
        }
        if (!p.hasPermission("chestsort.automatic")) {
            return;
        }
        if (!isAPICall(inventory, holder) && !belongsToChestLikeBlock(inventory, holder)
                && !LlamaUtils.belongsToLlama(inventory, holder)
                && !plugin.getOrganizer().isMarkedAsSortable(inventory)) {
            return;
        }
        if (!isReadyToSort(p)) {
            return;
        }

        plugin.getLgr().logSort(p, Logger.SortCause.CONT_CLOSE);

        if (LlamaUtils.belongsToLlama(inventory, holder)) {
            ChestedHorse llama = (ChestedHorse) holder;
            plugin.getOrganizer().sortInventory(inventory, 2, LlamaUtils.getLlamaChestSize(llama) + 1);
            return;
        }

        plugin.getOrganizer().sortInventory(inventory);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChestOpen(InventoryOpenEvent event) {
        Inventory inventory = event.getInventory();
        InventoryHolder holder = inventory.getHolder();

        if (isPossiblyBlacklisted(event.getView(), holder)) {
            plugin.debug("Abort: holder is blacklisted");
            return;
        }
        if (!plugin.getConfig().getBoolean("allow-automatic-sorting")) {
            return;
        }

        String sortTime = plugin.getConfig().getString("sort-time");
        if (!("open".equalsIgnoreCase(sortTime) || "both".equalsIgnoreCase(sortTime))) {
            return;
        }
        if (event.isCancelled()) {
            return;
        }
        if (!(event.getPlayer() instanceof Player p)) {
            return;
        }
        if (!p.hasPermission("chestsort.automatic")) {
            return;
        }
        if (!isAPICall(inventory, holder) && !belongsToChestLikeBlock(inventory, holder)
                && !LlamaUtils.belongsToLlama(inventory, holder)
                && !plugin.getOrganizer().isMarkedAsSortable(inventory)) {
            return;
        }
        if (!isReadyToSort(p)) {
            return;
        }

        plugin.getLgr().logSort(p, Logger.SortCause.CONT_OPEN);

        if (LlamaUtils.belongsToLlama(inventory, holder)) {
            ChestedHorse llama = (ChestedHorse) holder;
            plugin.getOrganizer().sortInventory(inventory, 2, LlamaUtils.getLlamaChestSize(llama) + 1);
            return;
        }

        plugin.getOrganizer().sortInventory(inventory);
    }

    private boolean belongsToChestLikeBlock(Inventory inventory) {
        return belongsToChestLikeBlock(inventory, inventory.getHolder());
    }

    private boolean belongsToChestLikeBlock(Inventory inventory, InventoryHolder holder) {
        if (inventory.getType() == InventoryType.ENDER_CHEST || inventory.getType() == InventoryType.SHULKER_BOX) {
            return true;
        }
        if (holder instanceof ChestBoat) {
            return true;
        }
        if (holder == null) {
            return false;
        }
        return holder instanceof Chest
                || holder instanceof DoubleChest
                || holder instanceof Barrel
                || holder instanceof ShulkerBox
                || holder instanceof StorageMinecart;
    }

    private boolean isReadyToSort(Player player) {
        if (!player.hasPermission("chestsort.use")) {
            return false;
        }
        if (plugin.getDisabledWorlds().contains(player.getWorld().getName().toLowerCase())) {
            return false;
        }
        if (player.getGameMode() == GameMode.SPECTATOR || player.getGameMode() == GameMode.ADVENTURE) {
            return false;
        }

        plugin.registerPlayerIfNeeded(player);
        PlayerSetting setting = plugin.getPerPlayerSettings().get(player.getUniqueId().toString());

        if (!plugin.isSortingEnabled(player)) {
            if (!setting.hasSeenMessage) {
                setting.hasSeenMessage = true;
                if (plugin.getConfig().getBoolean("show-message-when-using-chest")) {
                    player.sendMessage(Messages.COMMAND_HINT_ENABLE);
                }
            }
            return false;
        } else if (!setting.hasSeenMessage) {
            setting.hasSeenMessage = true;
            if (plugin.getConfig().getBoolean("show-message-when-using-chest-and-sorting-is-enabled")) {
                player.sendMessage(Messages.COMMAND_HINT_DISABLE);
            }
        }
        return true;
    }

    @EventHandler
    public void onEnderChestOpen(InventoryOpenEvent event) {
        if (!plugin.getConfig().getBoolean("allow-automatic-sorting")) {
            return;
        }

        Inventory inventory = event.getInventory();
        InventoryHolder holder = inventory.getHolder();

        if (isPossiblyBlacklisted(event.getView(), holder)) {
            plugin.debug("Abort: holder is blacklisted");
            return;
        }
        if (!(event.getPlayer() instanceof Player p)) {
            return;
        }
        if (!p.hasPermission("chestsort.automatic")) {
            return;
        }
        if (!inventory.equals(p.getEnderChest())) {
            return;
        }

        if (isReadyToSort(p)) {
            plugin.getLgr().logSort(p, Logger.SortCause.EC_OPEN);
            plugin.getOrganizer().sortInventory(inventory);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onHotkey(InventoryClickEvent event) {
        plugin.debug2("Hotkey?");
        Inventory inventory = event.getInventory();
        InventoryHolder topHolder = inventory.getHolder();
        Inventory clicked = event.getClickedInventory();

        if (isPossiblyBlacklisted(event.getView(), topHolder)) {
            plugin.debug("Abort: holder is blacklisted");
            return;
        }
        if (!(event.getWhoClicked() instanceof Player p)) {
            return;
        }

        plugin.registerPlayerIfNeeded(p);

        if (!plugin.getConfig().getBoolean("allow-sorting-hotkeys")) {
            return;
        }
        if (!p.hasPermission("chestsort.use") && !p.hasPermission("chestsort.use.inventory")) {
            return;
        }
        if (clicked == null) {
            return;
        }

        InventoryHolder holder = clicked.getHolder();
        boolean isAPICall = isAPICall(clicked, holder);

        if (!isAPICall && plugin.getGenericGuiDetector().isPluginGui(holder)) {
            plugin.debug("Aborting hotkey sorting: no API call & generic GUI detected");
            return;
        }
        if (!isAPICall && holder == p && clicked != p.getInventory()) {
            return;
        }

        PlayerSetting setting = plugin.getPerPlayerSettings().get(p.getUniqueId().toString());

        if (clicked == setting.guiInventory) {
            return;
        }
        if (inventory == setting.guiInventory) {
            event.setCancelled(true);
            return;
        }

        boolean sort = false;
        Logger.SortCause cause = null;

        switch (event.getClick()) {
            case MIDDLE -> {
                cause = Logger.SortCause.H_MIDDLE;
                if (setting.middleClick && p.hasPermission(Hotkey.getPermission(Hotkey.MIDDLE_CLICK))
                        && (event.getWhoClicked().getGameMode() != GameMode.CREATIVE
                        || event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR)) {
                    sort = true;
                }
            }
            case DOUBLE_CLICK -> {
                if (event.isShiftClick()) {
                    return;
                }
                cause = Logger.SortCause.H_DOUBLE;
                if (setting.doubleClick && p.hasPermission(Hotkey.getPermission(Hotkey.DOUBLE_CLICK))
                        && event.getCursor().getType() == Material.AIR) {
                    sort = true;
                }
            }
            case SHIFT_LEFT -> {
                cause = Logger.SortCause.H_SHIFT;
                if (setting.shiftClick && p.hasPermission(Hotkey.getPermission(Hotkey.SHIFT_CLICK))
                        && (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR)) {
                    sort = true;
                }
            }
            case SHIFT_RIGHT -> {
                cause = Logger.SortCause.H_SHIFTRIGHT;
                if (setting.shiftRightClick && p.hasPermission(Hotkey.getPermission(Hotkey.SHIFT_RIGHT_CLICK))
                        && (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR)) {
                    sort = true;
                }
            }
            default -> {
            }
        }

        if (!sort) {
            return;
        }
        event.setCancelled(true);

        if (plugin.isInHotkeyCooldown(p.getUniqueId())) {
            plugin.debug("Skipping: hotkey cooldown");
            return;
        }
        plugin.debug("Hotkey triggered: " + event.getClick().name());

        if (isAPICall || belongsToChestLikeBlock(clicked, holder) || clicked.getType() == InventoryType.HOPPER
                || plugin.getOrganizer().isMarkedAsSortable(clicked) || LlamaUtils.belongsToLlama(clicked, holder)) {

            if (!p.hasPermission("chestsort.use")) {
                return;
            }
            plugin.getLgr().logSort(p, cause);

            if (LlamaUtils.belongsToLlama(clicked, topHolder)) {
                ChestedHorse llama = (ChestedHorse) topHolder;
                plugin.getOrganizer().sortInventory(clicked, 2, LlamaUtils.getLlamaChestSize(llama) + 1);
                plugin.getOrganizer().updateInventoryView(event);
                return;
            }

            plugin.getOrganizer().sortInventory(clicked);
            plugin.getOrganizer().updateInventoryView(event);
        } else if (holder instanceof Player) {
            if (!p.hasPermission("chestsort.use.inventory")) {
                return;
            }
            if (event.getSlotType() == SlotType.QUICKBAR) {
                plugin.getLgr().logSort(p, cause);
                plugin.getOrganizer().sortInventory(p.getInventory(), 0, 8);
                plugin.getOrganizer().updateInventoryView(event);
            } else if (event.getSlotType() == SlotType.CONTAINER) {
                plugin.getLgr().logSort(p, cause);
                plugin.getOrganizer().sortInventory(p.getInventory(), 9, 35);
                plugin.getOrganizer().updateInventoryView(event);
            }
        }
    }

    private boolean isAPICall(Inventory inv, InventoryHolder holder) {
        if (inv == null) {
            return false;
        }
        return holder instanceof ISortable || plugin.getOrganizer().isMarkedAsSortable(inv);
    }

    @EventHandler
    public void onAdditionalHotkeys(InventoryClickEvent event) {
        HumanEntity whoClicked = event.getWhoClicked();
        Inventory clickedInventory = event.getClickedInventory();
        Inventory inventory = event.getInventory();
        InventoryHolder holder = inventory.getHolder();

        if (isPossiblyBlacklisted(event.getView(), holder)) {
            plugin.debug("Abort: holder is blacklisted");
            return;
        }
        if (!plugin.getConfig().getBoolean("allow-additional-hotkeys")) {
            return;
        }
        if (!(whoClicked instanceof Player player)) {
            return;
        }
        if (clickedInventory != null) {
            return;
        }
        if (event.getCursor().getType() != Material.AIR) {
            return;
        }
        if (LlamaUtils.belongsToLlama(inventory, holder)) {
            return;
        }
        if (holder == null && !event.getView().getTopInventory().equals(player.getEnderChest())) {
            return;
        }
        if (holder == player && inventory != player.getInventory()) {
            return;
        }
        if (inventory.getType() != InventoryType.CHEST
                && inventory.getType() != InventoryType.DISPENSER
                && inventory.getType() != InventoryType.DROPPER
                && inventory.getType() != InventoryType.ENDER_CHEST
                && inventory.getType() != InventoryType.HOPPER
                && inventory.getType() != InventoryType.SHULKER_BOX
                && !(holder instanceof Barrel)
                && inventory != player.getEnderChest()
                && !(holder instanceof ISortable)) {
            return;
        }
        if (!isAPICall(inventory, holder) && plugin.getGenericGuiDetector().isPluginGui(holder)) {
            return;
        }
        if (!player.hasPermission("chestsort.use")) {
            return;
        }

        plugin.registerPlayerIfNeeded(player);
        PlayerSetting setting = plugin.getPerPlayerSettings().get(player.getUniqueId().toString());

        ChestSortEvent chestSortEvent = new ChestSortEvent(inventory);
        chestSortEvent.setPlayer(whoClicked);
        chestSortEvent.setLocation(whoClicked.getLocation());

        Map<ItemStack, Map<String, String>> sortableMaps = new HashMap<>();
        ItemStack[] contents = inventory.getContents();
        for (ItemStack item : contents) {
            sortableMaps.put(item, plugin.getOrganizer().getSortableMap(item));
        }
        chestSortEvent.setSortableMaps(sortableMaps);

        Bukkit.getPluginManager().callEvent(chestSortEvent);
        if (chestSortEvent.isCancelled()) {
            return;
        }

        if (event.isLeftClick() && setting.leftClick && player.hasPermission(Hotkey.getPermission(Hotkey.LEFT_CLICK))) {
            plugin.getLgr().logSort(player, Logger.SortCause.H_LEFT);
            if (setting.getCurrentDoubleClick(plugin, PlayerSetting.DoubleClickType.LEFT_CLICK) == PlayerSetting.DoubleClickType.LEFT_CLICK) {
                plugin.getOrganizer().stuffPlayerInventoryIntoAnother(player.getInventory(), inventory, false, chestSortEvent);
                plugin.getOrganizer().sortInventory(inventory);
            } else {
                plugin.getOrganizer().stuffPlayerInventoryIntoAnother(player.getInventory(), inventory, true, chestSortEvent);
            }
        } else if (event.isRightClick() && setting.rightClick && player.hasPermission(Hotkey.getPermission(Hotkey.RIGHT_CLICK))) {
            plugin.getLgr().logSort(player, Logger.SortCause.H_RIGHT);
            if (setting.getCurrentDoubleClick(plugin, PlayerSetting.DoubleClickType.RIGHT_CLICK) == PlayerSetting.DoubleClickType.RIGHT_CLICK) {
                plugin.getOrganizer().stuffInventoryIntoAnother(inventory, player.getInventory(), inventory, false);
                plugin.getOrganizer().sortInventory(player.getInventory(), 9, 35);
            } else {
                plugin.getOrganizer().stuffInventoryIntoAnother(inventory, player.getInventory(), inventory, true);
            }
        }

        plugin.getOrganizer().updateInventoryView(inventory);
        plugin.getOrganizer().updateInventoryView(player.getInventory());

        Bukkit.getPluginManager().callEvent(new ChestSortPostSortEvent(chestSortEvent));
    }
}
