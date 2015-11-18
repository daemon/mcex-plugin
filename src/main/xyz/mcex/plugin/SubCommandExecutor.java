package xyz.mcex.plugin;

import org.bukkit.command.CommandExecutor;

public interface SubCommandExecutor extends CommandExecutor
{
  public String getUsage();
}
