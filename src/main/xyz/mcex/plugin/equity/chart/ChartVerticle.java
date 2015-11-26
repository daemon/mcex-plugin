package xyz.mcex.plugin.equity.chart;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import org.bukkit.configuration.file.FileConfiguration;
import xyz.mcex.plugin.DatabaseManager;

public class ChartVerticle extends AbstractVerticle
{
  private final FileConfiguration _config;
  private final DatabaseManager _manager;

  public ChartVerticle(FileConfiguration config, DatabaseManager manager)
  {
    this._config = config;
    this._manager = manager;
  }

  @Override
  public void start()
  {
    if (!_config.getBoolean("chart-enabled", false))
      return;

    int port = _config.getInt("chart-port", 2015);

    Vertx vertx = this.getVertx();
    HttpServer server = vertx.createHttpServer();

    Router router = Router.router(vertx);
    JsonDataHandler jsonHandler = new JsonDataHandler(this._manager);
    router.route("/charts/:item/").handler(new ChartHandler(_config.getString("chart-url") + ":" + port, jsonHandler));
    router.route("/charts/:item/json").handler(jsonHandler);

    server.requestHandler(router::accept).listen(port);
  }
}
