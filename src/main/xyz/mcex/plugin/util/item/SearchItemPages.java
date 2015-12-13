package xyz.mcex.plugin.util.item;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import xyz.mcex.plugin.DatabaseManager;
import xyz.mcex.plugin.equity.database.GetOrderResponse;
import xyz.mcex.plugin.equity.database.ItemDatabase;
import xyz.mcex.plugin.equity.database.Order;
import xyz.mcex.plugin.equity.database.RegisteredItem;
import xyz.mcex.plugin.internals.Nullable;
import xyz.mcex.plugin.message.BufferedDatabasePages;

import java.sql.SQLException;
import java.util.List;

public class SearchItemPages extends BufferedDatabasePages
{
  private final ItemDatabase _itemDb;
  private final String _itemName;

  public SearchItemPages(DatabaseManager manager, String itemName)
  {
    super(manager);
    this._itemDb = new ItemDatabase(manager);
    this._itemName = itemName;
  }

  @Override
  public @Nullable String getPage(int index)
  {
    List<RegisteredItem> items;
    try
    {
      items = this._itemDb.findByName(this._itemName, index * 6, 6);
    } catch (SQLException e)
    {
      e.printStackTrace();
      return null;
    }

    if (items == null)
      return null;

    StringBuilder builder = new StringBuilder();
    int i = index * 6 + 1;
    for (RegisteredItem item : items)
    {
      builder.append(ChatColor.YELLOW).append(i).append(") ");
      builder.append(ChatColor.WHITE).append(item.alias.toUpperCase()).append("\n");
      ++i;
    }

    return builder.toString();
  }
}
