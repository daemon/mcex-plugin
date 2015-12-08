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
import java.util.*;

public class ItemDatabase extends Database
{
  public ItemDatabase(DatabaseManager manager)
  {
    super(manager);
  }

  public void addItem(String name) throws SQLException, ItemNotFoundException, DuplicateItemException
  {
    Material m = Material.getMaterial(name.toUpperCase());
    if (m == null)
      throw new ItemNotFoundException();

    this.addItem(new ItemStack(m, 1), null);
  }

  public void addItem(ItemStack itemStack, @Nullable String alias) throws SQLException, DuplicateItemException
  {
    Connection conn = null;
    PreparedStatement stmt = null;
    PreparedStatement loreInsertStmt = null;
    PreparedStatement loreGetIdStmt = null;
    PreparedStatement loreInsAssocStmt = null;
    PreparedStatement enchantInsertStmt = null;
    PreparedStatement enchantGetStmt = null;
    PreparedStatement enchantInsertAssocStmt = null;
    ResultSet rs = null;
    ResultSet enchantRs = null;

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
      conn.setAutoCommit(false);

      try
      {
        this.getItem(alias, conn);
      } catch (SQLException e) {
        e.printStackTrace();
      } catch (ItemNotFoundException e)
      {
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

        RegisteredItem item = null;
        try
        {
          item = this.getItem(alias, conn);
        } catch (SQLException e2) {
          e2.printStackTrace();
        } catch (ItemNotFoundException e1)
        {
          throw new SQLException("Error inserting item into database");
        }

        // Enchantments
        List<String> enchantments = new LinkedList<>();
        List<Integer> levels = new LinkedList<>();
        itemStack.getEnchantments().forEach((enchant, lvl) -> {
          enchantments.add(enchant.getName());
          levels.add(lvl);
        });

        enchantInsertStmt = this._createInsertItemEnchantStmt(conn);
        enchantGetStmt = this._createGetItemEnchantIdStmt(conn);
        enchantInsertAssocStmt = this._createInsertItemEnchantAssocStmt(conn);
        Iterator<Integer> levelIt = levels.iterator();
        for (String enchant : enchantments)
        {
          enchantInsertStmt.setString(1, enchant);
          enchantInsertStmt.execute();

          enchantGetStmt.setString(1, enchant);
          enchantRs = enchantGetStmt.executeQuery();
          if (!enchantRs.next())
            throw new SQLException("Inserting item enchantments failed");

          int enchantid = enchantRs.getInt(1);
          enchantRs.close();

          enchantInsertAssocStmt.setInt(1, item.id);
          enchantInsertAssocStmt.setInt(2, enchantid);
          enchantInsertAssocStmt.setInt(3, levelIt.next());
          enchantInsertAssocStmt.execute();
        }

        if (!itemStack.hasItemMeta() || !itemStack.getItemMeta().hasLore())
        {
          conn.commit();
          return;
        }

        // Lore
        List<String> lores = itemStack.getItemMeta().getLore();
        List<Integer> loreIds = new LinkedList<>();
        loreInsertStmt = this._createInsertItemLoreStmt(conn);
        loreGetIdStmt = this._createGetItemLoreIdStmt(conn);
        for (String lore : lores)
        {
          loreInsertStmt.setString(1, lore);
          loreInsertStmt.execute();

          loreGetIdStmt.setString(1, lore);
          rs = loreGetIdStmt.executeQuery();
          if (!rs.next())
            throw new SQLException("Couldn't insert item lore");

          loreIds.add(rs.getInt(1));
          rs.close();
        }

        loreInsAssocStmt = this._createInsertItemLoreAssocStmt(conn);
        for (Integer loreId : loreIds)
        {
          loreInsAssocStmt.setInt(1, item.id);
          loreInsAssocStmt.setInt(2, loreId);
          loreInsAssocStmt.execute();
        }

        conn.commit();
        return;
      }

      throw new DuplicateItemException();
    } catch (SQLException e) {
      e.printStackTrace();
      if (conn != null)
        conn.rollback();
      throw e;
    } finally {
      if (enchantRs != null)
        enchantRs.close();
      if (enchantGetStmt != null)
        enchantGetStmt.close();
      if (enchantInsertAssocStmt != null)
        enchantInsertAssocStmt.close();
      if (enchantInsertStmt != null)
        enchantInsertStmt.close();
      if (rs != null)
        rs.close();
      if (loreGetIdStmt != null)
        loreGetIdStmt.close();
      if (loreInsAssocStmt != null)
        loreInsAssocStmt.close();
      if (loreInsertStmt != null)
        loreInsertStmt.close();
      if (stmt != null)
        stmt.close();
      if (conn != null)
      {
        conn.setAutoCommit(true);
        conn.close();
      }
    }
  }

  public RegisteredItem getItem(int rowId, @Nullable Connection connection) throws SQLException, ItemNotFoundException
  {
    boolean externalConnection = connection != null;
    Connection conn = connection;
    ResultSet rs = null;
    ResultSet rs2 = null;
    ResultSet rs3 = null;
    PreparedStatement stmt = null;
    PreparedStatement loreStmt = null;
    PreparedStatement enchantStmt = null;

    try
    {
      if (!externalConnection)
        conn = manager().getConnection();
      stmt = this._createGetItemByIdStmt(conn);
      stmt.setInt(1, rowId);
      rs = stmt.executeQuery();
      if (rs.next())
      {
        ItemNbtHash hash = ItemNbtHash.from(rs.getString(3));
        if (hash == null)
          throw new IllegalArgumentException("SHA-1 doesn't exist!");

        int itemId = rs.getInt(1);
        loreStmt = this._createGetItemLoreStmt(conn);
        loreStmt.setInt(1, itemId);
        rs2 = loreStmt.executeQuery();

        List<String> lore = new LinkedList<>();
        while (rs2.next())
          lore.add(rs2.getString(1));

        enchantStmt = this._createGetItemEnchantStmt(conn);
        enchantStmt.setInt(1, itemId);
        rs3 = enchantStmt.executeQuery();
        Map<String, Integer> enchantNameToLvl = new HashMap<>();
        while (rs3.next())
          enchantNameToLvl.put(rs3.getString(1), rs3.getInt(2));

        return new RegisteredItem(itemId, hash, rs.getInt(4), lore, rs.getString(5), Material.getMaterial(rs.getString(6)), rs.getString(2), enchantNameToLvl);
      }

      throw new ItemNotFoundException();
    } finally {
      if (rs3 != null)
        rs3.close();
      if (stmt != null)
        stmt.close();
      if (loreStmt != null)
        loreStmt.close();
      if (enchantStmt != null)
        enchantStmt.close();
      if (rs != null)
        rs.close();
      if (rs2 != null)
        rs2.close();
      if (conn != null && !externalConnection)
        conn.close();
    }
  }

  // TODO refactor
  public RegisteredItem getItem(String name, @Nullable Connection connection) throws SQLException, ItemNotFoundException
  {
    name = name.toUpperCase();

    boolean externalConnection = connection != null;
    Connection conn = connection;
    ResultSet rs = null;
    ResultSet rs2 = null;
    ResultSet rs3 = null;
    PreparedStatement stmt = null;
    PreparedStatement loreStmt = null;
    PreparedStatement enchantStmt = null;

    try
    {
      if (!externalConnection)
        conn = manager().getConnection();
      stmt = this._createGetItemStmt(conn);
      stmt.setString(1, name);
      rs = stmt.executeQuery();
      if (rs.next())
      {
        ItemNbtHash hash = ItemNbtHash.from(rs.getString(3));
        if (hash == null)
          throw new ItemNotFoundException();

        loreStmt = this._createGetItemLoreStmt(conn);
        loreStmt.setInt(1, rs.getInt(1));
        rs2 = loreStmt.executeQuery();

        List<String> lore = new LinkedList<>();
        while (rs2.next())
          lore.add(rs2.getString(1));

        enchantStmt = this._createGetItemEnchantStmt(conn);
        enchantStmt.setInt(1, rs.getInt(1));
        rs3 = enchantStmt.executeQuery();
        Map<String, Integer> enchantNameToLvl = new HashMap<>();
        while (rs3.next())
          enchantNameToLvl.put(rs3.getString(1), rs3.getInt(2));

        return new RegisteredItem(rs.getInt(1), hash, rs.getInt(4), lore, rs.getString(5), Material.getMaterial(rs.getString(6)), rs.getString(2), enchantNameToLvl);
      }

      throw new ItemNotFoundException();
    } finally {
      if (rs3 != null)
        rs3.close();
      if (stmt != null)
        stmt.close();
      if (loreStmt != null)
        loreStmt.close();
      if (enchantStmt != null)
        enchantStmt.close();
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

  private PreparedStatement _createInsertItemEnchantStmt(Connection connection) throws SQLException
  {
    return connection.prepareStatement("INSERT INTO item_enchant (name) VALUES (?) ON DUPLICATE KEY UPDATE id=id");
  }

  private PreparedStatement _createInsertItemEnchantAssocStmt(Connection connection) throws SQLException
  {
    return connection.prepareStatement("INSERT INTO item_enchant_assoc (item_id, enchant_id, level) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE id=id");
  }

  private PreparedStatement _createGetItemEnchantIdStmt(Connection connection) throws SQLException
  {
    return connection.prepareStatement("SELECT id FROM item_enchant WHERE name=? LOCK IN SHARE MODE");
  }

  private PreparedStatement _createGetItemEnchantStmt(Connection connection) throws SQLException
  {
    return connection.prepareStatement("SELECT name, level FROM item_enchant INNER JOIN item_enchant_assoc ON item_enchant_assoc.enchant_id=item_enchant.id WHERE item_enchant_assoc.item_id=? LOCK IN SHARE MODE");
  }

  private PreparedStatement _createInsertItemLoreStmt(Connection connection) throws SQLException
  {
    return connection.prepareStatement("INSERT INTO item_lore (lore) VALUES (?) ON DUPLICATE KEY UPDATE id=id");
  }

  private PreparedStatement _createInsertItemLoreAssocStmt(Connection connection) throws SQLException
  {
    return connection.prepareStatement("INSERT INTO item_lore_assoc (item_id, lore_id) VALUES (?, ?) ON DUPLICATE KEY UPDATE id=id");
  }

  private PreparedStatement _createGetItemLoreStmt(Connection connection) throws SQLException
  {
    return connection.prepareStatement("SELECT lore FROM item_lore INNER JOIN item_lore_assoc ON item_lore_assoc.lore_id=item_lore.id WHERE item_lore_assoc.item_id=? LOCK IN SHARE MODE");
  }

  private PreparedStatement _createGetItemLoreIdStmt(Connection connection) throws SQLException
  {
    return connection.prepareStatement("SELECT id FROM item_lore WHERE lore=? LOCK IN SHARE MODE");
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
