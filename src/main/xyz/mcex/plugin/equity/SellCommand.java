package xyz.mcex.plugin.equity;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.mcex.plugin.SubCommandExecutor;
import xyz.mcex.plugin.equity.database.*;
import xyz.mcex.plugin.equity.event.PlayerEquityTradeEvent;
import xyz.mcex.plugin.message.MessageAlertColor;
import xyz.mcex.plugin.message.Messages;
import xyz.mcex.plugin.util.item.ItemNbtHash;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

public class SellCommand implements SubCommandExecutor
{
  private final JavaPlugin _plugin;
  private final EquityDatabase _eqDb;

  public SellCommand(JavaPlugin plugin, EquityDatabase db)
  {
    this._plugin = plugin;
    this._eqDb = db;
  }

  private boolean properPruneInv(PlayerInventory inv, RegisteredItem item, int quantity)
  {
    ListIterator<ItemStack> li = inv.iterator();
    List<Integer> removeIndexes = new LinkedList<Integer>();

    int index = -1;
    while (li.hasNext())
    {
      ++index;
      ItemStack is = li.next();
      if (is == null)
        continue;

      ItemNbtHash hash = ItemNbtHash.from(is);
      if (hash == null)
        continue;

      if (!hash.equals(item.hash))
        continue;

      if (is.getAmount() <= quantity)
      {
        removeIndexes.add(index);
        quantity -= is.getAmount();
      } else {
        is.setAmount(is.getAmount() - quantity);
        quantity = 0;
      }
    }

    if (quantity != 0)
      return false;

    for (Integer removeIndex : removeIndexes)
      inv.clear(removeIndex);

    return true;
  }

  @Override
  public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings)
  {
    if (!(commandSender instanceof Player))
    {
      commandSender.sendMessage(MessageAlertColor.ERROR + Messages.PLAYER_CMD_ERROR);
      return true;
    }

    if (strings.length < 4)
      return false;

    Player p = (Player) commandSender;

    String itemName = strings[1];
    Integer quantity;
    Double offerVal;
    try
    {
      quantity = Integer.parseInt(strings[2]);
      offerVal = Double.parseDouble(strings[3]);
    } catch (NumberFormatException e) {
      p.sendMessage(MessageAlertColor.ERROR + "Quantity and offer price must be an integer and decimal, respectively");
      return false;
    }

    if (quantity <= 0 || offerVal <= 0.00000001)
    {
      p.sendMessage(MessageAlertColor.ERROR + "Quantity and price must be positive!");
      return false;
    }

    ItemDatabase db = new ItemDatabase(this._eqDb.manager());
    RegisteredItem item;
    try
    {
      item = db.getItem(itemName, null);
    } catch (SQLException e)
    {
      p.sendMessage(MessageAlertColor.ERROR + Messages.DATABASE_ERROR);
      return true;
    } catch (ItemNotFoundException e)
    {
      p.sendMessage(MessageAlertColor.ERROR + "Item doesn't exist!");
      return true;
    }

    PlayerInventory pInv = p.getInventory();
    // System.out.println(item.createItemStacks(1)[0].getItemMeta().getDisplayName() + " " + item.createItemStacks(1)[0].getItemMeta().getLore());
    if (!this.properPruneInv(pInv, item, quantity))
    {
      p.sendMessage(MessageAlertColor.ERROR + "You don't have enough of that item to sell!");
      return true;
    }

    p.sendMessage(MessageAlertColor.NOTIFY_AGNOSTIC + "Processing order...");
    PutOrderAsyncTask.OrderRequest orderRequest = PutOrderAsyncTask.makeOrderRequest(itemName, quantity, offerVal / quantity, false);
    PutOrderAsyncTask task = new PutOrderAsyncTask(this._plugin, this._eqDb, p, orderRequest);

    final Integer finalQuantity = quantity;
    task.addObserver((o, arg) -> {
      PutOrderResponse response = (PutOrderResponse) arg;
      if (response.responseCode != PutOrderResponse.ResponseCode.OK)
      {
        ItemStack[] refund = item.createItemStacks(finalQuantity);
        pInv.addItem(refund);
      } else {
        response.playerUuidToQuantity.forEach((uuid, quant) -> {
          OfflinePlayer buyer = Bukkit.getOfflinePlayer(uuid);
          double offerValue = response.playerUuidToMoney.get(uuid) / quant;
          Bukkit.getPluginManager().callEvent(new PlayerEquityTradeEvent(p, buyer, response.item, quant, offerValue));
        });
      }
    });

    Bukkit.getScheduler().runTaskAsynchronously(this._plugin, task);
    return true;
  }

  @Override
  public String getUsage()
  {
    return "/mcex sell <item name> <quantity> <offer value>";
  }

  @Override
  public String getPermissionName()
  {
    return "mcex.cmd.sell";
  }
}
