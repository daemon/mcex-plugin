package xyz.mcex.plugin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import xyz.mcex.plugin.message.HelpPagesBuilder;
import xyz.mcex.plugin.message.MessageAlertColor;
import xyz.mcex.plugin.message.Messages;
import xyz.mcex.plugin.message.Pages;

import java.util.HashMap;
import java.util.Map;

public class BaseCommand implements CommandExecutor
{
  private Pages _helpPages;
  private Map<String, SubCommandExecutor> _subcommandToExecutor = new HashMap<>();

  public BaseCommand()
  {
    HelpPagesBuilder builder = new HelpPagesBuilder();
    builder.addHelp("mcex buy", "Purchase some items.");
    builder.addHelp("mcex sell", "Sell some items.");
    builder.addHelp("mcex cancel", "Cancel an order. See /mcex account for orders");
    builder.addHelp("mcex account", "View your current outstanding orders.");
    builder.addHelp("mcex list", "View current item prices.");
    builder.addHelp("mcex mailbox", "View your item mailbox for bought items.");
    builder.addHelp("mcex accept", "Accept an item package in your mailbox.");
    builder.addHelp("mcex help <command>", "Shows detailed help for an MCEX subcommand.");
    builder.addHelp("mcex admin", "Admin commands");
    this._helpPages = builder.toPages();
  }

  public void registerCommand(String subCommand, SubCommandExecutor executor)
  {
    this._subcommandToExecutor.put(subCommand.toLowerCase(), executor);
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
  {
    if (args.length == 0)
    {
      this._helpPages.printTo(sender, 0);
      return true;
    }

    // TODO page numbers
    String cmd = args[0].toLowerCase();
    SubCommandExecutor executor = this._subcommandToExecutor.get(cmd);
    if (executor == null)
      return false;

    if (!sender.hasPermission(executor.getPermissionName()))
    {
      sender.sendMessage(MessageAlertColor.ERROR + Messages.NO_PERMISSION);
      return true;
    }

    if (!executor.onCommand(sender, command, label, args))
      sender.sendMessage(MessageAlertColor.ERROR + "Usage: " + executor.getUsage());
    return true;
  }
}
