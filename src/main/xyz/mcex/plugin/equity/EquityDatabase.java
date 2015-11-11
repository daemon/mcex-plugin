package xyz.mcex.plugin.equity;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import xyz.mcex.plugin.DatabaseManager;
import xyz.mcex.plugin.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class EquityDatabase extends Database
{
  public EquityDatabase(DatabaseManager manager)
  {
    super(manager);
  }

  public boolean addItem(String name) throws IllegalArgumentException, SQLException
  {
    Material m = Material.getMaterial(name.toUpperCase());
    if (m == null)
      throw new IllegalArgumentException("Item not found.");

    ItemStack itemStack = new ItemStack(m, 1);
    int id = itemStack.getTypeId();
    byte dv = itemStack.getData().getData();

   return this.addItem(id, dv);
  }

  public boolean addItem(int id, byte dv) throws SQLException
  {
    Connection conn = null;
    PreparedStatement stmt = null;

    Material m = Material.getMaterial(id);
    if (m == null)
      throw new IllegalArgumentException("Item not found.");

    try
    {
      conn = manager().getConnection();
      stmt = this._createInsertItemStmt(conn);
      stmt.setInt(1, id);
      stmt.setByte(2, dv);
      boolean rc = stmt.execute();

      stmt.close();
      conn.close();
      return rc;
    } finally {
      if (stmt != null)
        stmt.close();
      if (conn != null)
        conn.close();
    }
  }

  public int getItemId(int id, byte dv) throws SQLException
  {
    Material m = Material.getMaterial(id);
    if (m  == null)
      throw new IllegalArgumentException("Item not found.");

    Connection conn = null;
    PreparedStatement stmt = null;
    ResultSet rs = null;

    try
    {
      conn = manager().getConnection();
      stmt = conn.prepareStatement("SELECT id FROM items WHERE item_id = ? AND item_dv = ?");
      rs = stmt.executeQuery();
      if (rs.next())
        return rs.getInt(1);

      throw new IllegalArgumentException("Item not registered for trade.");
    } finally {
      if (rs != null)
        rs.close();
      if (stmt != null)
        stmt.close();
      if (conn != null)
        conn.close();
    }
  }

  public boolean putBuyOrder(UUID playerUuid, int id, byte dv, int quantity, int price) throws SQLException
  {
    Connection conn = null;
    conn = this.manager().getConnection();
    conn.setAutoCommit(false);

  }

  public boolean putSellOrder(UUID playerUuid, int id, byte dv, int quantity, int price) throws SQLException
  {
    Connection conn = null;
    conn = this.manager().getConnection();
    conn.setAutoCommit(false);
  }


  private PreparedStatement _createInsertItemStmt(Connection connection) throws SQLException
  {
    return connection.prepareStatement("INSERT INTO items (item_id, item_dv) VALUES (?, ?)");
  }

  private PreparedStatement _createInsertOrderStmt(Connection connection, boolean isBuy) throws SQLException
  {
    if (isBuy)
      return connection.prepareStatement("INSERT INTO equity_buy_orders (player_uuid, item_id, quantity, offer_value) VALUES (?, ?, ?, ?)");
    else
      return connection.prepareStatement("INSERT INTO equity_sell_orders (player_uuid, item_id, quantity, offer_value) VALUES (?, ?, ?, ?)");
  }
}
