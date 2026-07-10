package de.jeff_media.chestsort.utils;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public final class EnchantmentUtils {

    private EnchantmentUtils() {
    }

    public static String getEnchantmentString(ItemStack item) {
        StringBuilder builder = new StringBuilder(",").append(getInversedEnchantmentAmount(item));
        if (!item.hasItemMeta()) {
            return builder.toString();
        }
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasEnchants() && !(meta instanceof EnchantmentStorageMeta)) {
            return builder.toString();
        }
        appendSorted(builder, meta, meta.getEnchants().keySet());
        if (meta instanceof EnchantmentStorageMeta storageMeta) {
            appendSorted(builder, meta, storageMeta.getStoredEnchants().keySet());
        }
        return builder.toString();
    }

    private static void appendSorted(StringBuilder builder, ItemMeta meta, Set<Enchantment> enchantments) {
        List<Enchantment> sorted = new ArrayList<>(enchantments);
        sorted.sort(Comparator.comparing(enchantment -> enchantment.getKey().getKey()));
        for (Enchantment enchantment : sorted) {
            builder.append(',').append(enchantment.getKey().getKey())
                    .append(',').append(Integer.MAX_VALUE - meta.getEnchantLevel(enchantment));
        }
    }

    public static int getInversedEnchantmentAmount(ItemStack item) {
        int amount = Integer.MAX_VALUE;
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasEnchants() && !(meta instanceof EnchantmentStorageMeta)) {
            return amount;
        }
        for (int level : meta.getEnchants().values()) {
            amount -= level;
        }
        if (meta instanceof EnchantmentStorageMeta storageMeta) {
            for (int level : storageMeta.getStoredEnchants().values()) {
                amount -= level;
            }
        }
        return amount;
    }
}
