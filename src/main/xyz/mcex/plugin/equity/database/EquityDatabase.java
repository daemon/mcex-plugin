package xyz.mcex.plugin.equity.database;

import org.bukkit.entity.Player;
import xyz.mcex.plugin.DatabaseManager;
import xyz.mcex.plugin.Database;
import xyz.mcex.plugin.util.player.PlayerDatabase;
import xyz.mcex.plugin.util.player.PlayerUtils;

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

  private Long fromBytes(byte[] bytes)
  {
    ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
    buffer.put(bytes);
    buffer.flip();
    return buffer.getLong();
  }

  public int countOrders(UUID playerUuid) throws SQLException
  {
    Connection conn = null;
    PreparedStatement countPlayerStmt = null;
    ResultSet rs = null;
    try
    {
      conn = this.manager().getConnection();
      conn.setAutoCommit(false);
      PlayerDatabase playerDb = new PlayerDatabase(this.manager());
      int playerId = playerDb.fetchPlayerId(playerUuid, conn);

      countPlayerStmt = this._createCountOrdersStmt(conn);
      countPlayerStmt.setInt(1, playerId);
      countPlayerStmt.setInt(2, playerId);
      rs = countPlayerStmt.executeQuery();
      if (!rs.next())
        throw new SQLException("Couldn't execute player count!");

      conn.commit();
      return rs.getInt(1);
    } catch (IOException e)
    {
      throw new SQLException("Couldn't read player UUID");
    } catch (SQLException e) {
      if (conn != null)
        conn.rollback();
      throw e;
    } finally {
      if (countPlayerStmt != null)
        countPlayerStmt.close();
      if (rs != null)
        rs.close();
      if (conn != null)
      {
        conn.setAutoCommit(true);
        conn.close();
      }
    }
  }

  public boolean deleteOrder(UUID playerUuid, int orderId, boolean isBuy) throws SQLException
  {
    Connection conn = null;
    PreparedStatement deleteOrderStmt = null;

    try
    {
      conn = this.manager().getConnection();
      conn.setAutoCommit(false);
      deleteOrderStmt = this._createDeleteOrdersStmt(conn, isBuy);
      deleteOrderStmt.setInt(1, orderId);
      deleteOrderStmt.execute();
      conn.commit();
      return true;
    } catch (SQLException e) {
      if (conn != null)
        conn.rollback();
      throw e;
    } finally {
      if (deleteOrderStmt != null)
        deleteOrderStmt.close();
      if (conn != null)
        conn.close();
    }
  }

  public GetOrderResponse getOrders(UUID playerUuid, int limit, int nOrders, boolean isBuy) throws SQLException
  {
    Connection conn = null;
    PreparedStatement getOrderStmt = null;
    ResultSet rs = null;
    try
    {
      conn = this.manager().getConnection();
      PlayerDatabase pDb = new PlayerDatabase(this.manager());
      int playerId = pDb.fetchPlayerId(playerUuid, conn);

      getOrderStmt = this._createGetOrdersByPlayer(conn, isBuy);
      getOrderStmt.setInt(1, playerId);
      getOrderStmt.setInt(2, limit);
      getOrderStmt.setInt(3, nOrders);
      rs = getOrderStmt.executeQuery();

      ItemDatabase db = new ItemDatabase(this.manager());

      List<Order> orders = new LinkedList<>();
      while(rs.next())
      {
        RegisteredItem item = db.getItem(rs.getInt(3), conn);
        orders.add(new Order(rs.getInt(1), playerUuid, rs.getInt(4), rs.getDouble(5), item));
      }

      return new GetOrderResponse(GetOrderResponse.ResponseCode.OK, orders);
    } catch (IOException e)
    {
      throw new SQLException("Invalid UUID format!");
    } catch (ItemNotFoundException e)
    {
      throw new SQLException("Error: Inconsistent database, turn referential integrity checking on!");
    } finally {
      if (getOrderStmt != null)
        getOrderStmt.close();
      if (conn != null)
        conn.close();
    }
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

      ItemDatabase itemDb = new ItemDatabase(this.manager());
      RegisteredItem registeredItem = itemDb.getItem(itemName, conn);
      int rowId = registeredItem.id;

      PlayerDatabase playerDb = new PlayerDatabase(this.manager());
      int playerId = playerDb.fetchPlayerId(playerUuid, conn);

      // Attempt to merge with existing orders
      getIsomorphicOrderStmt = this._createGetOrderStmt(conn, isBuy);
      getIsomorphicOrderStmt.setDouble(1, price);
      getIsomorphicOrderStmt.setInt(2, playerId);
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
      int orderItemId = 0;
      int lastPlayerId = -1;

      OrderHistoryDatabase orderHistoryDb = new OrderHistoryDatabase(this.manager());
      Stack<Integer> orderDeltas = new Stack<>();

      while (getOrderRs.next())
      {
        if (quantity <= 0)
          break;

        int orderId = getOrderRs.getInt(1);
        int orderPlayerId = getOrderRs.getInt(2);

        orderItemId = getOrderRs.getInt(3);
        int orderQuantity = getOrderRs.getInt(4);
        double orderPrice = getOrderRs.getDouble(5);

        orderDelta = Math.min(orderQuantity, quantity);
        quantity -= orderDelta;

        try
        {
          if (isBuy)
            orderHistoryDb.logTrade(conn, orderPlayerId, playerId, orderDelta, orderPrice, rowId);
          else
            orderHistoryDb.logTrade(conn, playerId, orderPlayerId, orderDelta, orderPrice, rowId);

        } catch (IOException e)
        {
          return new PutOrderResponse(PutOrderResponse.ResponseCode.FAILURE_SQL);
        }

        orders.push(new Order(orderId, playerDb.getPlayerUuid(orderPlayerId, conn), orderQuantity, orderPrice, registeredItem));
        orderDeltas.add(orderDelta);
        totalMoney += orderPrice * orderDelta;
        totalQuantity += orderDelta;

        lastPlayerId = orderPlayerId;
      }

      getOrderRs.close();
      putOrderStmt = this._createInsertOrderStmt(conn, !isBuy);

      if (!orders.empty())
      {
        Order lastOrder = orders.peek();
        if (orderDelta != 0 && lastOrder.quantity != orderDelta)
        {
          putOrderStmt.setInt(1, lastPlayerId);
          putOrderStmt.setInt(2, orderItemId);
          putOrderStmt.setInt(3, lastOrder.quantity - orderDelta);
          putOrderStmt.setDouble(4, lastOrder.price);
          putOrderStmt.execute();

          orders.pop();
          orders.push(new Order(lastOrder.rowId, lastOrder.playerUuid, lastOrder.quantity - orderDelta, lastOrder.price, registeredItem));
        }

        deleteOrdersStmt = this._createDeleteOrdersStmt(conn, !isBuy);
      }

      PutOrderResponse response = new PutOrderResponse(PutOrderResponse.ResponseCode.OK, totalMoney, totalQuantity, registeredItem);

      while (!orders.empty())
      {
        assert (deleteOrdersStmt != null);
        Order order = orders.pop();
        deleteOrdersStmt.setInt(1, order.rowId);
        deleteOrdersStmt.execute();

        Integer orderDel = orderDeltas.pop();
        response.playerUuidToMoney.put(order.playerUuid, order.price * orderDel);
        response.playerUuidToQuantity.put(order.playerUuid, orderDel);
      }

      if (quantity == 0)
        return response;

      putOrderStmt.close();
      putOrderStmt = this._createInsertOrderStmt(conn, isBuy);
      putOrderStmt.setInt(1, playerId);
      putOrderStmt.setInt(2, rowId);
      putOrderStmt.setInt(3, quantity);
      putOrderStmt.setDouble(4, price);

      putOrderStmt.execute();
      conn.commit();
      return response;
    } catch (SQLException e) {
      if (conn != null && !conn.getAutoCommit())
        conn.rollback();
      e.printStackTrace();
      return new PutOrderResponse(PutOrderResponse.ResponseCode.FAILURE_SQL);
    } catch (ItemNotFoundException e) {
      return new PutOrderResponse(PutOrderResponse.ResponseCode.FAILURE_NOT_FOUND);
    } catch (IOException e)
    {
      e.printStackTrace();
      return new PutOrderResponse(PutOrderResponse.ResponseCode.FAILURE_SQL);
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

      PlayerDatabase playerDb = new PlayerDatabase(this.manager());
      ItemDatabase db = new ItemDatabase(this.manager());

      RegisteredItem registeredItem = db.getItem(itemName, conn);
      int id = registeredItem.id;

      stmt = this._createGetOrdersByLimit(conn, isBuy);
      stmt.setInt(1, id);
      stmt.setInt(2, startLimit);
      stmt.setInt(3, nItems);

      rs = stmt.executeQuery();
      while (rs.next())
      {
        int playerId = rs.getInt(2);
        try
        {
          orders.add(new Order(rs.getInt(1), playerDb.getPlayerUuid(playerId, conn), rs.getInt(4), rs.getDouble(5), registeredItem));
        } catch (IOException ignored) {}
      }

      return new GetOrderResponse(GetOrderResponse.ResponseCode.OK, orders);
    } catch (SQLException e) {
      e.printStackTrace();
      return new GetOrderResponse(GetOrderResponse.ResponseCode.FAILURE_SQL);
    } catch (ItemNotFoundException e)
    {
      e.printStackTrace();
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

  private PreparedStatement _createCountOrdersStmt(Connection connection) throws SQLException
  {
    return connection.prepareStatement("SELECT COUNT(*) FROM ((SELECT * FROM equity_buy_orders WHERE player_uuid = ?) UNION " +
        "(SELECT * FROM equity_sell_orders WHERE player_uuid = ?)) AS t1");
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

  private PreparedStatement _createGetOrdersByPlayer(Connection connection, boolean isBuy) throws SQLException
  {
    if (isBuy)
      return connection.prepareStatement("SELECT * FROM equity_buy_orders WHERE player_uuid = ? LIMIT ?, ?");
    else
      return connection.prepareStatement("SELECT * FROM equity_sell_orders WHERE player_uuid = ? LIMIT ?, ?");
  }

  private PreparedStatement _createInsertOrderStmt(Connection connection, boolean isBuy) throws SQLException
  {
    if (isBuy)
      return connection.prepareStatement("INSERT INTO equity_buy_orders (player_uuid, item_id, quantity, offer_value) VALUES (?, ?, ?, ?)");
    else
      return connection.prepareStatement("INSERT INTO equity_sell_orders (player_uuid, item_id, quantity, offer_value) VALUES (?, ?, ?, ?)");
  }
}
