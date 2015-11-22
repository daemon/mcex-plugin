package xyz.mcex.plugin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import xyz.mcex.plugin.message.HelpPagesBuilder;
import xyz.mcex.plugin.message.Pages;

public class AdminCommand implements SubCommandExecutor
{
  private final Pages _pages;

  public AdminCommand()
  {
    HelpPagesBuilder builder = new HelpPagesBuilder();
    builder.addHelp("mcex additem", "Registers an item for trade");
    this._pages = builder.toPages();
  }

  @Override
  public String getUsage()
  {
    return "/mcex admin";
  }

  @Override
  public String getPermissionName()
  {
    return "mcex.admin";
  }

  @Override
  public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings)
  {
    this._pages.printTo(commandSender, 0);
    return true;
  }
}
