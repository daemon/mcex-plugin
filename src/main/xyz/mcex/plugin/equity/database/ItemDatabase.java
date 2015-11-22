package xyz.mcex.plugin.equity.database;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import xyz.mcex.plugin.Database;
import xyz.mcex.plugin.DatabaseManager;
import xyz.mcex.plugin.internals.Nullable;
import xyz.mcex.plugin.util.item.ItemNbtHash;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

public class ItemDatabase extends Database
{
  public ItemDatabase(DatabaseManager manager)
  {
    super(manager);
  }

  public void addItem(String name) throws SQLException, ItemNotFoundException
  {
    Material m = Material.getMaterial(name.toUpperCase());
    if (m == null)
      throw new ItemNotFoundException();

    this.addItem(new ItemStack(m, 1), null);
  }

  public void addItem(ItemStack itemStack, @Nullable String alias) throws SQLException
  {
    Connection conn = null;
    PreparedStatement stmt = null;

    if (alias == null)
      alias = itemStack.getData().getItemType().name();
    else
      alias = alias.toUpperCase();

    ItemNbtHash hash = ItemNbtHash.from(itemStack);
    if (hash == null)
      throw new IllegalArgumentException("SHA-1 algorithm doesn't exist!");

    try
    {
      conn = manager().getConnection();
      stmt = this._createInsertItemStmt(conn);
      stmt.setString(1, alias);
      stmt.setString(2, hash.base64Digest());
      stmt.setInt(3, itemStack.getDurability());
      if (itemStack.hasItemMeta() && itemStack.getItemMeta().hasDisplayName())
        stmt.setString(4, itemStack.getItemMeta().getDisplayName());
      else
        stmt.setString(4, null);
      stmt.setString(5, itemStack.getData().getItemType().name());
      stmt.execute();
    } finally {
      if (stmt != null)
        stmt.close();
      if (conn != null)
        conn.close();
    }
  }

  public RegisteredItem getItem(int rowId, @Nullable Connection connection) throws SQLException, ItemNotFoundException
  {
    boolean externalConnection = connection != null;
    Connection conn = connection;
    ResultSet rs = null;
    ResultSet rs2 = null;

    try
    {
      if (!externalConnection)
        conn = manager().getConnection();
      PreparedStatement stmt = this._createGetItemByIdStmt(conn);
      stmt.setInt(1, rowId);
      rs = stmt.executeQuery();
      if (rs.next())
      {
        ItemNbtHash hash = ItemNbtHash.from(rs.getString(3));
        if (hash == null)
          throw new IllegalArgumentException("SHA-1 doesn't exist!");

        PreparedStatement loreStmt = this._createGetItemLoreStmt(conn);
        loreStmt.setInt(1, rs.getInt(1));
        rs2 = loreStmt.executeQuery();

        List<String> lore = new LinkedList<>();
        while (rs2.next())
          lore.add(rs2.getString(1));

        return new RegisteredItem(rs.getInt(1), hash, rs.getInt(4), lore, rs.getString(5), Material.getMaterial(rs.getString(6)), rs.getString(2));
      }

      throw new ItemNotFoundException();
    } finally {
      if (rs != null)
        rs.close();
      if (rs2 != null)
        rs2.close();
      if (conn != null && !externalConnection)
        conn.close();
    }
  }

  public RegisteredItem getItem(String name, @Nullable Connection connection) throws SQLException, ItemNotFoundException
  {
    name = name.toUpperCase();

    boolean externalConnection = connection != null;
    Connection conn = connection;
    ResultSet rs = null;
    ResultSet rs2 = null;

    try
    {
      if (!externalConnection)
        conn = manager().getConnection();
      PreparedStatement stmt = this._createGetItemStmt(conn);
      stmt.setString(1, name);
      rs = stmt.executeQuery();
      if (rs.next())
      {
        ItemNbtHash hash = ItemNbtHash.from(rs.getString(3));
        if (hash == null)
          throw new ItemNotFoundException();

        PreparedStatement loreStmt = this._createGetItemLoreStmt(conn);
        loreStmt.setInt(1, rs.getInt(1));
        rs2 = loreStmt.executeQuery();

        List<String> lore = new LinkedList<>();
        while (rs2.next())
          lore.add(rs2.getString(1));

        return new RegisteredItem(rs.getInt(1), hash, rs.getInt(4), lore, rs.getString(5), Material.getMaterial(rs.getString(6)), rs.getString(2));
      }

      throw new ItemNotFoundException();
    } finally {
      if (rs != null)
        rs.close();
      if (rs2 != null)
        rs2.close();
      if (conn != null && !externalConnection)
        conn.close();
    }
  }

  private PreparedStatement _createInsertItemStmt(Connection connection) throws SQLException
  {
    return connection.prepareStatement("INSERT INTO items (name, nbt_hash_b64, durability, display_name, mat_name) VALUES (?, ?, ?, ?, ?)");
  }

  private PreparedStatement _createInsertItemLoreStmt(Connection connection) throws SQLException
  {
    return connection.prepareStatement("INSERT INTO item_lore (lore) VALUES (?)");
  }

  private PreparedStatement _createInsertItemLoreAssocStmt(Connection connection) throws SQLException
  {
    return connection.prepareStatement("INSERT INTO item_lore_assoc (item_id, lore_id) VALUES (?, ?)");
  }

  private PreparedStatement _createGetItemLoreStmt(Connection connection) throws SQLException
  {
    return connection.prepareStatement("SELECT lore FROM item_lore INNER JOIN item_lore_assoc ON item_lore_assoc.lore_id=item_lore.id WHERE item_lore_assoc.item_id=? LOCK IN SHARE MODE");
  }

  private PreparedStatement _createGetItemStmt(Connection connection) throws SQLException
  {
    return connection.prepareStatement("SELECT * FROM items WHERE name = ? LOCK IN SHARE MODE");
  }

  private PreparedStatement _createGetItemByIdStmt(Connection connection) throws SQLException
  {
    return connection.prepareStatement("SELECT * FROM items WHERE id = ? LOCK IN SHARE MODE");
  }
}
