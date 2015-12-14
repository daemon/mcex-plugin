package xyz.mcex.plugin.equity;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.mcex.plugin.DatabaseManager;
import xyz.mcex.plugin.SubCommandExecutor;
import xyz.mcex.plugin.equity.database.OrderListPages;
import xyz.mcex.plugin.message.MessageAlertColor;
import xyz.mcex.plugin.message.Messages;

public class ListOrdersCommand implements SubCommandExecutor
{
  private final JavaPlugin _plugin;
  private final DatabaseManager _manager;

  public ListOrdersCommand(JavaPlugin plugin, DatabaseManager manager)
  {
    this._plugin = plugin;
    this._manager = manager;
  }

  @Override
  public String getUsage()
  {
    return "/mcex list <buy|sell> <item name> [page number]";
  }

  @Override
  public String getPermissionName()
  {
    return "mcex.cmd.list";
  }

  @Override
  public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args)
  {
    if (args.length < 3)
      return false;

    String type = args[1];
    boolean isBuy = true;
    if (type.equalsIgnoreCase("buy"))
    {
      isBuy = true;
      type = "Buy";
    }
    else if (type.equalsIgnoreCase("sell"))
    {
      isBuy = false;
      type = "Sell";
    }
    else
    {
      commandSender.sendMessage(MessageAlertColor.ERROR + "The second argument must be either \"buy\" or \"sell\"");
      return false;
    }

    String itemName = args[2];
    Integer pageNo = 1;

    if (args.length >= 4)
      try
      {
        pageNo = Integer.parseInt(args[3]);
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
      OrderListPages pages = new OrderListPages(this._manager, itemName, finalIsBuy);
      String pageStr = pages.getPage(finalPageNo - 1);
      String msg = MessageAlertColor.INFO + finalType + " orders for " + itemName.toLowerCase() + "\n" + pageStr;

      if (pageStr == null)
        msg = MessageAlertColor.NOTIFY_AGNOSTIC + "Item not found. Try /mcex search all";
      else if (pageStr.equals(""))
        msg = MessageAlertColor.NOTIFY_AGNOSTIC + "You've reached the end of this database.";
      else
        msg += "\n" + MessageAlertColor.INFO + "/mcex list " + finalType.toLowerCase() + " " + itemName.toLowerCase() + " " + (finalPageNo + 1) + " for the next page.";

      final String finalMsg = msg;
      Bukkit.getScheduler().runTask(this._plugin, () -> {
        commandSender.sendMessage(finalMsg);
      });
    });

    return true;
  }
}
