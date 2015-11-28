package xyz.mcex.plugin.equity.database;

import xyz.mcex.plugin.Database;
import xyz.mcex.plugin.DatabaseManager;
import xyz.mcex.plugin.util.player.PlayerUtils;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public class OrderHistoryDatabase extends Database
{
  public OrderHistoryDatabase(DatabaseManager manager)
  {
    super(manager);
  }

  public void logTrade(Connection conn, int sellerId, int buyerId, int quantity, double soldValue, int itemRowId) throws SQLException, IOException
  {
    PreparedStatement stmt = conn.prepareStatement("INSERT INTO equity_sell_history (player_uuid_seller, player_uuid_buyer, item_id, quantity, offer_value) " +
        "VALUES (?, ?, ?, ?, ?)");
    stmt.setInt(1, sellerId);
    stmt.setInt(2, buyerId);
    stmt.setInt(3, itemRowId);
    stmt.setInt(4, quantity);
    stmt.setDouble(5, soldValue);
    stmt.execute();
  }

  public List<Trade> getTrades(int itemRowId) throws SQLException
  {
    Connection conn = this.manager().getConnection();
    try
    {
      PreparedStatement stmt = conn.prepareStatement("SELECT quantity, offer_value, ts FROM equity_sell_history WHERE item_id = ? ORDER BY ts DESC LIMIT 500");
      List<Trade> trades = new LinkedList<>();
      stmt.setInt(1, itemRowId);
      ResultSet rs = stmt.executeQuery();
      while (rs.next())
      {
        Trade t = new Trade(rs.getInt(1), rs.getDouble(2), rs.getTimestamp(3).getTime());
        trades.add(t);
      }
      rs.close();

      return trades;
    } finally {
      if (conn != null)
        conn.close();
    }
  }

  public static class Trade
  {
    public final int quantity;
    public final double value;
    public final long timeStamp;

    public Trade(int quantity, double value, long timeStamp)
    {
      this.quantity = quantity;
      this.value = value;
      this.timeStamp = timeStamp;
    }
  }
}
