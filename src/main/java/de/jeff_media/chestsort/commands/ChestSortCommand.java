package de.jeff_media.chestsort.commands;

import de.jeff_media.chestsort.ChestSortPlugin;
import de.jeff_media.chestsort.config.Messages;
import de.jeff_media.chestsort.data.PlayerSetting;
import de.jeff_media.chestsort.gui.NewUI;
import de.jeff_media.chestsort.handlers.Debugger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ChestSortCommand implements CommandExecutor {

    private final ChestSortPlugin plugin;

    public ChestSortCommand(ChestSortPlugin plugin) {
        this.plugin = plugin;
    }

    private void sendNoPermissionMessage(CommandSender sender, Command command) {
        Component message = command.permissionMessage();
        if (message != null) {
            sender.sendMessage(message);
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("sort")) {
            return false;
        }

        if (!plugin.getConfig().getBoolean("allow-commands") && !sender.isOp()) {
            sendNoPermissionMessage(sender, command);
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("resetplayersettings")) {
            if (!sender.hasPermission("chestsort.resetplayersettings")) {
                sendNoPermissionMessage(sender, command);
                return true;
            }
            for (Player online : Bukkit.getOnlinePlayers()) {
                plugin.unregisterPlayer(online);
            }
            plugin.incrementFingerprint();
            sender.sendMessage(Component.text("All player settings have been reset!", NamedTextColor.RED));
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("chestsort.reload")) {
                sendNoPermissionMessage(sender, command);
                return true;
            }
            sender.sendMessage(Component.text("Reloading ChestSort...", NamedTextColor.GRAY));
            plugin.load(true);
            sender.sendMessage(Component.text("ChestSort has been reloaded.", NamedTextColor.GREEN));
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("debug")) {
            if (!sender.hasPermission("chestsort.debug")) {
                sendNoPermissionMessage(sender, command);
                return true;
            }
            sender.sendMessage(Component.text("ChestSort Debug mode enabled - I hope you know what you are doing!", NamedTextColor.RED));
            plugin.setDebug(true);
            plugin.getServer().getPluginManager().registerEvents(new Debugger(plugin), plugin);
            plugin.debug("Debug mode activated through command by " + sender.getName());
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("help")) {
            return false;
        }

        if (!(sender instanceof Player p)) {
            if (args.length != 0 && args[0].equalsIgnoreCase("debug")) {
                plugin.setDebug(true);
                plugin.getLogger().info("ChestSort debug mode enabled.");
                return true;
            }
            sender.sendMessage(Messages.PLAYERS_ONLY);
            return true;
        }

        plugin.registerPlayerIfNeeded(p);

        if (!plugin.getConfig().getBoolean("allow-automatic-sorting") || !p.hasPermission("chestsort.automatic")) {
            args = new String[]{"hotkeys"};
        }

        if (args.length == 0 && !plugin.getConfig().getBoolean("allow-gui", true)) {
            args = new String[]{"toggle"};
        }

        if (args.length > 0 && (args[0].equalsIgnoreCase("hotkeys") || args[0].equalsIgnoreCase("hotkey"))) {
            new NewUI(p).showGUI();
            return true;
        }

        PlayerSetting setting = plugin.getPerPlayerSettings().get(p.getUniqueId().toString());

        if (args.length > 0 && !args[0].equalsIgnoreCase("toggle") && !args[0].equalsIgnoreCase("on") && !args[0].equalsIgnoreCase("off")) {
            p.sendMessage(Messages.invalidOptions("\"" + args[0] + "\"", "\"toggle\", \"on\", \"off\", \"hotkeys\""));
            return true;
        }

        if (args.length > 0) {
            switch (args[0].toLowerCase()) {
                case "toggle" -> setting.toggleChestSorting();
                case "on" -> setting.enableChestSorting();
                case "off" -> setting.disableChestSorting();
                default -> {
                }
            }
            setting.hasSeenMessage = true;
            p.sendMessage(setting.sortingEnabled ? Messages.ACTIVATED : Messages.DEACTIVATED);
            return true;
        }

        setting.hasSeenMessage = true;
        new NewUI(p).showGUI();
        return true;
    }
}
