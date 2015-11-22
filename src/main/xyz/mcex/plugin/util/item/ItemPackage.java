package xyz.mcex.plugin.util.item;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import xyz.mcex.plugin.equity.database.ItemNotFoundException;
import xyz.mcex.plugin.equity.database.RegisteredItem;

import java.util.UUID;

public class ItemPackage
{
  public final UUID receiver;
  public final int quantity;
  public final RegisteredItem item;
  public final int id;

  public ItemPackage(UUID receiver, RegisteredItem item, int quantity)
  {
    this(receiver, item, quantity, -1);
  }

  public ItemPackage(UUID receiver, RegisteredItem item, int quantity, int id)
  {
    this.receiver = receiver;
    this.item = item;
    this.quantity = quantity;
    this.id = id;
  }

  public ItemStack[] toItemStacks()
  {
    return item.createItemStacks(this.quantity);
  }

  /*public static ItemPackage from(UUID receiver, String itemName, int quantity) throws ItemNotFoundException
  {
    Material material = Material.getMaterial(itemName);
    if (material == null)
      throw new ItemNotFoundException();
    return new ItemPackage(receiver, material, quantity, -1);
  }*/
}
