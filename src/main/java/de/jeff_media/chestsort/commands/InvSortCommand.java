package de.jeff_media.chestsort.commands;

import de.jeff_media.chestsort.ChestSortPlugin;
import de.jeff_media.chestsort.config.Messages;
import de.jeff_media.chestsort.data.PlayerSetting;
import de.jeff_media.chestsort.handlers.Logger;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class InvSortCommand implements CommandExecutor {

    private final ChestSortPlugin plugin;

    public InvSortCommand(ChestSortPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!plugin.getConfig().getBoolean("allow-commands") && !sender.isOp()) {
            var message = command.permissionMessage();
            if (message != null) {
                sender.sendMessage(message);
            }
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("help")) {
            return false;
        }

        Player p = null;

        if (!(sender instanceof Player)) {
            if (args.length == 0) {
                sender.sendMessage(Messages.PLAYERS_ONLY);
                return true;
            }
            p = Bukkit.getPlayer(args[0]);
            if (p == null) {
                sender.sendMessage("Could not find player " + args[0]);
                return true;
            }
            args = args.length > 1 ? new String[]{args[1]} : new String[0];
        }

        if (p == null) {
            p = (Player) sender;
        }

        int start = 9;
        int end = 35;

        PlayerSetting setting = plugin.getPerPlayerSettings().get(p.getUniqueId().toString());

        if (!plugin.getConfig().getBoolean("allow-automatic-inventory-sorting")
                && (args.length == 0 || args[0].equalsIgnoreCase("on") || args[0].equalsIgnoreCase("off") || args[0].equalsIgnoreCase("toggle"))) {
            args = new String[]{"inv"};
        }

        if (args.length > 0) {
            switch (args[0].toLowerCase()) {
                case "all" -> {
                    start = 0;
                    end = 35;
                }
                case "hotbar" -> {
                    start = 0;
                    end = 8;
                }
                case "inv" -> {
                    start = 9;
                    end = 35;
                }
                case "on" -> {
                    setting.enableInvSorting();
                    p.sendMessage(Messages.INV_ACTIVATED);
                    return true;
                }
                case "off" -> {
                    setting.disableInvSorting();
                    p.sendMessage(Messages.INV_DEACTIVATED);
                    return true;
                }
                case "toggle" -> {
                    setting.toggleInvSorting();
                    p.sendMessage(setting.invSortingEnabled ? Messages.INV_ACTIVATED : Messages.INV_DEACTIVATED);
                    return true;
                }
                default -> {
                    p.sendMessage(Messages.invalidOptions("\"" + args[0] + "\"", "\"on\", \"off\", \"toggle\", \"inv\", \"hotbar\", \"all\""));
                    return true;
                }
            }
        }

        plugin.getLgr().logSort(p, Logger.SortCause.CMD_ISORT);
        plugin.getOrganizer().sortInventory(p.getInventory(), start, end);
        p.sendMessage(Messages.PLAYER_INVENTORY_SORTED);

        return true;
    }
}
