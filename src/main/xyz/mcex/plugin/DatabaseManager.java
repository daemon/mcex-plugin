package xyz.mcex.plugin;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;

import java.beans.PropertyVetoException;
import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseManager
{
  private final ComboPooledDataSource _source;

  public DatabaseManager(FileConfiguration config) throws PropertyVetoException
  {
    this._source = new ComboPooledDataSource();
    this._source.setMaxStatements(128);
    this._source.setMaxStatementsPerConnection(16);
    this._source.setMinPoolSize(2);
    this._source.setMaxPoolSize(8);

    int port = config.getInt("sql-port");
    String user = config.getString("sql-user");
    String password = config.getString("sql-password");
    String database = config.getString("sql-database");
    String hostname = config.getString("sql-hostname");
    this._source.setJdbcUrl("jdbc:mysql://" + hostname + ":" + port + "/" + database);
    this._source.setUser(user);
    this._source.setPassword(password);
    this._source.setAutoCommitOnClose(true);

    this._source.setDriverClass("com.mysql.jdbc.Driver");
  }

  public Connection getConnection() throws SQLException
  {
    return this._source.getConnection();
  }

  public void createDefaultTables() throws SQLException
  {
    Connection c = null;
    try
    {
      c = this._source.getConnection();
      // TODO reduce player UUID redundancy
      c.createStatement().execute("CREATE TABLE IF NOT EXISTS items (id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY NOT NULL, name VARCHAR(32) NOT NULL) ENGINE=InnoDB");
      c.createStatement().execute("CREATE TABLE IF NOT EXISTS equity_buy_orders (id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY NOT NULL, player_uuid BINARY(16) NOT NULL, " +
          "item_id INT UNSIGNED NOT NULL, quantity INT UNSIGNED NOT NULL, offer_value REAL NOT NULL, ts TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
          "CONSTRAINT FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE, INDEX offer_value_i(offer_value)) ENGINE=InnoDB");
      c.createStatement().execute("CREATE TABLE IF NOT EXISTS equity_sell_orders (id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY NOT NULL, player_uuid BINARY(16) NOT NULL, " +
          "item_id INT UNSIGNED NOT NULL, quantity INT UNSIGNED NOT NULL, offer_value REAL NOT NULL, ts TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
          "CONSTRAINT FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE, INDEX offer_value_i(offer_value)) ENGINE=InnoDB");
      c.createStatement().execute("CREATE TABLE IF NOT EXISTS equity_sell_history (id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY NOT NULL, player_uuid_seller BINARY(16) NOT NULL, " +
          "player_uuid_buyer BINARY(16) NOT NULL, item_id INT UNSIGNED NOT NULL, quantity INT UNSIGNED NOT NULL, offer_value REAL NOT NULL, ts TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
          "CONSTRAINT FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE) ENGINE=InnoDB");
      c.createStatement().execute("CREATE TABLE IF NOT EXISTS item_package_queue (id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY NOT NULL, player_uuid BINARY(16) NOT NULL, " +
          "item_id INT UNSIGNED NOT NULL, quantity INT UNSIGNED NOT NULL, CONSTRAINT FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE, INDEX player_uuid_i(player_uuid)) ENGINE=InnoDB");
    } finally {
      if (c != null)
        c.close();
    }
  }
}
