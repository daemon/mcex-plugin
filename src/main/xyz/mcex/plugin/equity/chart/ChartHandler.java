package xyz.mcex.plugin.equity.chart;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import java.io.*;

public class ChartHandler implements Handler<RoutingContext>
{
  private final String _host;
  private final JsonDataHandler _jsonHandler;

  public ChartHandler(String host, JsonDataHandler jsonHandler)
  {
    this._host = host;
    this._jsonHandler = jsonHandler;
  }

  @Override
  public void handle(RoutingContext context)
  {
    // Optimize html building, stop being a lazy asshole
    String item = context.request().params().get("item");

    try
    {
      BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/chart.html")));
      context.response().setChunked(true);
      String line;
      while ((line = reader.readLine()) != null)
        context.response().write(line.replace("{item}", item).replace("{host}", this._host) + "\n");
      context.response().end();
    } catch (Exception e)
    {
      e.printStackTrace();
      context.response().close();
    }
  }
}
