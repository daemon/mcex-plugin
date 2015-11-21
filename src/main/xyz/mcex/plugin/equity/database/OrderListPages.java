package xyz.mcex.plugin.equity.database;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import xyz.mcex.plugin.DatabaseManager;
import xyz.mcex.plugin.message.BufferedDatabasePages;

import java.sql.SQLException;

public class OrderListPages extends BufferedDatabasePages
{
  private final String  _itemName;
  private final boolean _viewBuy;
  private final EquityDatabase _eqDb;

  public OrderListPages(DatabaseManager manager, String itemName, boolean viewBuy)
  {
    super(manager);
    this._itemName = itemName;
    this._viewBuy = viewBuy;
    this._eqDb = new EquityDatabase(manager);
  }

  @Override
  public String getPage(int index)
  {
    GetOrderResponse response = null;
    try
    {
      response = this._eqDb.getOrders(this._itemName, index * 6, 6, this._viewBuy);
    } catch (SQLException e)
    {
      return null;
    }

    if (response.code != GetOrderResponse.ResponseCode.OK)
      return null;

    StringBuilder builder = new StringBuilder();
    for (Order o : response.orders)
    {
      // TODO: Is Bukkit.getOfflinePlayer threadsafe?
      builder.append(ChatColor.GOLD);
      builder.append("$").append(Math.round(o.price * 100) / 100.0);
      builder.append(" x ").append(o.quantity).append(" : ").append(ChatColor.AQUA)
        .append(Bukkit.getOfflinePlayer(o.playerUuid).getName());
      builder.append("\n");
    }

    return builder.toString();
  }
}
