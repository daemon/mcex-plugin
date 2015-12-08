package xyz.mcex.plugin.util.item;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import xyz.mcex.plugin.DatabaseManager;
import xyz.mcex.plugin.SubCommandExecutor;
import xyz.mcex.plugin.equity.database.EquityDatabase;
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
    return "/mcex mailbox <page #>";
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
    List<ItemPackage> packages;
    try
    {
      packages = this._ipDb.getPackages(p.getUniqueId());
    } catch (Exception e)
    {
      e.printStackTrace();
      p.sendMessage(MessageAlertColor.ERROR + Messages.DATABASE_ERROR);
      return true;
    }

    if (packages.isEmpty())
    {
      p.sendMessage(MessageAlertColor.NOTIFY_AGNOSTIC + "Your mailbox is empty.");
      return true;
    }

    int i = 1;
    StringBuilder builder = new StringBuilder();
    for (ItemPackage pkg : packages)
    {
      builder.append(ChatColor.YELLOW).append(i).append(") ");
      builder.append(ChatColor.WHITE).append(pkg.quantity).append(" x ").append(pkg.item.alias).append("\n");
      ++i;
    }

    // TODO page numbers
    StringPages.from(builder.toString(), 6).printTo(p, 0);
    return true;
  }
}
