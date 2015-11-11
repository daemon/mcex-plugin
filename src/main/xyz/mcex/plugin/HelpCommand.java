package xyz.mcex.plugin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import xyz.mcex.plugin.message.MessageAlertColor;

import java.util.HashMap;
import java.util.Map;

public class HelpCommand implements CommandExecutor
{
  private static final Map<String, String> _commandToHelp = new HashMap<>();

  static
  {
    _commandToHelp.put("buy", "Usage: /mcex buy item_name quantity\nPurchases item_name at the lowest price of the sell listings.\n" +
        "Example: /mcex buy dirt 10");
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
  {
    if (args.length == 0)
      return false;
    else if (_commandToHelp.get(args[0].toLowerCase()) == null)
      return false;

    String helpText = _commandToHelp.get(args[0].toLowerCase());
    sender.sendMessage(MessageAlertColor.INFO + helpText);
    return true;
  }
}
