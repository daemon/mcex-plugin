package xyz.mcex.plugin.account;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import xyz.mcex.plugin.SubCommandExecutor;

public class CancelCommand implements SubCommandExecutor
{
  @Override
  public String getUsage()
  {
    return "/mcex cancel <order number>. See /mcex account for your orders.";
  }

  @Override
  public String getPermissionName()
  {
    return null;
  }

  @Override
  public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings)
  {
    return false;
  }
}
