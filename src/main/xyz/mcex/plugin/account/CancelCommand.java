package xyz.mcex.plugin.account;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.mcex.plugin.DatabaseManager;
import xyz.mcex.plugin.SubCommandExecutor;
import xyz.mcex.plugin.equity.database.EquityDatabase;
import xyz.mcex.plugin.message.MessageAlertColor;
import xyz.mcex.plugin.message.Messages;

import java.sql.SQLException;
import java.util.UUID;

public class CancelCommand implements SubCommandExecutor
{
  private final DatabaseManager _manager;
  private final JavaPlugin _plugin;

  public CancelCommand(JavaPlugin plugin, DatabaseManager manager)
  {
    this._manager = manager;
    this._plugin = plugin;
  }

  @Override
  public String getUsage()
  {
    return "/mcex cancel <buy|sell> <order number>. See /mcex account for your orders.";
  }

  @Override
  public String getPermissionName()
  {
    return "mcex.cmd.cancel";
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String s, String[] args)
  {
    if (!(sender instanceof Player))
    {
      sender.sendMessage(MessageAlertColor.ERROR + Messages.PLAYER_CMD_ERROR);
      return true;
    } else if (args.length < 3)
      return false;

    boolean isBuy = args[1].equalsIgnoreCase("buy");
    if (!isBuy && !args[1].equalsIgnoreCase("sell"))
      return false;

    Integer orderNo;
    try
    {
      orderNo = Integer.parseInt(args[2]);
    } catch (NumberFormatException e) {
      sender.sendMessage(MessageAlertColor.ERROR + "Order number must be a positive integer!");
      return false;
    }

    Player p = (Player) sender;
    UUID pUuid = p.getUniqueId();
    EquityDatabase eqDb = new EquityDatabase(this._manager);
    final Integer finalOrderNo = orderNo;
    Bukkit.getScheduler().runTaskAsynchronously(this._plugin, () -> {
      boolean success;
      try
      {
        success = eqDb.deleteOrder(pUuid, finalOrderNo, isBuy);
      } catch (SQLException e)
      {
        Bukkit.getScheduler().runTask(this._plugin, () -> {
          p.sendMessage(MessageAlertColor.ERROR + Messages.DATABASE_ERROR);
        });
        return;
      }

      Bukkit.getScheduler().runTask(this._plugin, () -> {
        if (success)
          p.sendMessage(MessageAlertColor.NOTIFY_SUCCESS + "Order was cancelled successfully!");
        else
          p.sendMessage(MessageAlertColor.NOTIFY_AGNOSTIC + "That order number doesn't exist. Please see /mcex account");
      });
    });
    return true;
  }
}
