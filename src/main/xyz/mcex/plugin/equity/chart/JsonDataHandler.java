package xyz.mcex.plugin.equity.chart;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import xyz.mcex.plugin.DatabaseManager;
import xyz.mcex.plugin.equity.database.ItemDatabase;
import xyz.mcex.plugin.equity.database.ItemNotFoundException;
import xyz.mcex.plugin.equity.database.OrderHistoryDatabase;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.StringJoiner;

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

    StringJoiner rootJoiner = new StringJoiner(",", "[", "]");

    Iterator<OrderHistoryDatabase.Trade> it = trades.iterator();
    while (it.hasNext())
    {
      StringJoiner joiner = new StringJoiner(",", "[", "]");
      OrderHistoryDatabase.Trade t1 = it.next();
      if (!it.hasNext())
        break;

      OrderHistoryDatabase.Trade t2 = it.next();
      String volume = String.valueOf(t1.quantity + t2.quantity);
      String price = String.valueOf((t1.value + t2.value) / 2);
      String timestamp = String.valueOf(t2.timeStamp + (t1.timeStamp - t2.timeStamp) / 2);

      joiner.add(timestamp).add(price).add(price).add(price).add(price).add(volume);
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
      e.printStackTrace();
      context.response().close();
    }
  }
}
