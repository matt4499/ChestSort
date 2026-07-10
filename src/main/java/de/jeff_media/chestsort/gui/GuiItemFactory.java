package de.jeff_media.chestsort.gui;

import com.destroystokyo.paper.profile.ProfileProperty;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import com.destroystokyo.paper.profile.PlayerProfile;

import java.util.UUID;

public final class GuiItemFactory {

    private GuiItemFactory() {
    }

    public static ItemStack fromConfigurationSection(ConfigurationSection section) {
        if (section == null) {
            return null;
        }

        Material material = Material.matchMaterial(section.getString("material", "STONE"));
        if (material == null) {
            material = Material.STONE;
        }

        ItemStack item = new ItemStack(material, section.getInt("amount", 1));
        ItemMeta meta = item.getItemMeta();

        if (section.isString("display-name")) {
            meta.displayName(parse(section.getString("display-name")));
        }

        if (section.isList("lore")) {
            meta.lore(section.getStringList("lore").stream().map(GuiItemFactory::parse).toList());
        }

        if (section.isInt("custom-model-data")) {
            meta.setCustomModelData(section.getInt("custom-model-data"));
        }

        if (meta instanceof SkullMeta skullMeta && section.isString("base64")) {
            PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID());
            profile.setProperty(new ProfileProperty("textures", section.getString("base64")));
            skullMeta.setPlayerProfile(profile);
        }

        if (section.isConfigurationSection("enchantments")) {
            ConfigurationSection enchantSection = section.getConfigurationSection("enchantments");
            for (String key : enchantSection.getKeys(false)) {
                Enchantment enchantment = Registry.ENCHANTMENT.get(NamespacedKey.minecraft(key.toLowerCase()));
                if (enchantment != null) {
                    meta.addEnchant(enchantment, enchantSection.getInt(key), true);
                }
            }
        }

        item.setItemMeta(meta);
        return item;
    }

    private static Component parse(String text) {
        return MiniMessage.miniMessage().deserialize(text == null ? "" : text);
    }
}
