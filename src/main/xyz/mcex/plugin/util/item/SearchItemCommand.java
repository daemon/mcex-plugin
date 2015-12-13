package xyz.mcex.plugin.util.item;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.mcex.plugin.DatabaseManager;
import xyz.mcex.plugin.SubCommandExecutor;
import xyz.mcex.plugin.equity.database.ItemDatabase;
import xyz.mcex.plugin.gui.MenuFlow;
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
    return "/mcex search <item name|all>";
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
    else if (!(sender instanceof Player))
    {
      sender.sendMessage(MessageAlertColor.ERROR + Messages.PLAYER_CMD_ERROR);
      return true;
    }

    if (args[1].equalsIgnoreCase("all"))
      args[1] = "%";

    SearchResultGui gui = new SearchResultGui(this._manager, args[1], (Player) sender, 0);
    MenuFlow flow = new MenuFlow(gui);
    gui.setNextClickListener(gui.createListener(flow, 0));
    gui.show();

    return true;
  }
}
