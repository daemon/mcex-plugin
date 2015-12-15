package xyz.mcex.plugin.equity;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.mcex.plugin.DatabaseManager;
import xyz.mcex.plugin.SubCommandExecutor;
import xyz.mcex.plugin.equity.database.ItemDatabase;
import xyz.mcex.plugin.equity.database.RegisteredItem;
import xyz.mcex.plugin.message.MessageAlertColor;
import xyz.mcex.plugin.message.Messages;

import java.sql.SQLException;
import java.util.List;

public class ChartCommand implements SubCommandExecutor
{
  private final JavaPlugin _plugin;
  private final ItemDatabase _itemDb;

  public ChartCommand(JavaPlugin plugin, DatabaseManager manager)
  {
    this._itemDb = new ItemDatabase(manager);
    this._plugin = plugin;
  }

  @Override
  public String getUsage()
  {
    return "/mcex chart <item name>";
  }

  @Override
  public String getPermissionName()
  {
    return "mcex.cmd";
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String s, String[] args)
  {
    if (args.length < 2)
      return false;

    sender.sendMessage(MessageAlertColor.NOTIFY_AGNOSTIC + "Processing...");
    Bukkit.getScheduler().runTaskAsynchronously(this._plugin, () -> {
      String message;
      FileConfiguration cfg = this._plugin.getConfig();

      try
      {
        List<RegisteredItem> matches = this._itemDb.findByName(args[1], 0, 1);
        if (matches.size() == 0)
          message = MessageAlertColor.NOTIFY_AGNOSTIC + "No results found for '" + args[1] + "'";
        else
          message = MessageAlertColor.NOTIFY_SUCCESS + "http://" + cfg.get("chart-url") + ":" + cfg.get("chart-port") + "/charts/" + matches.get(0).alias.toLowerCase();
      } catch (SQLException e)
      {
        message = MessageAlertColor.ERROR + Messages.DATABASE_ERROR;
      }

      final String finalMessage = message;
      Bukkit.getScheduler().runTask(this._plugin, () -> {
        sender.sendMessage(finalMessage);
      });
    });

    return true;
  }
}
