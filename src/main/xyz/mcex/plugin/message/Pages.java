package xyz.mcex.plugin.message;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public interface Pages
{
  public void printTo(CommandSender sender, int pageIndex);
  public String getPage(int index) throws Exception;
}
