package xyz.mcex.plugin.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import xyz.mcex.plugin.internals.Nullable;

import java.util.List;

public class Buttons
{
  public static final ItemStack ACTION;
  public static final ItemStack INACTIVE;
  public static final ItemStack DENY;

  static
  {
    ACTION = new ItemStack(Material.STAINED_GLASS_PANE, 1);
    ACTION.setDurability((short) 5); // lime green
    INACTIVE = new ItemStack(Material.STAINED_GLASS_PANE, 1);
    INACTIVE.setDurability((short) 7); // dark gray
    DENY = new ItemStack(Material.STAINED_GLASS_PANE, 1);
    DENY.setDurability((short) 14); // red
  }

  public static ItemStack makeButton(ItemStack template, String title)
  {
    return makeButton(template, title, null);
  }

  public static ItemStack makeButton(ItemStack template, String title, @Nullable List<String> lore)
  {
    ItemMeta itemMeta = Bukkit.getItemFactory().getItemMeta(Material.STAINED_GLASS_PANE);
    itemMeta.setDisplayName(title);
    if (lore != null)
      itemMeta.setLore(lore);

    ItemStack item = template.clone();
    item.setItemMeta(itemMeta);

    return item;
  }
}
