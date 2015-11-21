package xyz.mcex.plugin.equity.database;

import xyz.mcex.plugin.Database;
import xyz.mcex.plugin.DatabaseManager;

import java.util.UUID;

public class OrderHistoryDatabase extends Database
{
  public OrderHistoryDatabase(DatabaseManager manager)
  {
    super(manager);
  }

  // /m &8?&a?&c?&8???????????????? &7$50
  public void logTrade(UUID seller, UUID buyer, int quantity, double soldValue, int itemRowId)
  {

  }
}
