package xyz.mcex.plugin.equity.database;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import xyz.mcex.plugin.DatabaseManager;
import xyz.mcex.plugin.message.BufferedDatabasePages;

import java.sql.SQLException;

public class RecentOrderListPages extends BufferedDatabasePages
{
  private final boolean _viewBuy;
  private final EquityDatabase _eqDb;

  public RecentOrderListPages(DatabaseManager manager, boolean viewBuy)
  {
    super(manager);
    this._viewBuy = viewBuy;
    this._eqDb = new EquityDatabase(manager);
  }

  @Override
  public String getPage(int index)
  {
    GetOrderResponse response = null;
    try
    {
      response = this._eqDb.getRecentOrders(index * 6, 6, this._viewBuy);
    } catch (SQLException e)
    {
      e.printStackTrace();
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
      builder.append(" x ").append(o.quantity).append(" ").append(ChatColor.YELLOW).append(o.item.alias).append(" : ").append(ChatColor.AQUA)
          .append(Bukkit.getOfflinePlayer(o.playerUuid).getName());
      builder.append("\n");
    }

    return builder.toString();
  }
}
