package xyz.mcex.plugin.equity;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.mcex.plugin.SubCommandExecutor;
import xyz.mcex.plugin.equity.database.EquityDatabase;
import xyz.mcex.plugin.equity.database.PutOrderAsyncTask;
import xyz.mcex.plugin.equity.database.PutOrderResponse;
import xyz.mcex.plugin.message.MessageAlertColor;
import xyz.mcex.plugin.message.Messages;

public class SellCommand implements SubCommandExecutor
{
  private final JavaPlugin _plugin;
  private final EquityDatabase _eqDb;

  public SellCommand(JavaPlugin plugin, EquityDatabase db)
  {
    this._plugin = plugin;
    this._eqDb = db;
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

    Material mat = Material.getMaterial(itemName.toUpperCase());
    if (mat == null)
    {
      p.sendMessage(MessageAlertColor.ERROR + "Item doesn't exist!");
      return false;
    }

    Inventory pInv = p.getInventory();
    if (!pInv.contains(mat, quantity))
    {
      p.sendMessage(MessageAlertColor.ERROR + "You do not have enough items in your inventory to sell!");
      return true;
    }

    pInv.removeItem(new ItemStack(mat, quantity));

    p.sendMessage(MessageAlertColor.NOTIFY_AGNOSTIC + "Processing order...");
    PutOrderAsyncTask task = new PutOrderAsyncTask(this._plugin, this._eqDb, p, mat, quantity, offerVal / quantity, false);

    final Integer finalQuantity = quantity;
    task.addObserver((o, arg) -> {
      PutOrderResponse response = (PutOrderResponse) arg;
      if (response.responseCode != PutOrderResponse.ResponseCode.OK)
        pInv.addItem(new ItemStack(mat, finalQuantity));
    });

    Bukkit.getScheduler().runTaskAsynchronously(this._plugin, task);
    return true;
  }

  @Override
  public String getUsage()
  {
    return "/mcex sell <item name> <quantity> [offer value]";
  }

  @Override
  public String getPermissionName()
  {
    return "mcex.cmd.sell";
  }
}
