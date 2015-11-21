package xyz.mcex.plugin.equity.database;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import xyz.mcex.plugin.DatabaseManager;
import xyz.mcex.plugin.Database;
import xyz.mcex.plugin.internals.Nullable;
import xyz.mcex.plugin.util.PlayerUtils;

import java.io.*;
import java.nio.ByteBuffer;
import java.sql.*;
import java.util.LinkedList;
import java.util.List;
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

    return this.addItem(m);
  }

  public boolean addItem(Material m) throws SQLException, ItemNotFoundException
  {
    Connection conn = null;
    PreparedStatement stmt = null;

    try
    {
      conn = manager().getConnection();
      stmt = this._createInsertItemStmt(conn);
      stmt.setString(1, m.name());
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

  private PutOrderResponse putOrder(UUID playerUuid, String itemName, int quantity, double price, boolean isBuy) throws SQLException
  {
    assert(quantity > 0);
    assert(price > 0);
    assert(playerUuid != null);
    Connection conn = null;
    PreparedStatement putOrderStmt = null;
    PreparedStatement getOrderStmt = null;
    PreparedStatement deleteOrdersStmt = null;
    PreparedStatement getIsomorphicOrderStmt = null;
    PreparedStatement updateOrderStmt = null;
    ResultSet getOrderRs = null;

    try
    {
      conn = this.manager().getConnection();
      conn.setAutoCommit(false);
      int rowId = this.getItemId(itemName, conn);

      byte[] uuidBytes;
      try
      {
        uuidBytes = PlayerUtils.uuidToBytes(playerUuid);
      } catch (IOException e)
      {
        return new PutOrderResponse(PutOrderResponse.ResponseCode.FAILURE_NOT_FOUND);
      }

      // Attempt to merge with existing orders
      getIsomorphicOrderStmt = this._createGetOrderStmt(conn, isBuy);
      getIsomorphicOrderStmt.setDouble(1, price);
      getIsomorphicOrderStmt.setBinaryStream(2, new ByteArrayInputStream(uuidBytes), 16);
      getIsomorphicOrderStmt.setInt(3, rowId);

      ResultSet isoRs = getIsomorphicOrderStmt.executeQuery();
      if (isoRs.next())
      {
        updateOrderStmt = this._createUpdateItemStmt(conn, isBuy);
        updateOrderStmt.setInt(1, quantity);
        updateOrderStmt.setInt(2, isoRs.getInt(1));
        isoRs.close();

        updateOrderStmt.execute();
        conn.commit();
        return new PutOrderResponse(PutOrderResponse.ResponseCode.OK);
      }

      // List actual order
      getOrderStmt = this._createGetOrdersStmt(conn, !isBuy);
      getOrderStmt.setDouble(1, price);
      getOrderStmt.setInt(2, rowId);
      getOrderRs = getOrderStmt.executeQuery();

      Stack<Order> orders = new Stack<>();
      int totalMoney = 0;
      int totalQuantity = 0;
      int orderDelta = 0;
      InputStream uuidStream = null;
      int orderItemId = 0;

      while (getOrderRs.next())
      {
        if (quantity <= 0)
          break;

        int orderId = getOrderRs.getInt(1);
        uuidStream = getOrderRs.getBinaryStream(2);
        UUID orderUuid = null;
        try
        {
          orderUuid = PlayerUtils.streamToUuid(uuidStream);
        } catch (IOException e)
        {
          e.printStackTrace();
          return new PutOrderResponse(PutOrderResponse.ResponseCode.FAILURE_NOT_FOUND);
        }

        orderItemId = getOrderRs.getInt(3);
        int orderQuantity = getOrderRs.getInt(4);
        double orderPrice = getOrderRs.getDouble(5);

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
          putOrderStmt.setBinaryStream(1, new ByteArrayInputStream(uuidBytes), 16);
          putOrderStmt.setInt(2, orderItemId);
          putOrderStmt.setInt(3, lastOrder.quantity - orderDelta);
          putOrderStmt.setDouble(4, lastOrder.price);
          putOrderStmt.execute();

          orders.pop();
          orders.push(new Order(lastOrder.rowId, lastOrder.playerUuid, lastOrder.quantity - orderDelta, lastOrder.price));
        }

        deleteOrdersStmt = this._createDeleteOrdersStmt(conn, !isBuy);
      }

      PutOrderResponse response = new PutOrderResponse(PutOrderResponse.ResponseCode.OK, totalMoney, totalQuantity);

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

      putOrderStmt.close();
      putOrderStmt = this._createInsertOrderStmt(conn, isBuy);
      putOrderStmt.setBinaryStream(1, new ByteArrayInputStream(uuidBytes), 16);
      putOrderStmt.setInt(2, rowId);
      putOrderStmt.setInt(3, quantity);
      putOrderStmt.setDouble(4, price);

      putOrderStmt.execute();
      conn.commit();
      return response;
    } catch (SQLException e) {
      if (conn != null && !conn.getAutoCommit())
        conn.rollback();
      return new PutOrderResponse(PutOrderResponse.ResponseCode.FAILURE_SQL);
    } catch (ItemNotFoundException e)
    {
      return new PutOrderResponse(PutOrderResponse.ResponseCode.FAILURE_NOT_FOUND);
    } finally {
      if (updateOrderStmt != null)
        updateOrderStmt.close();
      if (deleteOrdersStmt != null)
        deleteOrdersStmt.close();
      if (getOrderRs != null)
        getOrderRs.close();
      if (getOrderStmt != null)
        getOrderStmt.close();
      if (putOrderStmt != null)
        putOrderStmt.close();
      if (getIsomorphicOrderStmt != null)
        getIsomorphicOrderStmt.close();
      if (conn != null)
      {
        conn.setAutoCommit(true);
        conn.close();
      }
    }
  }

  public GetOrderResponse getOrders(String itemName, int startLimit, int nItems, boolean isBuy) throws SQLException
  {
    List<Order> orders = new LinkedList<>();
    Connection conn = null;
    PreparedStatement stmt = null;
    ResultSet rs = null;

    try
    {
      conn = this.manager().getConnection();
      conn.setAutoCommit(false);

      int id = this.getItemId(itemName, conn);

      stmt = this._createGetOrdersByLimit(conn, isBuy);
      stmt.setInt(1, id);
      stmt.setInt(2, startLimit);
      stmt.setInt(3, nItems);

      rs = stmt.executeQuery();
      while (rs.next())
      {
        InputStream uuidStream = rs.getBinaryStream(2);
        try
        {
          orders.add(new Order(rs.getInt(1), PlayerUtils.streamToUuid(uuidStream), rs.getInt(4), rs.getDouble(5)));
        } catch (IOException ignored) {}
      }

      return new GetOrderResponse(GetOrderResponse.ResponseCode.OK, orders);
    } catch (SQLException e) {
      return new GetOrderResponse(GetOrderResponse.ResponseCode.FAILURE_SQL);
    } catch (ItemNotFoundException e)
    {
      return new GetOrderResponse(GetOrderResponse.ResponseCode.FAILURE_NOT_FOUND);
    } finally {
      if (rs != null)
        rs.close();
      if (stmt != null)
        stmt.close();
      if (conn != null)
      {
        conn.setAutoCommit(true);
        conn.close();
      }
    }
  }

  public PutOrderResponse putBuyOrder(UUID playerUuid, String itemName, int quantity, double price) throws SQLException
  {
    return this.putOrder(playerUuid, itemName, quantity, price, true);
  }

  public PutOrderResponse putSellOrder(UUID playerUuid, String itemName, int quantity, double price) throws SQLException
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

  private PreparedStatement _createGetOrdersByLimit(Connection connection, boolean isBuy) throws SQLException
  {
    if (isBuy)
      return connection.prepareStatement("SELECT * FROM equity_buy_orders WHERE item_id = ? ORDER BY offer_value DESC LIMIT ?,?");
    else
      return connection.prepareStatement("SELECT * FROM equity_sell_orders WHERE item_id = ? ORDER BY offer_value ASC LIMIT ?,?");
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
    return connection.prepareStatement("INSERT INTO items (name) VALUES (?)");
  }

  private PreparedStatement _createUpdateItemStmt(Connection connection, boolean isBuy) throws SQLException
  {
    if (isBuy)
      return connection.prepareStatement("UPDATE equity_buy_orders SET quantity = quantity + ? WHERE id = ?");
    else
      return connection.prepareStatement("UPDATE equity_sell_orders SET quantity = quantity + ? WHERE id = ?");
  }

  private PreparedStatement _createGetOrderStmt(Connection connection, boolean isBuy) throws SQLException
  {
    if (isBuy)
      return connection.prepareStatement("SELECT id FROM equity_buy_orders WHERE offer_value = ? AND player_uuid = ? AND item_id = ?");
    else
      return connection.prepareStatement("SELECT id FROM equity_sell_orders WHERE offer_value = ? AND player_uuid = ? AND item_id = ?");
  }

  private PreparedStatement _createInsertOrderStmt(Connection connection, boolean isBuy) throws SQLException
  {
    if (isBuy)
      return connection.prepareStatement("INSERT INTO equity_buy_orders (player_uuid, item_id, quantity, offer_value) VALUES (?, ?, ?, ?)");
    else
      return connection.prepareStatement("INSERT INTO equity_sell_orders (player_uuid, item_id, quantity, offer_value) VALUES (?, ?, ?, ?)");
  }
}
