package xyz.mcex.plugin.equity;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.mcex.plugin.DatabaseManager;
import xyz.mcex.plugin.McexPlugin;
import xyz.mcex.plugin.SubCommandExecutor;
import xyz.mcex.plugin.equity.database.OrderListPages;
import xyz.mcex.plugin.equity.database.RecentOrderListPages;
import xyz.mcex.plugin.message.MessageAlertColor;
import xyz.mcex.plugin.message.Messages;

public class ListRecentCommand implements SubCommandExecutor
{
  private final JavaPlugin _plugin;
  private final DatabaseManager _manager;

  public ListRecentCommand(DatabaseManager manager)
  {
    this._plugin = McexPlugin.instance;
    this._manager = manager;
  }

  @Override
  public String getUsage()
  {
    return "/mcex recent <buy|sell> [page number]";
  }

  @Override
  public String getPermissionName()
  {
    return "mcex.cmd";
  }

  @Override
  public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args)
  {
    if (args.length < 2)
      return false;

    String type = args[1];
    boolean isBuy = true;
    if (type.equalsIgnoreCase("buy"))
    {
      isBuy = true;
      type = "buy";
    }
    else if (type.equalsIgnoreCase("sell"))
    {
      isBuy = false;
      type = "sell";
    }
    else
    {
      commandSender.sendMessage(MessageAlertColor.ERROR + "The second argument must be either \"buy\" or \"sell\"");
      return false;
    }

    Integer pageNo = 1;

    if (args.length >= 3)
      try
      {
        pageNo = Integer.parseInt(args[2]);
      } catch (NumberFormatException e) {
        commandSender.sendMessage(MessageAlertColor.ERROR + "Page number must be a positive integer.");
        return false;
      }

    if (pageNo <= 0)
    {
      commandSender.sendMessage(MessageAlertColor.ERROR + "Page number must be a positive integer.");
      return false;
    }

    commandSender.sendMessage(MessageAlertColor.NOTIFY_AGNOSTIC + "Processing...");
    final Integer finalPageNo = pageNo;
    final boolean finalIsBuy = isBuy;
    final String finalType = type;
    Bukkit.getScheduler().runTaskAsynchronously(this._plugin, () -> {
      RecentOrderListPages pages = new RecentOrderListPages(this._manager, finalIsBuy);
      String pageStr = pages.getPage(finalPageNo - 1);
      String msg = MessageAlertColor.INFO + "Recent open " + finalType + " orders:\n" + pageStr;

      if (pageStr == null)
        msg = MessageAlertColor.NOTIFY_AGNOSTIC + "Item not found. Try /mcex search all";
      else if (pageStr.equals(""))
        msg = MessageAlertColor.NOTIFY_AGNOSTIC + "You've reached the end of this database.";
      else
        msg += "\n" + MessageAlertColor.INFO + "/mcex recent " + finalType + " " + (finalPageNo + 1) + " for the next page.";

      final String finalMsg = msg;
      Bukkit.getScheduler().runTask(this._plugin, () -> {
        commandSender.sendMessage(finalMsg);
      });
    });

    return true;
  }
}
