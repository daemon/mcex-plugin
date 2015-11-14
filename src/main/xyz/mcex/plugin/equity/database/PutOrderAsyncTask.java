package xyz.mcex.plugin.equity.database;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import xyz.mcex.plugin.internals.Nullable;
import xyz.mcex.plugin.message.MessageAlertColor;

import java.sql.SQLException;
import java.util.UUID;

public class PutOrderAsyncTask implements Runnable
{
  private enum Response { OK, FAILURE_NOT_FOUND, FAILURE_SQL };

  private final JavaPlugin _plugin;
  private final boolean _isBuy;
  private final UUID _playerUuid;
  private final Material _material;
  private final int _quantity;
  private final int _price;
  private final EquityDatabase _database;
  private final Player _player;
  private final Economy _economy;

  public PutOrderAsyncTask(JavaPlugin plugin, EquityDatabase database, Player player, Material material, int quantity, int price, boolean isBuy)
  {
    this._plugin = plugin;
    this._player = player;
    this._playerUuid = player.getUniqueId();
    this._material = material;
    this._quantity = quantity;
    this._price = price;
    this._isBuy = isBuy;
    this._database = database;
    this._economy = Bukkit.getServer().getServicesManager().getRegistration(Economy.class).getProvider();
  }

  @Override
  public void run()
  {
    BukkitScheduler s = Bukkit.getScheduler();
    Runnable responseTask;
    try
    {
      PutOrderResponse response;
      if (this._isBuy)
        response = this._database.putBuyOrder(this._playerUuid, this._material.name(), this._quantity, this._price);
      else
        response = this._database.putSellOrder(this._playerUuid, this._material.name(), this._quantity, this._price);
      responseTask = new PutOrderResponseSyncTask(Response.OK, response);
    } catch (SQLException e)
    {
      responseTask = new PutOrderResponseSyncTask(Response.FAILURE_SQL, null);
    } catch (ItemNotFoundException e)
    {
      responseTask = new PutOrderResponseSyncTask(Response.FAILURE_NOT_FOUND, null);
    }

    s.runTask(this._plugin, responseTask);
  }

  private class PutOrderResponseSyncTask implements Runnable
  {
    private final Response _response;
    private final PutOrderResponse _dbResponse;

    public PutOrderResponseSyncTask(Response response, @Nullable PutOrderResponse dbResponse)
    {
      this._response = response;
      this._dbResponse = dbResponse;
    }

    @Override
    public void run()
    {
      Player player = PutOrderAsyncTask.this._player;
      int queuedQuantity  = _quantity - this._dbResponse.totalQuantity;
      switch (this._response)
      {
      case OK:
        if (_isBuy)
        {
          this._dbResponse.exerciseOrders(_economy);
          if (this._dbResponse.totalQuantity > 0)
          {
            player.sendMessage(MessageAlertColor.NOTIY_SUCCESS + "Bought " + this._dbResponse.totalQuantity + " items for $"
              + this._dbResponse.totalMoney);
            // TODO: item package delivery
          }
        } else {
          if (this._dbResponse.totalQuantity > 0)
          {
            player.sendMessage(MessageAlertColor.NOTIY_SUCCESS + "Sold " + this._dbResponse.totalQuantity + " items for $"
                + this._dbResponse.totalMoney);
            _economy.depositPlayer(player, this._dbResponse.totalMoney);
          }
        }

        if (queuedQuantity != 0)
          player.sendMessage(MessageAlertColor.NOTIY_SUCCESS + "Listed " + queuedQuantity  + " items at $" + queuedQuantity * _price);
      break;
      }
    }
  }
}
