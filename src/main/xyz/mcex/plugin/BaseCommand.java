package xyz.mcex.plugin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import xyz.mcex.plugin.message.HelpPagesBuilder;
import xyz.mcex.plugin.message.Pages;

public class BaseCommand implements CommandExecutor
{
  private Pages _helpPages;

  public BaseCommand()
  {
    HelpPagesBuilder builder = new HelpPagesBuilder();
    builder.addHelp("/mcex buy", "Purchase some items.");
    builder.addHelp("/mcex sell", "Sell some items.");
    builder.addHelp("/mcex account", "View your current outstanding and completed orders.");
    builder.addHelp("/mcex cancel", "Cancel an order. See /mcex account for orders");
    builder.addHelp("/mcex list", "View listed orders.");
    builder.addHelp("/mcex mailbox", "View your item mailbox for bought items.");
    builder.addHelp("/mcex help <command>", "Shows detailed help for an MCEX subcommand.");
    this._helpPages = builder.toPages();
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
  {
    this._helpPages.printTo(sender, 0);
    return true;
  }
}
