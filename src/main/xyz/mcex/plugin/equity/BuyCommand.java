package xyz.mcex.plugin.equity;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.mcex.plugin.McexPlugin;
import xyz.mcex.plugin.SubCommandExecutor;
import xyz.mcex.plugin.equity.database.EquityDatabase;
import xyz.mcex.plugin.equity.database.PutOrderAsyncTask;
import xyz.mcex.plugin.equity.database.PutOrderResponse;
import xyz.mcex.plugin.equity.event.PlayerEquityTradeEvent;
import xyz.mcex.plugin.message.MessageAlertColor;
import xyz.mcex.plugin.message.Messages;

public class BuyCommand implements SubCommandExecutor
{
  private final Economy _economy;
  private final JavaPlugin _plugin;
  private final EquityDatabase _eqDb;

  public BuyCommand(Economy economy, EquityDatabase db)
  {
    this._plugin = McexPlugin.instance;
    this._economy = economy;
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
    Integer quantity = null;
    Double offerVal = null;

    boolean offerPriceTotalSemantic = false;
    if (strings[3].charAt(0) == '/')
    {
      strings[3] = strings[3].substring(1);
      offerPriceTotalSemantic = true;
    }

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

    if (!offerPriceTotalSemantic)
      offerVal *= quantity;

    double money = this._economy.getBalance(p);
    if (money < offerVal)
    {
      p.sendMessage(MessageAlertColor.ERROR + "You don't have enough money to offer that!");
      return true;
    }

    this._economy.withdrawPlayer(p, offerVal);
    p.sendMessage(MessageAlertColor.NOTIFY_AGNOSTIC + "Processing order...");
    PutOrderAsyncTask.OrderRequest orderRequest = PutOrderAsyncTask.makeOrderRequest(itemName, quantity, offerVal / quantity, true);
    PutOrderAsyncTask task = new PutOrderAsyncTask(this._plugin, this._eqDb, p, orderRequest);
    final Double finalOfferVal = offerVal;

    final Integer finalQuantity = quantity;
    task.addObserver((o, arg) -> {
      PutOrderResponse response = (PutOrderResponse) arg;

      if (response.responseCode != PutOrderResponse.ResponseCode.OK)
        this._economy.depositPlayer(p, finalOfferVal);
      else
      {
        double rate = finalOfferVal / finalQuantity;
        double refund = response.totalQuantity * rate - response.totalMoney;
        if (refund > 0.000001)
          this._economy.depositPlayer(p, refund);

        response.playerUuidToQuantity.forEach((uuid, quant) -> {
          OfflinePlayer seller = Bukkit.getOfflinePlayer(uuid);
          double offerValue = response.playerUuidToMoney.get(uuid) / quant;
          Bukkit.getPluginManager().callEvent(new PlayerEquityTradeEvent(seller, p, response.item, quant, offerValue));
        });

        String announceMessage = this._plugin.getConfig().getString("announce-listing-msg-buy", ChatColor.GREEN + "[%action] " +
            ChatColor.GRAY + "�7%player " + ChatColor.GRAY + "wants " + ChatColor.GOLD + "%quantity %item_name " + ChatColor.GRAY + "at " +
            ChatColor.AQUA + "$%price " + ChatColor.GRAY + "each.");
        boolean shouldAnnounce = this._plugin.getConfig().getBoolean("announce-listing", false);
        if (response.totalQuantity != finalQuantity && shouldAnnounce && finalQuantity - response.totalQuantity > 0)
        {
          announceMessage = announceMessage.replace("%player", p.getDisplayName())
              .replace("%action", "BUY")
              .replace("%quantity", String.valueOf(finalQuantity - response.totalQuantity))
              .replace("%item_name", itemName.toUpperCase())
              .replace("%price", String.valueOf(Math.round(rate * 100) / 100.0));
          Bukkit.getServer().broadcastMessage(announceMessage);
        }
      }
    });

    Bukkit.getScheduler().runTaskAsynchronously(this._plugin, task);
    return true;
  }

  @Override
  public String getUsage()
  {
    return "/mcex buy <item name> <quantity> <offer value>";
  }

  @Override
  public String getPermissionName()
  {
    return "mcex.cmd";
  }
}
