package de.jeff_media.chestsort.handlers;

import de.jeff_media.chestsort.ChestSortPlugin;
import de.jeff_media.chestsort.api.ChestSortEvent;
import de.jeff_media.chestsort.api.ChestSortPostSortEvent;
import de.jeff_media.chestsort.data.Category;
import de.jeff_media.chestsort.data.CategoryLinePair;
import de.jeff_media.chestsort.utils.EnchantmentUtils;
import de.jeff_media.chestsort.utils.TypeMatchPositionPair;
import de.jeff_media.chestsort.utils.Utils;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public class ChestSortOrganizer {

    static final String[] COLORS = {"white", "orange", "magenta", "light_blue", "light_gray", "yellow", "lime", "pink",
            "gray", "cyan", "purple", "blue", "brown", "green", "red", "black"};
    static final String[] WOOD_NAMES = {"acacia", "birch", "jungle", "oak", "spruce", "dark_oak"};

    private static final int MAX_INVENTORY_SIZE = 54;
    private static final int PLAYER_INV_START_SLOT = 9;
    private static final int PLAYER_INV_END_SLOT = 35;
    private static final String EMPTY_PLACEHOLDER = "~";

    public final List<Category> categories = new ArrayList<>();
    final ChestSortPlugin plugin;
    final List<String> stickyCategoryNames = new ArrayList<>();
    private final WeakHashMap<Inventory, Void> sortableInventories = new WeakHashMap<>();
    private final WeakHashMap<Inventory, Void> unsortableInventories = new WeakHashMap<>();

    public ChestSortOrganizer(ChestSortPlugin plugin) {
        this.plugin = plugin;

        File categoriesFolder = new File(plugin.getDataFolder(), "categories");
        File[] categoryFiles = categoriesFolder.listFiles((directory, fileName) ->
                fileName.matches("(?i)^\\d\\d\\d.*\\.txt$"));

        if (categoryFiles != null) {
            for (File file : categoryFiles) {
                if (!file.isFile()) {
                    continue;
                }
                String categoryName = file.getName().replaceFirst("\\.txt$", "");
                try {
                    Category category = new Category(categoryName, loadCategoryFile(file));
                    categories.add(category);
                    if (plugin.isDebug()) {
                        plugin.getLogger().info("Loaded category file " + file.getName() + " (" + category.typeMatches.length + " items)");
                    }
                } catch (IOException e) {
                    plugin.getLogger().warning("Could not load category file: " + file.getName());
                }
            }
        }

        for (String categoryName : stickyCategoryNames) {
            for (Category category : categories) {
                if (categoryName.equalsIgnoreCase(category.name)) {
                    category.setSticky();
                }
            }
        }
    }

    static String getColorOrdered(String color) {
        return switch (color) {
            case "white" -> "01_white";
            case "light_gray" -> "02_light_gray";
            case "gray" -> "03_gray";
            case "black" -> "04_black";
            case "brown" -> "05_brown";
            case "red" -> "06_red";
            case "orange" -> "07_orange";
            case "yellow" -> "08_yellow";
            case "lime" -> "09_lime";
            case "green" -> "10_green";
            case "cyan" -> "11_cyan";
            case "light_blue" -> "12_light_blue";
            case "blue" -> "13_blue";
            case "magenta" -> "14_magenta";
            case "purple" -> "15_purple";
            case "pink" -> "16_pink";
            default -> "";
        };
    }

    public void setSortable(Inventory inv) {
        sortableInventories.put(inv, null);
    }

    public void setUnsortable(Inventory inv) {
        unsortableInventories.put(inv, null);
    }

    public boolean isMarkedAsSortable(Inventory inv) {
        return sortableInventories.containsKey(inv);
    }

    TypeMatchPositionPair[] loadCategoryFile(File file) throws IOException {
        boolean appendLineNumber = false;
        List<TypeMatchPositionPair> lines = new ArrayList<>();
        short lineNumber = 1;

        for (String rawLine : Files.readAllLines(file.toPath())) {
            String line = rawLine.trim().replace(" ", "");
            int hashIndex = line.indexOf('#');
            if (hashIndex >= 0) {
                line = line.substring(0, hashIndex);
            }
            if (!line.isEmpty()) {
                if (line.toLowerCase().startsWith("sticky=")) {
                    if (line.toLowerCase().endsWith("=true")) {
                        appendLineNumber = true;
                        makeCategoryStickyByFileName(file.getName());
                        if (plugin.isDebug()) {
                            plugin.getLogger().info("Sticky set to true in " + file.getName());
                        }
                    }
                } else {
                    lines.add(new TypeMatchPositionPair(line, lineNumber, appendLineNumber));
                    if (plugin.isDebug()) {
                        plugin.getLogger().info("Added typeMatch to category file: " + line);
                    }
                }
            }
            lineNumber++;
        }
        return lines.toArray(new TypeMatchPositionPair[0]);
    }

    private void makeCategoryStickyByFileName(String name) {
        stickyCategoryNames.add(name.replaceFirst("\\.txt$", ""));
    }

    String[] getTypeAndColor(String rawTypeName) {
        String typeName = rawTypeName.toLowerCase();
        String color = plugin.isDebug() ? "~color~" : EMPTY_PLACEHOLDER;

        for (String c : COLORS) {
            if (typeName.startsWith(c + "_")) {
                typeName = typeName.substring(c.length() + 1);
                color = getColorOrdered(c);
                break;
            }
        }

        for (String wood : WOOD_NAMES) {
            if (typeName.equals(wood + "_wood")) {
                typeName = "log_wood";
                color = wood;
            } else if (typeName.startsWith(wood + "_")) {
                typeName = typeName.substring(wood.length() + 1);
                color = wood;
            } else if (typeName.equals("stripped_" + wood + "_log")) {
                typeName = "log_stripped";
                color = wood;
            } else if (typeName.equals("stripped_" + wood + "_wood")) {
                typeName = "log_wood_stripped";
                color = wood;
            }
        }

        if (typeName.equals("log")) {
            typeName = "log_a";
        }

        if (typeName.endsWith("_egg")) {
            typeName = "egg_" + typeName.substring(0, typeName.length() - "_egg".length());
        }

        if (typeName.startsWith("polished_")) {
            typeName = typeName.substring("polished_".length()) + "_polished";
        }

        if (typeName.equals("wet_sponge")) {
            typeName = "sponge_wet";
        }

        if (typeName.equals("carved_pumpkin")) {
            typeName = "pumpkin_carved";
        }

        if (typeName.endsWith("helmet")) {
            typeName = typeName.replaceFirst("helmet$", "1_helmet");
        } else if (typeName.endsWith("chestplate")) {
            typeName = typeName.replaceFirst("chestplate$", "2_chestplate");
        } else if (typeName.endsWith("leggings")) {
            typeName = typeName.replaceFirst("leggings$", "3_leggings");
        } else if (typeName.endsWith("boots")) {
            typeName = typeName.replaceFirst("boots$", "4_boots");
        }

        if (typeName.endsWith("horse_armor")) {
            typeName = "horse_armor_" + typeName.substring(0, typeName.length() - "_horse_armor".length());
        }

        return new String[]{typeName, color};
    }

    public CategoryLinePair getCategoryLinePair(String typeName) {
        String lowerTypeName = typeName.toLowerCase();
        for (Category category : categories) {
            short matchingLineNumber = category.matches(lowerTypeName);
            if (matchingLineNumber != 0) {
                return new CategoryLinePair(category.name, matchingLineNumber);
            }
        }
        return new CategoryLinePair(plugin.isDebug() ? "~category~" : EMPTY_PLACEHOLDER, (short) 0);
    }

    public Map<String, String> getSortableMap(ItemStack item) {
        if (item == null) {
            return new HashMap<>();
        }

        char blocksFirst = item.getType().isBlock() ? '!' : '#';
        char itemsFirst = item.getType().isBlock() ? '#' : '!';

        String[] typeAndColor = getTypeAndColor(item.getType().name());
        String typeName = typeAndColor[0];
        String color = typeAndColor[1];
        String potionEffect = ",";

        if (item.getItemMeta() instanceof PotionMeta potionMeta) {
            PotionType baseType = potionMeta.getBasePotionType();
            if (baseType != null) {
                potionEffect = "|" + baseType.getKey().getKey();
            }
        }

        String categoryLookupName = item.getType().name();
        CategoryLinePair categoryLinePair = getCategoryLinePair(categoryLookupName);
        String categoryName = categoryLinePair.categoryName();
        String categorySticky = categoryName;
        String lineNumber = categoryLinePair.formattedPosition();
        if (stickyCategoryNames.contains(categoryName)) {
            categorySticky = categoryName + "~" + lineNumber;
        }

        String customName = plugin.isDebug() ? "~customName~" : EMPTY_PLACEHOLDER;
        String lore = plugin.isDebug() ? "~lore~" : EMPTY_PLACEHOLDER;
        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta.hasDisplayName() && meta.displayName() != null) {
                customName = PlainTextComponentSerializer.plainText().serialize(meta.displayName());
            }
            if (meta.hasLore() && meta.lore() != null && !meta.lore().isEmpty()) {
                lore = meta.lore().stream()
                        .map(PlainTextComponentSerializer.plainText()::serialize)
                        .reduce((a, b) -> a + "," + b)
                        .orElse(lore);
            }
        }

        typeName = typeName + EnchantmentUtils.getEnchantmentString(item);

        String tier = getTier(item);

        Map<String, String> sortableMap = new HashMap<>();
        sortableMap.put("{itemsFirst}", String.valueOf(itemsFirst));
        sortableMap.put("{blocksFirst}", String.valueOf(blocksFirst));
        sortableMap.put("{name}", typeName + potionEffect);
        sortableMap.put("{color}", color);
        sortableMap.put("{category}", categorySticky);
        sortableMap.put("{keepCategoryOrder}", lineNumber);
        sortableMap.put("{customName}", customName);
        sortableMap.put("{lore}", lore);
        sortableMap.put("{tier}", tier);
        return sortableMap;
    }

    private String getTier(ItemStack item) {
        String type = item.getType().name();
        if (type.contains("NETHERITE")) return "10netherite";
        if (type.contains("DIAMOND")) return "20diamond";
        if (type.contains("GOLD")) return "30gold";
        if (type.contains("IRON")) return "40iron";
        if (type.contains("STONE")) return "50stone";
        if (type.contains("WOOD")) return "60wood";
        return "99none";
    }

    String getSortableString(ItemStack item, Map<String, String> sortableMap) {
        String sortableString = plugin.getSortingMethod().replace(",", "|");
        for (Map.Entry<String, String> entry : sortableMap.entrySet()) {
            sortableString = sortableString.replace(entry.getKey(), entry.getValue());
        }
        return sortableString;
    }

    public void sortInventory(Inventory inv) {
        sortInventory(inv, 0, inv.getSize() - 1);
    }

    public void sortInventory(Inventory inv, int startSlot, int endSlot) {
        if (inv == null || unsortableInventories.containsKey(inv)) {
            return;
        }
        plugin.debug("Attempting to sort an Inventory and calling ChestSortEvent.");
        ChestSortEvent chestSortEvent = new ChestSortEvent(inv);

        try {
            if (inv.getLocation() != null) {
                chestSortEvent.setLocation(inv.getLocation());
            }
        } catch (Throwable ignored) {
        }

        InventoryHolder holder = inv.getHolder();
        if (holder instanceof HumanEntity humanEntity) {
            chestSortEvent.setPlayer(humanEntity);
        }

        Map<ItemStack, Map<String, String>> sortableMaps = new HashMap<>();
        for (ItemStack item : inv.getContents()) {
            sortableMaps.put(item, getSortableMap(item));
        }
        chestSortEvent.setSortableMaps(sortableMaps);

        Bukkit.getPluginManager().callEvent(chestSortEvent);
        if (chestSortEvent.isCancelled()) {
            plugin.debug("ChestSortEvent cancelled, I'll stay in bed.");
            return;
        }
        sortableMaps = chestSortEvent.getSortableMaps();

        List<Integer> unsortableSlots = new ArrayList<>();
        ItemStack[] items = inv.getContents();

        for (int i = 0; i < startSlot; i++) {
            items[i] = null;
        }
        for (int i = endSlot + 1; i < inv.getSize(); i++) {
            items[i] = null;
        }
        for (int i = startSlot; i <= endSlot; i++) {
            if (isOversizedStack(items[i]) || chestSortEvent.isUnmovable(i) || chestSortEvent.isUnmovable(items[i])) {
                items[i] = null;
                unsortableSlots.add(i);
            }
        }

        for (int i = startSlot; i <= endSlot; i++) {
            if (!unsortableSlots.contains(i)) {
                inv.clear(i);
            }
        }

        record SortableItem(ItemStack item, String sortKey) {
        }

        List<SortableItem> sortableItems = new ArrayList<>();
        for (ItemStack item : items) {
            if (item != null) {
                sortableItems.add(new SortableItem(item, getSortableString(item, sortableMaps.get(item))));
            }
        }
        sortableItems.sort(Comparator.comparing(SortableItem::sortKey));

        Inventory tempInventory = Bukkit.createInventory(null, MAX_INVENTORY_SIZE);
        for (SortableItem sortableItem : sortableItems) {
            if (plugin.isDebug()) {
                plugin.getLogger().info(sortableItem.sortKey());
            }
            tempInventory.addItem(sortableItem.item());
        }

        int currentSlot = startSlot;
        for (ItemStack item : tempInventory.getContents()) {
            if (item == null) {
                break;
            }
            while (unsortableSlots.contains(currentSlot) && currentSlot < endSlot) {
                currentSlot++;
            }
            inv.setItem(currentSlot, item);
            currentSlot++;
        }
        plugin.debug("Sorting successful. I'll go back to bed now.");

        Bukkit.getPluginManager().callEvent(new ChestSortPostSortEvent(chestSortEvent));
    }

    public void updateInventoryView(InventoryClickEvent event) {
        for (HumanEntity viewer : event.getViewers()) {
            if (viewer instanceof Player player) {
                player.updateInventory();
            }
        }
    }

    public void updateInventoryView(Inventory inventory) {
        for (HumanEntity viewer : inventory.getViewers()) {
            if (viewer instanceof Player player) {
                player.updateInventory();
            }
        }
    }

    public boolean isOversizedStack(ItemStack item) {
        return item != null && item.getAmount() > 64;
    }

    private boolean doesInventoryContain(Inventory inv, Material mat) {
        for (ItemStack item : Utils.getStorageContents(inv)) {
            if (item != null && item.getType() == mat) {
                return true;
            }
        }
        return false;
    }

    public void stuffInventoryIntoAnother(Inventory source, Inventory destination, Inventory origSource, boolean onlyMatchingStuff) {
        ItemStack[] hotbarStuff = new ItemStack[9];
        boolean destinationIsPlayerInventory = destination.getHolder() instanceof Player && destination.getType() == InventoryType.PLAYER;

        if (destinationIsPlayerInventory) {
            for (int i = 0; i < 9; i++) {
                hotbarStuff[i] = destination.getItem(i);
                destination.setItem(i, getPlaceholderBlock());
            }
        }

        List<ItemStack> leftovers = new ArrayList<>();

        for (int i = 0; i < source.getSize(); i++) {
            ItemStack current = source.getItem(i);
            if (current == null) continue;
            if (onlyMatchingStuff && !doesInventoryContain(destination, current.getType())) continue;
            if (isOversizedStack(current)) continue;
            source.clear(i);
            leftovers.addAll(destination.addItem(current).values());
        }

        origSource.addItem(leftovers.toArray(new ItemStack[0]));

        if (destinationIsPlayerInventory) {
            for (int i = 0; i < 9; i++) {
                destination.setItem(i, hotbarStuff[i]);
            }
        }

        updateInventoryView(destination);
        updateInventoryView(source);
    }

    private ItemStack getPlaceholderBlock() {
        return new ItemStack(Material.BARRIER, 64);
    }

    public void stuffPlayerInventoryIntoAnother(PlayerInventory source, Inventory destination, boolean onlyMatchingStuff, ChestSortEvent chestSortEvent) {
        boolean destinationIsShulkerBox = destination.getType() == InventoryType.SHULKER_BOX;
        Inventory temp = Bukkit.createInventory(null, MAX_INVENTORY_SIZE);

        for (int i = PLAYER_INV_START_SLOT; i <= PLAYER_INV_END_SLOT; i++) {
            ItemStack currentItem = source.getItem(i);
            if (currentItem == null) continue;
            if (chestSortEvent.isUnmovable(i) || chestSortEvent.isUnmovable(currentItem)) continue;
            if (destinationIsShulkerBox && currentItem.getType().name().endsWith("SHULKER_BOX")) continue;
            if (isOversizedStack(currentItem)) continue;
            if (onlyMatchingStuff && !doesInventoryContain(destination, currentItem.getType())) continue;

            temp.addItem(currentItem);
            source.clear(i);
        }
        stuffInventoryIntoAnother(temp, destination, source, false);
    }
}
