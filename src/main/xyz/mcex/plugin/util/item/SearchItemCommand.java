package xyz.mcex.plugin.util.item;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.mcex.plugin.DatabaseManager;
import xyz.mcex.plugin.SubCommandExecutor;
import xyz.mcex.plugin.equity.database.ItemDatabase;
import xyz.mcex.plugin.message.MessageAlertColor;
import xyz.mcex.plugin.message.Messages;

public class SearchItemCommand implements SubCommandExecutor
{
  private final DatabaseManager _manager;
  private final JavaPlugin _plugin;
  private final ItemDatabase _itemDb;

  public SearchItemCommand(JavaPlugin plugin, DatabaseManager manager)
  {
    this._manager = manager;
    this._plugin = plugin;
    this._itemDb = new ItemDatabase(this._manager);
  }

  @Override
  public String getUsage()
  {
    return "/mcex search <item name|all> [page #]";
  }

  @Override
  public String getPermissionName()
  {
    return "mcex.cmd.search";
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String s, String[] args)
  {
    if (args.length < 2)
      return false;

    int pageNo = 0;
    if (args.length >= 3)
      try
      {
        pageNo = Integer.parseInt(args[2]) - 1;
      } catch (NumberFormatException e) {
        sender.sendMessage(MessageAlertColor.ERROR + "The page number parameter must be an integer.");
        return false;
      }

    if (args[1].equalsIgnoreCase("all"))
      args[1] = "%";
    String query = args[1];
    sender.sendMessage(MessageAlertColor.NOTIFY_AGNOSTIC + "Processing...");
    final int finalPageNo = pageNo;
    Bukkit.getScheduler().runTaskAsynchronously(this._plugin, () -> {
      SearchItemPages pages = new SearchItemPages(this._manager, query);
      String message = pages.getPage(finalPageNo);
      if (message == null)
        message = MessageAlertColor.ERROR + Messages.DATABASE_ERROR;
      else if (message.length() == 0)
        message = MessageAlertColor.NOTIFY_AGNOSTIC + "You've reached the end of this database.";
      else
        message += MessageAlertColor.INFO + "/mcex search " + query + " " + (finalPageNo + 2) + " for next page";

      final String finalMessage = message;
      Bukkit.getScheduler().runTask(this._plugin, () -> sender.sendMessage(finalMessage));
    });

    return true;
  }
}
