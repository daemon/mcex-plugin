package xyz.mcex.plugin.equity;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import xyz.mcex.plugin.DatabaseManager;
import xyz.mcex.plugin.SubCommandExecutor;
import xyz.mcex.plugin.equity.database.DuplicateItemException;
import xyz.mcex.plugin.equity.database.ItemDatabase;
import xyz.mcex.plugin.equity.database.ItemNotFoundException;
import xyz.mcex.plugin.message.MessageAlertColor;
import xyz.mcex.plugin.message.Messages;

import java.sql.SQLException;

public class RegisterItemCommand implements SubCommandExecutor
{
  private final DatabaseManager _manager;

  public RegisterItemCommand(DatabaseManager manager)
  {
    this._manager = manager;
  }

  @Override
  public String getUsage()
  {
    return "/mcex additem <material name|hand <alias>>";
  }

  @Override
  public String getPermissionName()
  {
    return "mcex.admin";
  }

  @Override
  public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args)
  {
    if (args.length < 2)
      return false;

    boolean isPlayer = commandSender instanceof Player;
    ItemDatabase database = new ItemDatabase(this._manager);

    if (!isPlayer && args[1].equalsIgnoreCase("hand"))
    {
      commandSender.sendMessage(MessageAlertColor.ERROR + Messages.PLAYER_CMD_ERROR);
      return false;
    } else if (isPlayer && args[1].equalsIgnoreCase("hand")) {
      Player p = (Player) commandSender;
      ItemStack handItem = p.getItemInHand();
      if (handItem == null)
      {
        commandSender.sendMessage(MessageAlertColor.ERROR + "You must have something in your hand!");
        return true;
      }

      String alias = handItem.getType().name();
      try
      {
        if (args.length < 3)
        {
          alias = (handItem.hasItemMeta() && handItem.getItemMeta().hasDisplayName()) ? handItem.getItemMeta().getDisplayName() : alias;
          database.addItem(handItem, alias);
        } else {
          alias = args[2];
          database.addItem(handItem, args[2]);
        }

        commandSender.sendMessage(MessageAlertColor.NOTIY_SUCCESS + alias.toUpperCase() + " added successfully.");
      } catch (SQLException e) {
        e.printStackTrace();
        commandSender.sendMessage(MessageAlertColor.ERROR + Messages.DATABASE_ERROR);
      } catch (DuplicateItemException e)
      {
        commandSender.sendMessage(MessageAlertColor.ERROR + "That item or alias is already registered.");
      }

      return true;
    }

    try
    {
      database.addItem(args[1]);
      commandSender.sendMessage(MessageAlertColor.NOTIY_SUCCESS + "Item added successfully");
    } catch (SQLException e)
    {
      e.printStackTrace();
      commandSender.sendMessage(MessageAlertColor.ERROR + Messages.DATABASE_ERROR);
    } catch (ItemNotFoundException e)
    {
      commandSender.sendMessage(MessageAlertColor.ERROR + "No item matches that name.");
    } catch (DuplicateItemException e)
    {
      commandSender.sendMessage(MessageAlertColor.ERROR + "That item or alias is already registered.");
    }

    return true;
  }
}
