package xyz.mcex.plugin.util.item;

import org.bukkit.Material;
import xyz.mcex.plugin.Database;
import xyz.mcex.plugin.DatabaseManager;
import xyz.mcex.plugin.equity.database.EquityDatabase;
import xyz.mcex.plugin.equity.database.ItemNotFoundException;
import xyz.mcex.plugin.util.PlayerUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
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
    PreparedStatement queueStmt = null;
    try
    {
      connection = this.manager().getConnection();
      connection.setAutoCommit(false);
      queueStmt = connection.prepareStatement("INSERT INTO item_package_queue (player_uuid, item_id, quantity) VALUES (?, ?, ?)");

      int itemId = this._equityDb.getItemId(itemPackage.material.name(), connection);

      ByteArrayInputStream stream = PlayerUtils.uuidToStream(itemPackage.receiver);
      queueStmt.setBinaryStream(1, stream, 16);
      queueStmt.setInt(2, itemId);
      queueStmt.setInt(3, itemPackage.quantity);
      queueStmt.execute();
      connection.commit();
    } catch (SQLException e) {
      if (connection != null && !connection.getAutoCommit())
        connection.rollback();
      throw e;
    } finally {
      if (connection != null)
      {
        connection.setAutoCommit(true);
        connection.close();
      }
      if (queueStmt != null)
        queueStmt.close();
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
      getStmt = connection.prepareStatement("SELECT items.name, quantity, item_package_queue.id FROM item_package_queue INNER JOIN items ON items.id=" +
          "item_package_queue.item_id WHERE player_uuid = ?");
      getStmt.setBinaryStream(1, PlayerUtils.uuidToStream(playerUuid));

      rs = getStmt.executeQuery();
      while (rs.next())
      {
        Material m;
        if ((m = Material.getMaterial(rs.getString(1))) == null)
          continue;
        packages.add(new ItemPackage(playerUuid, m, rs.getInt(2), rs.getInt(3)));
      }

      return packages;
    } finally {
      if (getStmt != null)
        getStmt.close();
      if (rs != null)
        rs.close();
      if (connection != null)
        connection.close();
    }
  }

  public ItemPackage fetchPackage(UUID playerUuid, int id) throws SQLException, IOException, NoSuchElementException
  {
    List<ItemPackage> packages = new LinkedList<>();
    Connection connection = null;
    PreparedStatement getStmt = null;
    ResultSet rs = null;

    try
    {
      connection = this.manager().getConnection();
      connection.setAutoCommit(false);
      getStmt = connection.prepareStatement("SELECT items.name, quantity, item_package_queue.id FROM item_package_queue INNER JOIN items ON items.id=" +
          "item_package_queue.item_id WHERE player_uuid = ? AND item_package_queue.id = ? FOR UPDATE");
      getStmt.setBinaryStream(1, PlayerUtils.uuidToStream(playerUuid), 16);
      getStmt.setInt(2, id);

      rs = getStmt.executeQuery();
      if (rs.next())
      {
        Material m;
        if ((m = Material.getMaterial(rs.getString(1))) == null)
          throw new NoSuchElementException("No such material in items registered for trade");

        ItemPackage pkg = new ItemPackage(playerUuid, m, rs.getInt(2), rs.getInt(3));

        connection.createStatement().execute("DELETE FROM item_package_queue WHERE id = " + pkg.id);
        connection.commit();
        return pkg;
      }

      throw new NoSuchElementException("Item package not found");
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
