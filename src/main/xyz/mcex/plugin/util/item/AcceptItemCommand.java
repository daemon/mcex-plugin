package xyz.mcex.plugin.util.item;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import xyz.mcex.plugin.DatabaseManager;
import xyz.mcex.plugin.SubCommandExecutor;
import xyz.mcex.plugin.equity.database.EquityDatabase;
import xyz.mcex.plugin.message.MessageAlertColor;
import xyz.mcex.plugin.message.Messages;

import java.util.NoSuchElementException;

public class AcceptItemCommand implements SubCommandExecutor
{
  private final EquityDatabase _eqDb;
  private final ItemPackageDatabase _ipDb;

  public AcceptItemCommand(DatabaseManager manager)
  {
    this._eqDb = new EquityDatabase(manager);
    this._ipDb = new ItemPackageDatabase(manager, this._eqDb);
  }

  @Override
  public String getUsage()
  {
    return "/mcex accept [item package number|all]. You can view package numbers using /mcex mailbox";
  }

  @Override
  public String getPermissionName()
  {
    return "mcex.cmd.itemmail.accept";
  }

  @Override
  public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args)
  {
    if (!(commandSender instanceof Player))
    {
      commandSender.sendMessage(MessageAlertColor.ERROR + Messages.PLAYER_CMD_ERROR);
      return true;
    }

    if (args.length < 2)
      return false;

    Integer orderNo;
    try
    {
      orderNo = Integer.parseInt(args[1]);
    } catch (NumberFormatException e) {
      return false;
    }

    if (orderNo < 1)
    {
      commandSender.sendMessage(MessageAlertColor.ERROR + "Order number must be positive.");
      return true;
    }

    Player p = (Player) commandSender;

    try
    {
      ItemPackage pkg = this._ipDb.fetchPackage(p.getUniqueId(), orderNo);
      (new AcceptItemPackageTask(this._ipDb, pkg)).run();
    } catch (NoSuchElementException e) {
      p.sendMessage(MessageAlertColor.ERROR + "Item package ID does not exist.");
    } catch (Exception e)
    {
      e.printStackTrace();
      p.sendMessage(MessageAlertColor.ERROR + Messages.DATABASE_ERROR);
    }

    return true;
  }
}
