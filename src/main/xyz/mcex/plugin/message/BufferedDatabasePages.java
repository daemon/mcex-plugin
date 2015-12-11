package xyz.mcex.plugin.message;

import org.bukkit.command.CommandSender;
import xyz.mcex.plugin.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public abstract class BufferedDatabasePages implements Pages
{
  private final DatabaseManager _manager;

  public BufferedDatabasePages(DatabaseManager manager)
  {
    this._manager = manager;
  }

  protected DatabaseManager manager()
  {
    return this._manager;
  }

  @Override
  public void printTo(CommandSender sender, int pageIndex)
  {
  }

  public abstract String getPage(int index);
}
