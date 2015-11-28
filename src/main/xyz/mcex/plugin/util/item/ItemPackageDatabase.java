package xyz.mcex.plugin.util.item;

import xyz.mcex.plugin.Database;
import xyz.mcex.plugin.DatabaseManager;
import xyz.mcex.plugin.equity.database.EquityDatabase;
import xyz.mcex.plugin.equity.database.ItemDatabase;
import xyz.mcex.plugin.equity.database.ItemNotFoundException;
import xyz.mcex.plugin.equity.database.RegisteredItem;
import xyz.mcex.plugin.util.player.PlayerDatabase;
import xyz.mcex.plugin.util.player.PlayerUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public class ItemPackageDatabase extends Database
{
  private final EquityDatabase _equityDb;

  public ItemPackageDatabase(DatabaseManager manager, EquityDatabase equityDatabase)
  {
    super(manager);
    this._equityDb = equityDatabase;
  }

  public void queuePackage(ItemPackage itemPackage) throws SQLException, ItemNotFoundException, IOException
  {
    Connection connection = null;
    try
    {
      connection = this.manager().getConnection();
      PreparedStatement queueStmt = connection.prepareStatement("INSERT INTO item_package_queue (player_uuid, item_id, quantity) VALUES (?, ?, ?) " +
          "ON DUPLICATE KEY UPDATE quantity=quantity + ?");

      PlayerDatabase pDb = new PlayerDatabase(this.manager());
      queueStmt.setInt(1, pDb.fetchPlayerId(itemPackage.receiver, connection));
      queueStmt.setInt(2, itemPackage.item.id);
      queueStmt.setInt(3, itemPackage.quantity);
      queueStmt.setInt(4, itemPackage.quantity);
      queueStmt.execute();
    } finally {
      if (connection != null)
        connection.close();
    }
  }

  // TODO refactor connection setup and destroy
  public List<ItemPackage> getPackages(UUID playerUuid) throws SQLException, IOException
  {
    List<ItemPackage> packages = new LinkedList<>();
    Connection connection = null;
    PreparedStatement getStmt = null;
    ResultSet rs = null;

    try
    {
      connection = this.manager().getConnection();
      ItemDatabase itemDb = new ItemDatabase(this.manager());

      getStmt = connection.prepareStatement("SELECT item_id, quantity, id FROM item_package_queue WHERE player_uuid = ?");
      PlayerDatabase pDb = new PlayerDatabase(this.manager());
      getStmt.setInt(1, pDb.fetchPlayerId(playerUuid, connection));

      rs = getStmt.executeQuery();
      while (rs.next())
      {
        RegisteredItem item;
        try
        {
          item = itemDb.getItem(rs.getInt(1), connection);
        } catch (ItemNotFoundException e)
        {
          e.printStackTrace();
          continue;
        }

        packages.add(new ItemPackage(playerUuid, item, rs.getInt(2), rs.getInt(3)));
      }

      return packages;
    } finally {
      if (rs != null)
        rs.close();
      if (connection != null)
        connection.close();
    }
  }

  public ItemPackage fetchPackage(UUID playerUuid, int packageNo) throws SQLException, IOException, ItemNotFoundException
  {
    List<ItemPackage> packages = new LinkedList<>();
    Connection connection = null;
    PreparedStatement getStmt = null;
    ResultSet rs = null;

    try
    {
      connection = this.manager().getConnection();
      connection.setAutoCommit(false);
      getStmt = connection.prepareStatement("SELECT item_id, quantity, id FROM item_package_queue WHERE player_uuid = ? LIMIT ?, 1 FOR UPDATE");
      PlayerDatabase pDb = new PlayerDatabase(this.manager());

      getStmt.setInt(1, pDb.fetchPlayerId(playerUuid, connection));
      getStmt.setInt(2, packageNo - 1);

      rs = getStmt.executeQuery();
      if (rs.next())
      {
        ItemDatabase itemDb = new ItemDatabase(this.manager());
        RegisteredItem item = itemDb.getItem(rs.getInt(1), connection);

        ItemPackage pkg = new ItemPackage(playerUuid, item, rs.getInt(2), rs.getInt(3));

        connection.createStatement().execute("DELETE FROM item_package_queue WHERE id = " + pkg.id);
        connection.commit();
        return pkg;
      }

      throw new ItemNotFoundException();
    } catch (SQLException e) {
      if (connection != null && !connection.getAutoCommit())
        connection.rollback();
      throw e;
    } finally {
      if (getStmt != null)
        getStmt.close();
      if (rs != null)
        rs.close();
      if (connection != null)
      {
        connection.setAutoCommit(true);
        connection.close();
      }
    }
  }

  public boolean deletePackage(ItemPackage itemPackage) throws SQLException
  {
    Connection connection = null;
    PreparedStatement delStmt = null;

    try
    {
      connection = this.manager().getConnection();
      delStmt = connection.prepareStatement("DELETE FROM item_package_queue WHERE id = ?");
      delStmt.setInt(1, itemPackage.id);
      return delStmt.execute();
    } finally {
      if (delStmt != null)
        delStmt.close();
      if (connection != null)
        connection.close();
    }
  }
}
