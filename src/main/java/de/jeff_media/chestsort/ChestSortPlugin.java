package de.jeff_media.chestsort;

import de.jeff_media.chestsort.commands.ChestSortCommand;
import de.jeff_media.chestsort.commands.InvSortCommand;
import de.jeff_media.chestsort.commands.TabCompleter;
import de.jeff_media.chestsort.config.ConfigUpdater;
import de.jeff_media.chestsort.config.Messages;
import de.jeff_media.chestsort.data.Category;
import de.jeff_media.chestsort.data.PlayerSetting;
import de.jeff_media.chestsort.gui.GUIListener;
import de.jeff_media.chestsort.handlers.ChestSortOrganizer;
import de.jeff_media.chestsort.handlers.ChestSortPermissionsHandler;
import de.jeff_media.chestsort.handlers.Debugger;
import de.jeff_media.chestsort.handlers.GenericGuiDetector;
import de.jeff_media.chestsort.handlers.Logger;
import de.jeff_media.chestsort.listeners.ChestSortListener;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

public final class ChestSortPlugin extends JavaPlugin {

    private static ChestSortPlugin instance;

    public ChestSortOrganizer organizer;
    public final List<Pattern> blacklistedInventoryHolderClassNames = new ArrayList<>();

    private GenericGuiDetector genericGuiDetector;
    private boolean debug = false;
    private List<String> disabledWorlds = new ArrayList<>();
    private final Map<UUID, Long> hotkeyCooldown = new HashMap<>();
    private Logger lgr;
    private ChestSortListener chestSortListener;
    private Map<String, PlayerSetting> perPlayerSettings = new HashMap<>();
    private ChestSortPermissionsHandler permissionsHandler;
    private String sortingMethod;
    private boolean usingMatchingConfig = true;
    private boolean verbose = true;
    private YamlConfiguration guiConfig = new YamlConfiguration();
    private int settingsFingerprint = 0;

    public static ChestSortPlugin getInstance() {
        return instance;
    }

    public YamlConfiguration getGuiConfig() {
        return guiConfig;
    }

    void createConfig() {
        saveDefaultConfig();
        createGUIConfig();
        reloadConfig();

        setDisabledWorlds(getConfig().getStringList("disabled-worlds"));

        ConfigUpdater.updateConfig();

        createDirectories();

        setDefaultConfigValues();
    }

    private void createGUIConfig() {
        File guiFile = new File(getDataFolder(), "gui.yml");
        if (!guiFile.exists()) {
            saveResource("gui.yml", false);
        }
        guiConfig = YamlConfiguration.loadConfiguration(guiFile);
    }

    private void createDirectories() {
        File categoriesFolder = new File(getDataFolder(), "categories");
        if (!categoriesFolder.exists()) {
            categoriesFolder.mkdir();
        }
    }

    public void debug(String message) {
        if (isDebug()) {
            getLogger().warning("[DEBUG] " + message);
        }
    }

    public void debug2(String message) {
        if (getConfig().getBoolean("debug2")) {
            getLogger().warning("[DEBUG2] " + message);
        }
    }

    void dump() {
        File file = new File(getDataFolder(), "dump.csv");
        try (BufferedWriter bw = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            for (Material mat : Material.values()) {
                bw.write(mat.name() + "," + getOrganizer().getCategoryLinePair(mat.name()).categoryName());
                bw.newLine();
            }
        } catch (IOException e) {
            getLogger().warning("Could not write dump.csv: " + e.getMessage());
        }
    }

    private String getCategoryList() {
        Category[] categories = getOrganizer().categories.toArray(new Category[0]);
        Arrays.sort(categories);
        StringBuilder list = new StringBuilder();
        for (Category category : categories) {
            list.append(category.name).append(" (").append(category.typeMatches.length).append("), ");
        }
        if (list.length() > 2) {
            list.setLength(list.length() - 2);
        }
        return list.toString();
    }

    public List<String> getDisabledWorlds() {
        return disabledWorlds;
    }

    public void setDisabledWorlds(List<String> disabledWorlds) {
        this.disabledWorlds = disabledWorlds == null ? new ArrayList<>() : disabledWorlds;
    }

    public GenericGuiDetector getGenericGuiDetector() {
        return genericGuiDetector;
    }

    public Map<UUID, Long> getHotkeyCooldown() {
        return hotkeyCooldown;
    }

    public Logger getLgr() {
        return lgr;
    }

    public void setLgr(Logger lgr) {
        this.lgr = lgr;
    }

    public ChestSortListener getListener() {
        return chestSortListener;
    }

    public void setListener(ChestSortListener chestSortListener) {
        this.chestSortListener = chestSortListener;
    }

    public ChestSortOrganizer getOrganizer() {
        return organizer;
    }

    public void setOrganizer(ChestSortOrganizer organizer) {
        this.organizer = organizer;
    }

    public Map<String, PlayerSetting> getPerPlayerSettings() {
        return perPlayerSettings;
    }

    public void setPerPlayerSettings(Map<String, PlayerSetting> perPlayerSettings) {
        this.perPlayerSettings = perPlayerSettings;
    }

    public ChestSortPermissionsHandler getPermissionsHandler() {
        return permissionsHandler;
    }

    public void setPermissionsHandler(ChestSortPermissionsHandler permissionsHandler) {
        this.permissionsHandler = permissionsHandler;
    }

    public PlayerSetting getPlayerSetting(Player p) {
        registerPlayerIfNeeded(p);
        return getPerPlayerSettings().get(p.getUniqueId().toString());
    }

    public String getSortingMethod() {
        return sortingMethod;
    }

    public void setSortingMethod(String sortingMethod) {
        this.sortingMethod = sortingMethod;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public boolean isInHotkeyCooldown(UUID uuid) {
        double cooldown = getConfig().getDouble("hotkey-cooldown") * 1000;
        if (cooldown == 0) {
            return false;
        }
        long lastUsage = getHotkeyCooldown().getOrDefault(uuid, 0L);
        long currentTime = System.currentTimeMillis();
        long difference = currentTime - lastUsage;
        getHotkeyCooldown().put(uuid, currentTime);
        debug("Difference: " + difference);
        return difference <= cooldown;
    }

    public boolean isSortingEnabled(Player p) {
        registerPlayerIfNeeded(p);
        return getPerPlayerSettings().get(p.getUniqueId().toString()).sortingEnabled;
    }

    public boolean isUsingMatchingConfig() {
        return usingMatchingConfig;
    }

    public void setUsingMatchingConfig(boolean usingMatchingConfig) {
        this.usingMatchingConfig = usingMatchingConfig;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public void load(boolean reload) {
        settingsFingerprint = 0;
        File fingerprintFile = new File(getDataFolder(), "settings.fingerprint");
        if (fingerprintFile.exists()) {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(fingerprintFile);
            settingsFingerprint = yaml.getInt("v", 0);
        }

        if (reload) {
            unregisterAllPlayers();
            reloadConfig();
        }

        createConfig();
        setDebug(getConfig().getBoolean("debug"));

        HandlerList.unregisterAll(this);

        if (isDebug()) {
            getServer().getPluginManager().registerEvents(new Debugger(this), this);
        }

        genericGuiDetector = new GenericGuiDetector(this);

        saveDefaultCategories();

        blacklistedInventoryHolderClassNames.clear();
        for (String line : getConfig().getStringList("blocked-inventory-holders-regex")) {
            try {
                blacklistedInventoryHolderClassNames.add(Pattern.compile(line));
            } catch (Exception e) {
                getLogger().warning("Invalid regex in blocked-inventory-holders-regex: " + line);
            }
        }

        setVerbose(getConfig().getBoolean("verbose"));
        setLgr(new Logger(this, getConfig().getBoolean("log")));
        Messages.reload();
        setOrganizer(new ChestSortOrganizer(this));
        setListener(new ChestSortListener(this));
        setPermissionsHandler(new ChestSortPermissionsHandler(this));
        setSortingMethod(getConfig().getString("sorting-method"));

        getServer().getPluginManager().registerEvents(getListener(), this);
        getServer().getPluginManager().registerEvents(new GUIListener(), this);

        ChestSortCommand chestSortCommandExecutor = new ChestSortCommand(this);
        TabCompleter tabCompleter = new TabCompleter();
        getCommand("sort").setExecutor(chestSortCommandExecutor);
        getCommand("sort").setTabCompleter(tabCompleter);

        InvSortCommand invSortCommandExecutor = new InvSortCommand(this);
        getCommand("isort").setExecutor(invSortCommandExecutor);
        getCommand("isort").setTabCompleter(tabCompleter);

        if (isVerbose()) {
            getLogger().info("Use permissions: " + getConfig().getBoolean("use-permissions"));
            getLogger().info("Current sorting method: " + getSortingMethod());
            getLogger().info("Allow automatic chest sorting: " + getConfig().getBoolean("allow-automatic-sorting"));
            getLogger().info("  |- Chest sorting enabled by default: " + getConfig().getBoolean("sorting-enabled-by-default"));
            getLogger().info("  |- Sort time: " + getConfig().getString("sort-time"));
            getLogger().info("Allow automatic inventory sorting: " + getConfig().getBoolean("allow-automatic-inventory-sorting"));
            getLogger().info("  |- Inventory sorting enabled by default: " + getConfig().getBoolean("inv-sorting-enabled-by-default"));
            getLogger().info("Auto generate category files: " + getConfig().getBoolean("auto-generate-category-files"));
            getLogger().info("Allow hotkeys: " + getConfig().getBoolean("allow-sorting-hotkeys"));
            if (getConfig().getBoolean("allow-sorting-hotkeys")) {
                getLogger().info("Hotkeys enabled by default:");
                getLogger().info("  |- Middle-Click: " + getConfig().getBoolean("sorting-hotkeys.middle-click"));
                getLogger().info("  |- Shift-Click: " + getConfig().getBoolean("sorting-hotkeys.shift-click"));
                getLogger().info("  |- Double-Click: " + getConfig().getBoolean("sorting-hotkeys.double-click"));
                getLogger().info("  |- Shift-Right-Click: " + getConfig().getBoolean("sorting-hotkeys.shift-right-click"));
            }
            getLogger().info("Allow additional hotkeys: " + getConfig().getBoolean("allow-additional-hotkeys"));
            if (getConfig().getBoolean("allow-additional-hotkeys")) {
                getLogger().info("Additional hotkeys enabled by default:");
                getLogger().info("  |- Left-Click: " + getConfig().getBoolean("additional-hotkeys.left-click"));
                getLogger().info("  |- Right-Click: " + getConfig().getBoolean("additional-hotkeys.right-click"));
            }
            getLogger().info("Categories: " + getCategoryList());
        }

        if (getConfig().getBoolean("dump")) {
            dump();
        }

        for (Player p : getServer().getOnlinePlayers()) {
            getPermissionsHandler().addPermissions(p);
        }
    }

    @Override
    public void onDisable() {
        for (Player player : getServer().getOnlinePlayers()) {
            unregisterPlayer(player);
            getPermissionsHandler().removePermissions(player);
        }
    }

    @Override
    public void onEnable() {
        instance = this;
        load(false);
    }

    public void incrementFingerprint() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("v", settingsFingerprint + 1);
        settingsFingerprint++;
        try {
            yaml.save(new File(getDataFolder(), "settings.fingerprint"));
            load(true);
        } catch (IOException e) {
            getLogger().warning("Could not save settings.fingerprint: " + e.getMessage());
        }
    }

    public void registerPlayerIfNeeded(Player p) {
        UUID uniqueId = p.getUniqueId();
        if (getPerPlayerSettings().containsKey(uniqueId.toString())) {
            return;
        }

        String fingerprint = getFingerprint();
        PersistentDataContainer pdc = p.getPersistentDataContainer();

        boolean sortingEnabled = getStoredBoolean(pdc, "sortingEnabled" + fingerprint, getConfig().getBoolean("sorting-enabled-by-default"));
        boolean invSortingEnabled = getStoredBoolean(pdc, "invSortingEnabled" + fingerprint, getConfig().getBoolean("inv-sorting-enabled-by-default"));
        boolean middleClick = getStoredBoolean(pdc, "middleClick" + fingerprint, getConfig().getBoolean("sorting-hotkeys.middle-click"));
        boolean shiftClick = getStoredBoolean(pdc, "shiftClick" + fingerprint, getConfig().getBoolean("sorting-hotkeys.shift-click"));
        boolean doubleClick = getStoredBoolean(pdc, "doubleClick" + fingerprint, getConfig().getBoolean("sorting-hotkeys.double-click"));
        boolean shiftRightClick = getStoredBoolean(pdc, "shiftRightClick" + fingerprint, getConfig().getBoolean("sorting-hotkeys.shift-right-click"));
        boolean leftClick = getStoredBoolean(pdc, "leftClick" + fingerprint, getConfig().getBoolean("additional-hotkeys.left-click"));
        boolean rightClick = getStoredBoolean(pdc, "rightClick" + fingerprint, getConfig().getBoolean("additional-hotkeys.right-click"));
        boolean leftClickOutside = getStoredBoolean(pdc, "leftClickOutside" + fingerprint, getConfig().getBoolean("left-click-to-sort-enabled-by-default"));
        boolean hasSeenMessage = !getConfig().getBoolean("show-message-again-after-logout")
                && getStoredBoolean(pdc, "hasSeenMessage" + fingerprint, false);

        PlayerSetting settings = new PlayerSetting(sortingEnabled, invSortingEnabled, middleClick, shiftClick, doubleClick,
                shiftRightClick, leftClick, rightClick, leftClickOutside, true, hasSeenMessage);

        getPerPlayerSettings().put(uniqueId.toString(), settings);
    }

    private boolean getStoredBoolean(PersistentDataContainer pdc, String key, boolean fallback) {
        Boolean stored = pdc.get(new NamespacedKey(this, key), PersistentDataType.BOOLEAN);
        return stored != null ? stored : fallback;
    }

    private void setStoredBoolean(PersistentDataContainer pdc, String key, boolean value) {
        pdc.set(new NamespacedKey(this, key), PersistentDataType.BOOLEAN, value);
    }

    private String getFingerprint() {
        return settingsFingerprint > 0 ? "-" + settingsFingerprint : "";
    }

    private void saveDefaultCategories() {
        if (!getConfig().getBoolean("auto-generate-category-files", true)) {
            return;
        }

        String[] defaultCategories = {"900-weapons", "905-common-tools", "907-other-tools", "909-food", "910-valuables",
                "920-armor-and-arrows", "930-brewing", "950-redstone", "960-wood", "970-stone", "980-plants", "981-corals",
                "_ReadMe - Category files"};

        File categoriesFolder = new File(getDataFolder(), "categories");
        File[] existingDefaultFiles = categoriesFolder.listFiles((directory, fileName) ->
                fileName.endsWith(".txt") && fileName.matches("(?i)9\\d\\d.*\\.txt$"));

        if (existingDefaultFiles != null) {
            for (File file : existingDefaultFiles) {
                boolean stillShipped = Arrays.stream(defaultCategories).anyMatch(name -> (name + ".txt").equalsIgnoreCase(file.getName()));
                if (!stillShipped) {
                    file.delete();
                    getLogger().warning("Deleting deprecated default category file " + file.getName());
                }
            }
        }

        for (String category : defaultCategories) {
            try (InputStream in = getClass().getResourceAsStream("/categories/" + category + ".default.txt")) {
                if (in == null) {
                    continue;
                }
                File target = new File(categoriesFolder, category + ".txt");
                Files.write(target.toPath(), in.readAllBytes());
            } catch (IOException e) {
                getLogger().warning("Could not save default category file " + category + ": " + e.getMessage());
            }
        }
    }

    private void setDefaultConfigValues() {
        getConfig().addDefault("use-permissions", true);
        getConfig().addDefault("allow-automatic-sorting", true);
        getConfig().addDefault("allow-automatic-inventory-sorting", true);
        getConfig().addDefault("allow-left-click-to-sort", true);
        getConfig().addDefault("left-click-to-sort-enabled-by-default", false);
        getConfig().addDefault("sorting-enabled-by-default", false);
        getConfig().addDefault("inv-sorting-enabled-by-default", false);
        getConfig().addDefault("show-message-when-using-chest", true);
        getConfig().addDefault("show-message-when-using-chest-and-sorting-is-enabled", false);
        getConfig().addDefault("show-message-again-after-logout", true);
        getConfig().addDefault("sorting-method", "{category},{itemsFirst},{name},{color}");
        getConfig().addDefault("auto-generate-category-files", true);
        getConfig().addDefault("sort-time", "close");
        getConfig().addDefault("allow-sorting-hotkeys", true);
        getConfig().addDefault("allow-additional-hotkeys", true);
        getConfig().addDefault("sorting-hotkeys.middle-click", true);
        getConfig().addDefault("sorting-hotkeys.shift-click", true);
        getConfig().addDefault("sorting-hotkeys.double-click", true);
        getConfig().addDefault("sorting-hotkeys.shift-right-click", true);
        getConfig().addDefault("additional-hotkeys.left-click", false);
        getConfig().addDefault("additional-hotkeys.right-click", false);
        getConfig().addDefault("dump", false);
        getConfig().addDefault("log", false);
        getConfig().addDefault("allow-commands", true);
        getConfig().addDefault("prevent-sorting-null-inventories", false);
        getConfig().addDefault("mute-protection-plugins", false);
        getConfig().addDefault("verbose", true);
    }

    void unregisterAllPlayers() {
        if (!getPerPlayerSettings().isEmpty()) {
            for (String uuid : List.copyOf(getPerPlayerSettings().keySet())) {
                Player p = getServer().getPlayer(UUID.fromString(uuid));
                if (p != null) {
                    unregisterPlayer(p);
                }
            }
        }
    }

    public void unregisterPlayer(Player p) {
        UUID uniqueId = p.getUniqueId();
        PlayerSetting setting = getPerPlayerSettings().get(uniqueId.toString());
        if (setting == null) {
            return;
        }

        String fingerprint = getFingerprint();
        PersistentDataContainer pdc = p.getPersistentDataContainer();

        setStoredBoolean(pdc, "sortingEnabled" + fingerprint, setting.sortingEnabled);
        setStoredBoolean(pdc, "invSortingEnabled" + fingerprint, setting.invSortingEnabled);
        setStoredBoolean(pdc, "hasSeenMessage" + fingerprint, setting.hasSeenMessage);
        setStoredBoolean(pdc, "middleClick" + fingerprint, setting.middleClick);
        setStoredBoolean(pdc, "shiftClick" + fingerprint, setting.shiftClick);
        setStoredBoolean(pdc, "doubleClick" + fingerprint, setting.doubleClick);
        setStoredBoolean(pdc, "shiftRightClick" + fingerprint, setting.shiftRightClick);
        setStoredBoolean(pdc, "leftClick" + fingerprint, setting.leftClick);
        setStoredBoolean(pdc, "rightClick" + fingerprint, setting.rightClick);
        setStoredBoolean(pdc, "leftClickOutside" + fingerprint, setting.leftClickOutside);

        getPerPlayerSettings().remove(uniqueId.toString());
    }
}
