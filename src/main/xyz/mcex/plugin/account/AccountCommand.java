package xyz.mcex.plugin.account;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.mcex.plugin.DatabaseManager;
import xyz.mcex.plugin.SubCommandExecutor;
import xyz.mcex.plugin.gui.MenuFlow;
import xyz.mcex.plugin.message.MessageAlertColor;
import xyz.mcex.plugin.message.Messages;

import java.util.UUID;

public class AccountCommand implements SubCommandExecutor
{
  private final DatabaseManager _manager;
  private final Economy _economy;

  public AccountCommand(DatabaseManager manager, Economy economy)
  {
    this._manager = manager;
    this._economy = economy;
  }

  @Override
  public String getUsage()
  {
    return "/mcex account <buy|sell>";
  }

  @Override
  public String getPermissionName()
  {
    return "mcex.cmd";
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String s, String[] args)
  {
    if (!(sender instanceof Player))
    {
      sender.sendMessage(MessageAlertColor.ERROR + Messages.PLAYER_CMD_ERROR);
      return true;
    }

    if (args.length <= 1)
      return false;

    Player p = (Player) sender;
    boolean isBuy = args[1].equalsIgnoreCase("buy");
    if (!isBuy && !args[1].equalsIgnoreCase("sell"))
      return false;
    String prelude = args[1].toLowerCase();

    int pageNo = 0;
    if (args.length >= 3)
    {
      try
      {
        pageNo = Integer.parseInt(args[2]) - 1;
      } catch (NumberFormatException e) {
        sender.sendMessage("Page number has to be an integer!");
        return true;
      }
    }

    /* UUID playerUuid = p.getUniqueId();
    final int finalPageNo = pageNo;
    p.sendMessage(MessageAlertColor.NOTIFY_AGNOSTIC + "Processing...");

    Bukkit.getScheduler().runTaskAsynchronously(_plugin, () -> {
      AccountPages pages = new AccountPages(this._manager, playerUuid, isBuy);
      String text = pages.getPage(finalPageNo);
      if (text == null)
        text = MessageAlertColor.ERROR + Messages.DATABASE_ERROR;
      else if (text.equals(""))
        text = MessageAlertColor.NOTIFY_AGNOSTIC + "You have no " + prelude + " orders in your account.";
      else
        text += MessageAlertColor.INFO + "/mcex account " + prelude + " " + (finalPageNo + 2) + " for the next page.";

      final String finalText = text;
      Bukkit.getScheduler().runTask(this._plugin, () -> p.sendMessage(MessageAlertColor.INFO + "Your " + prelude + " orders\n" + finalText));
    });
    return true;*/
    AccountGui gui = new AccountGui(p, this._manager, pageNo, isBuy, this._economy);
    gui.setNextClickListener(gui.createListener(pageNo)); // todo: refactor
    gui.show();

    return true;
  }
}
