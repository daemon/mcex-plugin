package xyz.mcex.plugin.equity.database;

import xyz.mcex.plugin.Database;
import xyz.mcex.plugin.DatabaseManager;
import xyz.mcex.plugin.util.PlayerUtils;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

public class OrderHistoryDatabase extends Database
{
  public OrderHistoryDatabase(DatabaseManager manager)
  {
    super(manager);
  }

  public void logTrade(Connection conn, UUID seller, UUID buyer, int quantity, double soldValue, int itemRowId) throws SQLException, IOException
  {
    PreparedStatement stmt = conn.prepareStatement("INSERT INTO equity_sell_history (player_uuid_seller, player_uuid_buyer, item_id, quantity, offer_value) " +
        "VALUES (?, ?, ?, ?, ?)");
    stmt.setBinaryStream(1, PlayerUtils.uuidToStream(seller));
    stmt.setBinaryStream(2, PlayerUtils.uuidToStream(buyer));
    stmt.setInt(3, itemRowId);
    stmt.setInt(4, quantity);
    stmt.setDouble(5, soldValue);
    stmt.execute();
  }
}
