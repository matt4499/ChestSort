package de.jeff_media.chestsort.config;

import de.jeff_media.chestsort.ChestSortPlugin;
import org.bukkit.plugin.Plugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

public final class ConfigUpdater {

    private static final String[] LINES_CONTAINING_STRING_LISTS = {"disabled-worlds:", "blocked-inventory-holders-regex:"};
    private static final String[] LINES_IGNORED = {"config-version:", "plugin-version:"};
    private static final String[] NODES_NEEDING_DOUBLE_QUOTES = {"message-", "sorting-method"};

    private ConfigUpdater() {
    }

    private static void backupCurrentConfig(ChestSortPlugin plugin) {
        File oldFile = new File(getFilePath(plugin, "config.yml"));
        File newFile = new File(getFilePath(plugin, "config-backup-" + plugin.getConfig().getString("plugin-version") + ".yml"));
        if (newFile.exists()) {
            newFile.delete();
        }
        oldFile.getAbsoluteFile().renameTo(newFile.getAbsoluteFile());
    }

    private static String getFilePath(Plugin plugin, String fileName) {
        return plugin.getDataFolder() + File.separator + fileName;
    }

    private static List<String> getNewConfigAsArrayList(Plugin plugin) {
        try {
            return Files.readAllLines(Path.of(getFilePath(plugin, "config.yml")), StandardCharsets.UTF_8);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not read shipped config.yml: " + e.getMessage());
            return List.of();
        }
    }

    private static long getNewConfigVersion() {
        try (InputStream in = ChestSortPlugin.getInstance().getClass().getResourceAsStream("/config-version.txt");
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            return Long.parseLong(reader.readLine());
        } catch (IOException e) {
            return 0;
        }
    }

    private static String getQuotes(String node) {
        for (String prefix : NODES_NEEDING_DOUBLE_QUOTES) {
            if (node.startsWith(prefix)) {
                return "\"";
            }
        }
        return "";
    }

    private static boolean lineContainsIgnoredNode(String line) {
        for (String prefix : LINES_IGNORED) {
            if (line.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static boolean lineIsStringList(String line) {
        for (String prefix : LINES_CONTAINING_STRING_LISTS) {
            if (line.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static void saveArrayListToConfig(Plugin plugin, List<String> lines) {
        try (var writer = Files.newBufferedWriter(new File(getFilePath(plugin, "config.yml")).toPath(), StandardCharsets.UTF_8)) {
            for (String line : lines) {
                writer.write(line + System.lineSeparator());
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save updated config.yml: " + e.getMessage());
        }
    }

    public static void updateConfig() {
        ChestSortPlugin plugin = ChestSortPlugin.getInstance();
        Logger logger = plugin.getLogger();

        if (plugin.getConfig().getLong("config-version") >= getNewConfigVersion()) {
            return;
        }

        logger.info("===========================================");
        logger.info("You are using an outdated config file.");
        logger.info("Your config file will now be updated to the");
        logger.info("newest version. Your changes will be kept.");
        logger.info("===========================================");

        backupCurrentConfig(plugin);
        plugin.saveDefaultConfig();

        Set<String> oldConfigNodes = plugin.getConfig().getKeys(false);
        List<String> newConfig = new ArrayList<>();

        for (String defaultLine : getNewConfigAsArrayList(plugin)) {
            String updatedLine = defaultLine;

            if (defaultLine.startsWith("sorting-hotkeys:") || defaultLine.startsWith("additional-hotkeys:")) {
                // keep default section headers as-is
            } else if (defaultLine.startsWith("  middle-click:")) {
                updatedLine = "  middle-click: " + plugin.getConfig().getBoolean("sorting-hotkeys.middle-click");
            } else if (defaultLine.startsWith("  shift-click:")) {
                updatedLine = "  shift-click: " + plugin.getConfig().getBoolean("sorting-hotkeys.shift-click");
            } else if (defaultLine.startsWith("  double-click:")) {
                updatedLine = "  double-click: " + plugin.getConfig().getBoolean("sorting-hotkeys.double-click");
            } else if (defaultLine.startsWith("  shift-right-click:")) {
                updatedLine = "  shift-right-click: " + plugin.getConfig().getBoolean("sorting-hotkeys.shift-right-click");
            } else if (defaultLine.startsWith("  left-click:")) {
                updatedLine = "  left-click: " + plugin.getConfig().getBoolean("additional-hotkeys.left-click");
            } else if (defaultLine.startsWith("  right-click:")) {
                updatedLine = "  right-click: " + plugin.getConfig().getBoolean("additional-hotkeys.right-click");
            } else if (defaultLine.startsWith("-") || defaultLine.startsWith(" -") || defaultLine.startsWith("  -")) {
                updatedLine = null;
            } else if (lineContainsIgnoredNode(defaultLine)) {
                // keep shipped default, never apply the old value
            } else if (lineIsStringList(defaultLine)) {
                updatedLine = null;
                newConfig.add(defaultLine);
                String node = defaultLine.split(":")[0];
                for (String entry : plugin.getConfig().getStringList(node)) {
                    newConfig.add("- " + entry);
                }
            } else {
                for (String node : oldConfigNodes) {
                    if (defaultLine.startsWith(node + ":")) {
                        String quotes = getQuotes(node);
                        String value = plugin.getConfig().get(node).toString().replace("\n", "\\n");
                        updatedLine = node + ": " + quotes + value + quotes;
                    }
                }
            }

            if (updatedLine != null) {
                newConfig.add(updatedLine);
            }
        }

        saveArrayListToConfig(plugin, newConfig);
    }
}
