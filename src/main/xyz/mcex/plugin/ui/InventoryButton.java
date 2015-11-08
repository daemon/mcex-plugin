package xyz.mcex.plugin.ui;

import org.bukkit.inventory.ItemStack;

public class InventoryButton
{
  public final int x;
  public final int y;
  public final ItemStack item;
  public final String text;

  public InventoryButton(int x, int y, ItemStack item, String text)
  {
    this.x = x;
    this.y = y;
    this.item = item;
    this.text = text;
  }
}
