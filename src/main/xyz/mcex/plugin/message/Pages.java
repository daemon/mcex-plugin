package xyz.mcex.plugin.message;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class Pages
{
  public List<String> pages = new ArrayList<>();
  public Pages add(String page)
  {
    this.pages.add(page);
    return this;
  }

  public void printTo(CommandSender output, int pageIndex)
  {
    if (pageIndex >= pages.size())
    {
      output.sendMessage(MessageAlertColor.ERROR + "No such page number!");
      return;
    }

    output.sendMessage(ChatColor.DARK_GRAY + "---=" + ChatColor.GRAY + " Page " + (pageIndex + 1) + "/" + this.pages.size() + ChatColor.DARK_GRAY + " =--");
    output.sendMessage(pages.get(pageIndex).trim());
  }

  public static Pages from(String text, int rowsPerPage)
  {
    String[] lines = text.split("\n");
    Pages pages = new Pages();
    StringBuilder builder = new StringBuilder();

    for (int i = 0; i < lines.length; ++i)
    {
      if (i % rowsPerPage == 0 && i != 0)
      {
        pages.add(builder.toString());
        builder = new StringBuilder();
      }

      builder.append(lines[i]).append("\n");
    }

    if (lines.length % rowsPerPage != 0)
      pages.add(builder.toString());
    return pages;
  }
}
