package xyz.mcex.plugin.equity.database;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import xyz.mcex.plugin.internals.Nullable;
import xyz.mcex.plugin.util.item.ItemNbtHash;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class RegisteredItem
{
  public final ItemNbtHash hash;
  public final int id;
  public final int durability;
  public final List<String> lore;
  public final String displayName;
  public final Material material;
  public final String alias;
  private final Map<String, Integer> enchantNameToLevel;

  RegisteredItem(int id, ItemNbtHash hash, int durability, List<String> lore, @Nullable String displayName, Material mat, String alias,
                 Map<String, Integer> enchantNameToLvl)
  {
    this.hash = hash;
    this.id = id;
    this.durability = durability;
    this.lore = lore;
    this.alias = alias;
    this.displayName = displayName;
    this.material = mat;
    this.enchantNameToLevel = enchantNameToLvl;
  }

  public ItemStack[] createItemStacks(int quantity)
  {
    int mss = this.material.getMaxStackSize();
    ItemStack[] stacks = new ItemStack[(int) Math.ceil(quantity / (double) mss)];

    for (int i = 0; i < stacks.length; ++i)
    {
      stacks[i] = new ItemStack(this.material, Math.min(quantity, mss));
      stacks[i].setDurability((short) this.durability);

      ItemMeta meta = Bukkit.getItemFactory().getItemMeta(stacks[i].getType());
      if (this.displayName != null && this.displayName.length() != 0)
        meta.setDisplayName(this.displayName);
      if (!this.lore.isEmpty())
        meta.setLore(this.lore);

      this.enchantNameToLevel.forEach((enchantName, lvl) -> meta.addEnchant(Enchantment.getByName(enchantName), lvl, true));

      stacks[i].setItemMeta(meta);
      quantity -= Math.min(quantity, mss);
    }

    return stacks;
  }

  public ItemStack createInfoItem(int quantity, double price)
  {
    ItemStack[] itemStack = this.createItemStacks(1);
    ItemMeta meta = itemStack[0].getItemMeta();
    if (meta.getLore() == null)
      meta.setLore(new LinkedList<>());

    List<String> lores = meta.getLore();
    lores.add(0, ChatColor.RESET + "" + ChatColor.GOLD + "$" + (Math.round(price * 100) / 100.0) + ChatColor.GRAY + " each");
    lores.add(0, ChatColor.RESET + "" + ChatColor.GRAY + "Quantity: " + ChatColor.GOLD + quantity);
    lores.add(0, ChatColor.RESET + "" + ChatColor.GRAY + "MCEX name: " + ChatColor.AQUA + this.alias);
    meta.setLore(lores);
    itemStack[0].setItemMeta(meta);
    return itemStack[0];
  }
}
