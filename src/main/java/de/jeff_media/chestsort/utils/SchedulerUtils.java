package de.jeff_media.chestsort.utils;

import de.jeff_media.chestsort.ChestSortPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class SchedulerUtils {

    public static void runTaskLater(@Nullable Location loc, @NotNull Runnable task, long delay) {
        ChestSortPlugin plugin = ChestSortPlugin.getInstance();
        long ticks = Math.max(1, delay);
        if (loc != null) {
            Bukkit.getRegionScheduler().runDelayed(plugin, loc, scheduledTask -> task.run(), ticks);
        } else {
            Bukkit.getGlobalRegionScheduler().runDelayed(plugin, scheduledTask -> task.run(), ticks);
        }
    }
}
