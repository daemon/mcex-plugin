package xyz.mcex.plugin.equity.database;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import xyz.mcex.plugin.DatabaseManager;
import xyz.mcex.plugin.Database;
import xyz.mcex.plugin.internals.Nullable;

import java.io.*;
import java.nio.ByteBuffer;
import java.sql.*;
import java.util.Stack;
import java.util.UUID;

public class EquityDatabase extends Database
{
  public EquityDatabase(DatabaseManager manager)
  {
    super(manager);
  }

  public boolean addItem(String name) throws SQLException, ItemNotFoundException
  {
    Material m = Material.getMaterial(name.toUpperCase());
    if (m == null)
      throw new ItemNotFoundException();

    ItemStack itemStack = new ItemStack(m, 1);
    int id = itemStack.getTypeId();
    byte dv = itemStack.getData().getData();

   return this.addItem(id, dv);
  }

  public boolean addItem(int id, byte dv) throws SQLException, ItemNotFoundException
  {
    Connection conn = null;
    PreparedStatement stmt = null;

    Material m = Material.getMaterial(id);
    if (m == null)
      throw new ItemNotFoundException();

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

  public int getItemId(String name, @Nullable Connection connection) throws SQLException, ItemNotFoundException
  {
    name = name.toUpperCase();
    Material m = Material.getMaterial(name);
    if (m  == null)
      throw new ItemNotFoundException();

    boolean externalConnection = connection != null;
    Connection conn = connection;
    PreparedStatement stmt = null;
    ResultSet rs = null;

    try
    {
      if (!externalConnection)
        conn = manager().getConnection();
      stmt = conn.prepareStatement("SELECT id FROM items WHERE name = ? LOCK IN SHARE MODE");
      stmt.setString(1, name.substring(0, Math.min(32, name.length())));
      rs = stmt.executeQuery();
      if (rs.next())
        return rs.getInt(1);

      throw new ItemNotFoundException();
    } finally {
      if (rs != null)
        rs.close();
      if (stmt != null)
        stmt.close();
      if (conn != null && !externalConnection)
        conn.close();
    }
  }

  private Long fromBytes(byte[] bytes)
  {
    ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
    buffer.put(bytes);
    buffer.flip();
    return buffer.getLong();
  }

  private PutOrderResponse putOrder(UUID playerUuid, String itemName, int quantity, int price, boolean isBuy) throws SQLException, ItemNotFoundException
  {
    assert(quantity > 0);
    assert(price > 0);
    assert(playerUuid != null);
    Connection conn = null;
    PreparedStatement putOrderStmt = null;
    PreparedStatement getOrderStmt = null;
    PreparedStatement deleteOrdersStmt = null;
    ResultSet getOrderRs = null;

    try
    {
      conn = this.manager().getConnection();
      conn.setAutoCommit(false);
      int rowId = this.getItemId(itemName, conn);

      getOrderStmt = this._createGetOrdersStmt(conn, isBuy);
      getOrderStmt.setInt(1, price);
      getOrderStmt.setInt(2, rowId);
      getOrderRs = getOrderStmt.executeQuery();

      Stack<Order> orders = new Stack<>();
      int totalMoney = 0;
      int totalQuantity = 0;
      int orderDelta = 0;
      Blob uuidBlob = null;
      int orderItemId = 0;

      while (getOrderRs.next())
      {
        if (quantity <= 0)
          continue;

        int orderId = getOrderRs.getInt(1);
        uuidBlob = getOrderRs.getBlob(2);
        UUID orderUuid = new UUID(this.fromBytes(uuidBlob.getBytes(1, 8)),
            this.fromBytes(uuidBlob.getBytes(8, 8)));

        orderItemId = getOrderRs.getInt(3);
        int orderQuantity = getOrderRs.getInt(4);
        int orderPrice = getOrderRs.getInt(5);

        orderDelta = Math.min(orderQuantity, quantity);
        quantity -= orderDelta;

        orders.push(new Order(orderId, orderUuid, orderQuantity, orderPrice));
        totalMoney += orderPrice * orderDelta;
        totalQuantity += orderDelta;
      }

      getOrderRs.close();
      putOrderStmt = this._createInsertOrderStmt(conn, !isBuy);

      if (!orders.empty())
      {
        Order lastOrder = orders.peek();
        if (orderDelta != 0 && lastOrder.quantity != orderDelta)
        {
          putOrderStmt.setBlob(1, uuidBlob);
          putOrderStmt.setInt(2, orderItemId);
          putOrderStmt.setInt(3, lastOrder.quantity - orderDelta);
          putOrderStmt.setInt(4, lastOrder.price);
          putOrderStmt.execute();

          orders.pop();
          orders.push(new Order(lastOrder.rowId, lastOrder.playerUuid, lastOrder.quantity - orderDelta, lastOrder.price));
        }

        deleteOrdersStmt = this._createDeleteOrdersStmt(conn, !isBuy);
      }

      PutOrderResponse response = new PutOrderResponse(totalMoney, totalQuantity);

      while (!orders.empty())
      {
        assert (deleteOrdersStmt != null);
        Order order = orders.pop();
        deleteOrdersStmt.setInt(1, order.rowId);
        deleteOrdersStmt.execute();

        response.playerUuidToMoney.put(order.playerUuid, order.price * order.quantity);
      }

      if (quantity == 0)
        return response;

      ByteArrayOutputStream ba = new ByteArrayOutputStream(16);
      DataOutputStream os = new DataOutputStream(ba);
      try
      {
        os.writeLong(playerUuid.getMostSignificantBits());
        os.writeLong(playerUuid.getLeastSignificantBits());
      } catch (IOException e)
      {
        return new PutOrderResponse();
      }

      putOrderStmt.close();
      putOrderStmt = this._createInsertOrderStmt(conn, isBuy);
      putOrderStmt.setBinaryStream(1, new ByteArrayInputStream(ba.toByteArray()), 16);
      putOrderStmt.setInt(2, rowId);
      putOrderStmt.setInt(3, quantity);
      putOrderStmt.setInt(4, price);

      putOrderStmt.execute();
      conn.commit();
      return response;
    } catch (SQLException e) {
      if (conn != null && !conn.getAutoCommit())
        conn.rollback();
      throw e;
    } finally {
      if (deleteOrdersStmt != null)
        deleteOrdersStmt.close();
      if (getOrderRs != null)
        getOrderRs.close();
      if (getOrderStmt != null)
        getOrderStmt.close();
      if (putOrderStmt != null)
        putOrderStmt.close();
      if (conn != null)
      {
        conn.setAutoCommit(true);
        conn.close();
      }
    }
  }

  public PutOrderResponse putBuyOrder(UUID playerUuid, String itemName, int quantity, int price) throws SQLException, ItemNotFoundException
  {
    return this.putOrder(playerUuid, itemName, quantity, price, true);
  }

  public PutOrderResponse putSellOrder(UUID playerUuid, String itemName, int quantity, int price) throws SQLException, ItemNotFoundException
  {
    return this.putOrder(playerUuid, itemName, quantity, price, false);
  }

  private PreparedStatement _createGetOrdersStmt(Connection connection, boolean isBuy) throws SQLException
  {
    if (isBuy)
      return connection.prepareStatement("SELECT * FROM equity_buy_orders WHERE offer_value >= ? AND item_id = ? ORDER BY offer_value DESC FOR UPDATE");
    else
      return connection.prepareStatement("SELECT * FROM equity_sell_orders WHERE offer_value <= ? AND item_id = ? ORDER BY offer_value ASC FOR UPDATE");
  }

  private PreparedStatement _createDeleteOrdersStmt(Connection connection, boolean isBuy) throws SQLException
  {
    if (isBuy)
      return connection.prepareStatement("DELETE FROM equity_buy_orders WHERE id = ?");
    else
      return connection.prepareStatement("DELETE FROM equity_sell_orders WHERE id = ?");
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
