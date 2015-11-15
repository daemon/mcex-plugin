package xyz.mcex.plugin.equity;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.mcex.plugin.equity.database.EquityDatabase;
import xyz.mcex.plugin.equity.database.PutOrderAsyncTask;
import xyz.mcex.plugin.message.MessageAlertColor;

public class BuyCommand implements CommandExecutor
{
  private final Economy _economy;
  private final JavaPlugin _plugin;
  private final EquityDatabase _eqDb;

  public BuyCommand(Economy economy, JavaPlugin plugin, EquityDatabase db)
  {
    this._plugin = plugin;
    this._economy = economy;
    this._eqDb = db;
  }

  @Override
  public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings)
  {
    if (!(commandSender instanceof Player))
    {
      commandSender.sendMessage(MessageAlertColor.ERROR + "You need to be a player to run this command.");
      return true;
    }

    if (strings.length < 3)
      return false;

    Player p = (Player) commandSender;

    String itemName = strings[0];
    Integer quantity = null;
    Double offerVal = null;
    try
    {
      quantity = Integer.parseInt(strings[1]);
      offerVal = Double.parseDouble(strings[2]);
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

    double money = this._economy.getBalance(p);
    if (money < offerVal)
    {
      p.sendMessage(MessageAlertColor.ERROR + "You don't have enough money to offer that!");
      return true;
    }

    this._economy.withdrawPlayer(p, offerVal);
    p.sendMessage(MessageAlertColor.NOTIFY_AGNOSTIC + "Processing order...");
    PutOrderAsyncTask task = new PutOrderAsyncTask(this._plugin, this._eqDb, p, mat, quantity, offerVal / quantity, true);
    final Double finalOfferVal = offerVal;

    task.addObserver((o, arg) -> {
      // TODO: move code from PutOrderAsyncTask.ResponseTask to here
      // TODO: notify online sellers that buyer purchased their stuff
      PutOrderAsyncTask.Response response = (PutOrderAsyncTask.Response) arg;
      if (response != PutOrderAsyncTask.Response.OK)
        this._economy.depositPlayer(p, finalOfferVal);
    });

    Bukkit.getScheduler().runTaskAsynchronously(this._plugin, task);
    return true;
  }
}
