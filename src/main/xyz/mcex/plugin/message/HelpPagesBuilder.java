package xyz.mcex.plugin.message;

import org.bukkit.ChatColor;

public class HelpPagesBuilder
{
  private StringBuilder strBuilder = new StringBuilder();

  public static String makeCommandHelp(String helpCommand, String description)
  {
    return ChatColor.GOLD + "/" + helpCommand + ": " + MessageAlertColor.INFO + description;
  }

  public HelpPagesBuilder addHelp(String helpCommand, String description)
  {
    strBuilder.append(makeCommandHelp(helpCommand, description)).append("\n");
    return this;
  }

  public Pages toPages()
  {
    return Pages.from(strBuilder.toString(), 6);
  }
}
