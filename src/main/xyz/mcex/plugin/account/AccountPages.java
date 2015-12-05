package xyz.mcex.plugin.account;

import org.bukkit.ChatColor;
import xyz.mcex.plugin.DatabaseManager;
import xyz.mcex.plugin.equity.database.EquityDatabase;
import xyz.mcex.plugin.equity.database.GetOrderResponse;
import xyz.mcex.plugin.equity.database.Order;
import xyz.mcex.plugin.message.BufferedDatabasePages;

import java.sql.SQLException;
import java.util.UUID;

public class AccountPages extends BufferedDatabasePages
{
  private final EquityDatabase _eqDb;
  private final UUID _playerUuid;
  private final boolean _isBuy;

  public AccountPages(DatabaseManager manager, UUID player, boolean isBuy)
  {
    super(manager);
    this._eqDb = new EquityDatabase(manager);
    this._playerUuid = player;
    this._isBuy = isBuy;
  }

  @Override
  public String getPage(int index)
  {
    GetOrderResponse response = null;
    try
    {
      response = this._eqDb.getOrders(this._playerUuid, index * 6, this._isBuy);
    } catch (SQLException e)
    {
      e.printStackTrace();
      return null;
    }

    StringBuilder builder = new StringBuilder();
    int orderNo = 1;
    for (Order order : response.orders)
    {
      builder.append(ChatColor.YELLOW).append(orderNo).append(") ");
      builder.append(ChatColor.WHITE).append(order.quantity).append(" x ").append(order.item.alias);
      builder.append(" at ").append(ChatColor.GOLD).append("$").append(Math.round(order.price * 100) / 100.0)
          .append(ChatColor.WHITE).append(" each\n");
    }

    return builder.toString();
  }
}
