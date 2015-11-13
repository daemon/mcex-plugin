package xyz.mcex.plugin.equity;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class BuyCommand implements CommandExecutor
{
  private final Economy _economy;

  public BuyCommand(Economy economy)
  {
    this._economy = economy;
  }

  @Override
  public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings)
  {

  }
}
