package xyz.mcex.plugin.equity.chart;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import xyz.mcex.plugin.DatabaseManager;
import xyz.mcex.plugin.equity.database.ItemDatabase;
import xyz.mcex.plugin.equity.database.ItemNotFoundException;
import xyz.mcex.plugin.equity.database.OrderHistoryDatabase;

import java.sql.SQLException;
import java.util.*;

public class JsonDataHandler implements Handler<RoutingContext>
{
  private final ItemDatabase _db;
  private final OrderHistoryDatabase _orderDb;

  public JsonDataHandler(DatabaseManager manager)
  {
    this._db = new ItemDatabase(manager);
    this._orderDb = new OrderHistoryDatabase(manager);
  }

  public String getJson(String item) throws SQLException, ItemNotFoundException
  {
    int itemId = this._db.getItem(item, null).id;
    List<OrderHistoryDatabase.Trade> trades = this._orderDb.getTrades(itemId);
    Collections.reverse(trades);

    StringJoiner rootJoiner = new StringJoiner(",", "[", "]");

    for (OrderHistoryDatabase.Trade trade : trades)
    {
      StringJoiner joiner = new StringJoiner(",", "[", "]");
      String volume = String.valueOf(trade.quantity);
      String price = String.valueOf(trade.value);
      String timestamp = String.valueOf(trade.timeStamp);

      joiner.add(timestamp).add(price).add(volume);
      rootJoiner.add(joiner.toString());
    }

    return rootJoiner.toString();
  }

  @Override
  public void handle(RoutingContext context)
  {
    String item = context.request().params().get("item");
    try
    {
      context.response().putHeader("Content-Type", "text/javascript").end(this.getJson(item));
    } catch (Exception e)
    {
      context.response().close();
    }
  }
}
