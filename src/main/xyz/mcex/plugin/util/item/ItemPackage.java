package xyz.mcex.plugin.util.item;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import xyz.mcex.plugin.equity.database.ItemNotFoundException;

import java.util.UUID;

public class ItemPackage
{
  public final UUID receiver;
  public final int quantity;
  public final Material material;
  public final int id;

  public ItemPackage(UUID receiver, Material material, int quantity)
  {
    this(receiver, material, quantity, -1);
  }

  public ItemPackage(UUID receiver, Material material, int quantity, int id)
  {
    this.receiver = receiver;
    this.material = material;
    this.quantity = quantity;
    this.id = id;
  }

  public ItemStack[] toItemStacks()
  {
    int mss = this.material.getMaxStackSize();
    int quantity = this.quantity;
    ItemStack[] stacks = new ItemStack[(int) Math.ceil(quantity / (double) mss)];

    for (int i = 0; i < stacks.length; ++i)
    {
      stacks[i] = new ItemStack(this.material, Math.min(quantity, mss));
      quantity -= Math.min(quantity, mss);
    }

    return stacks;
  }

  public static ItemPackage from(UUID receiver, String itemName, int quantity) throws ItemNotFoundException
  {
    Material material = Material.getMaterial(itemName);
    if (material == null)
      throw new ItemNotFoundException();
    return new ItemPackage(receiver, material, quantity, -1);
  }
}
