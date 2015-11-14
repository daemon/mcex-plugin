package xyz.mcex.plugin.util.item;

import xyz.mcex.plugin.Database;
import xyz.mcex.plugin.DatabaseManager;
import xyz.mcex.plugin.equity.database.EquityDatabase;
import xyz.mcex.plugin.equity.database.ItemNotFoundException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class ItemPackageDatabase extends Database
{
  private final EquityDatabase _equityDb;

  public ItemPackageDatabase(DatabaseManager manager, EquityDatabase equityDatabase)
  {
    super(manager);
    this._equityDb = equityDatabase;
  }

  public void queuePackage(ItemPackage itemPackage) throws SQLException, ItemNotFoundException
  {
    Connection connection = null;
    PreparedStatement queueStmt = null;
    try
    {
      connection = this.manager().getConnection();
      connection.setAutoCommit(false);
      queueStmt = connection.prepareStatement("INSERT INTO item_package_queue (player_uuid, item_id, quantity) VALUES (?, ?, ?)");

      int itemId = this._equityDb.getItemId(itemPackage._material.name(), connection);
      // TODO refactor
      ByteArrayOutputStream ba = new ByteArrayOutputStream(16);
      DataOutputStream os = new DataOutputStream(ba);
      try
      {
        os.writeLong(itemPackage._receiver.getMostSignificantBits());
        os.writeLong(itemPackage._receiver.getLeastSignificantBits());
      } catch (IOException e) {
        return;
      }

      queueStmt.setBinaryStream(1, new ByteArrayInputStream(ba.toByteArray()), 16);
      queueStmt.setInt(2, itemId);
      queueStmt.setInt(3, itemPackage._quantity);
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
}
