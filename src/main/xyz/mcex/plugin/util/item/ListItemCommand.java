package xyz.mcex.plugin.util.item;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import xyz.mcex.plugin.DatabaseManager;
import xyz.mcex.plugin.McexPlugin;
import xyz.mcex.plugin.SubCommandExecutor;
import xyz.mcex.plugin.equity.database.EquityDatabase;
import xyz.mcex.plugin.gui.MenuFlow;
import xyz.mcex.plugin.message.MessageAlertColor;
import xyz.mcex.plugin.message.Messages;
import xyz.mcex.plugin.message.Pages;
import xyz.mcex.plugin.message.StringPages;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class ListItemCommand implements SubCommandExecutor
{
  private final ItemPackageDatabase _ipDb;

  public ListItemCommand(DatabaseManager manager)
  {
    EquityDatabase eqDb = new EquityDatabase(manager);
    this._ipDb = new ItemPackageDatabase(manager, eqDb);
  }

  @Override
  public String getUsage()
  {
    return "/mcex mailbox";
  }

  @Override
  public String getPermissionName()
  {
    return "mcex.cmd.itemmail.mailbox";
  }

  @Override
  public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings)
  {
    if (!(commandSender instanceof Player))
    {
      commandSender.sendMessage(MessageAlertColor.ERROR + Messages.PLAYER_CMD_ERROR);
      return true;
    }

    Player p = (Player) commandSender;
    MailboxGui gui = new MailboxGui(p, this._ipDb.manager(), 0);
    Bukkit.getPluginManager().registerEvents(gui, McexPlugin.instance);
    gui.show();
    return true;
  }
}
