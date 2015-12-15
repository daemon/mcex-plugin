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
    builder.addHelp("mcex account", "View your current outstanding orders.");
    builder.addHelp("mcex list", "View current item prices.");
    builder.addHelp("mcex recent", "View recent offers.");
    builder.addHelp("mcex chart", "View a graphical display of item prices.");
    builder.addHelp("mcex mailbox", "View your item mailbox for bought items.");
    // builder.addHelp("mcex help <command>", "Shows detailed help for an MCEX subcommand.");
    builder.addHelp("mcex search", "Search for currently registered items");
    builder.addHelp("mcex admin", "Admin commands");
    this._helpPages = builder.toPages();
  }

  public SubCommandExecutor getCommandExecutor(String subCommand)
  {
    return this._subcommandToExecutor.get(subCommand);
  }

  public void registerCommand(String subCommand, SubCommandExecutor executor)
  {
    this._subcommandToExecutor.put(subCommand.toLowerCase(), executor);
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
  {
    boolean isPageNo = false;
    int pageNo = 0;

    if (args.length > 0)
      try
      {
        pageNo = Integer.parseInt(args[0]) - 1;
        isPageNo = true;
      } catch (NumberFormatException ignored) {
      }
    if (args.length == 0 || isPageNo)
    {
      this._helpPages.printTo(sender, pageNo);
      return true;
    }

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
