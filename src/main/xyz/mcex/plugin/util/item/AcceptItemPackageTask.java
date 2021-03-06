package xyz.mcex.plugin.util.item;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import xyz.mcex.plugin.message.MessageAlertColor;

import java.util.HashMap;

/* This could be improved on (use async-then-sync model), but I think the typical use case will not be costly.
  TODO benchmark
 */
public class AcceptItemPackageTask implements Runnable
{
  private final ItemPackage _pkg;
  private final ItemPackageDatabase _db;

  public AcceptItemPackageTask(ItemPackageDatabase db, ItemPackage itemPackage)
  {
    this._pkg = itemPackage;
    this._db = db;
  }

  @Override
  public void run()
  {
    Player p = Bukkit.getPlayer(this._pkg.receiver);
    if (p == null)
      return;

    Inventory inv = p.getInventory();
    HashMap<Integer, ItemStack> remainder = inv.addItem(this._pkg.toItemStacks());
    remainder.forEach((index, itemStack) -> {
      p.getWorld().dropItem(p.getLocation(), itemStack);
    });

    p.sendMessage(MessageAlertColor.NOTIFY_SUCCESS + "Items accepted.");
  }
}
