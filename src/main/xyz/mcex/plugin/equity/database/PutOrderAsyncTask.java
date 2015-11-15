package xyz.mcex.plugin.equity.database;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import xyz.mcex.plugin.internals.Nullable;
import xyz.mcex.plugin.message.MessageAlertColor;
import xyz.mcex.plugin.util.item.DeliverItemPackageAsyncTask;
import xyz.mcex.plugin.util.item.ItemPackage;
import xyz.mcex.plugin.util.item.ItemPackageDatabase;
import xyz.mcex.plugin.util.item.NotifyItemPackageTask;

import java.sql.SQLException;
import java.util.Observable;
import java.util.UUID;

public class PutOrderAsyncTask extends Observable implements Runnable
{
  public enum Response { OK, FAILURE_NOT_FOUND, FAILURE_SQL };

  private final JavaPlugin _plugin;
  private final boolean _isBuy;
  private final UUID _playerUuid;
  private final Material _material;
  private final int _quantity;
  private final double _price;
  private final EquityDatabase _database;
  private final Player _player;
  private final Economy _economy;

  public PutOrderAsyncTask(JavaPlugin plugin, EquityDatabase database, Player player, Material material, int quantity, double price, boolean isBuy)
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
    PutOrderResponse response = null;
    Response responseEnum = Response.OK;

    try
    {
      if (this._isBuy)
        response = this._database.putBuyOrder(this._playerUuid, this._material.name(), this._quantity, this._price);
      else
        response = this._database.putSellOrder(this._playerUuid, this._material.name(), this._quantity, this._price);
    } catch (SQLException e)
    {
      responseEnum = Response.FAILURE_SQL;
    } catch (ItemNotFoundException e)
    {
      responseEnum = Response.FAILURE_NOT_FOUND;
    }

    responseTask = new PutOrderResponseSyncTask(responseEnum, response);
    s.runTask(this._plugin, responseTask);
    final Response finalResponse = responseEnum;

    s.callSyncMethod(this._plugin, () -> {
      this.notifyObservers(finalResponse);
      return null;
    });
  }

  private class PutOrderResponseSyncTask implements Runnable
  {
    private final Response _response;
    private final PutOrderResponse _dbResponse;
    private final ItemPackageDatabase _itemDb;

    public PutOrderResponseSyncTask(Response response, @Nullable PutOrderResponse dbResponse)
    {
      this._response = response;
      this._dbResponse = dbResponse;
      this._itemDb = new ItemPackageDatabase(_database.manager(), _database);
    }

    @Override
    public void run()
    {
      Player player = PutOrderAsyncTask.this._player;
      int queuedQuantity  = _quantity - this._dbResponse.totalQuantity;
      switch (this._response)
      {
        case OK:
          if (queuedQuantity != 0)
            player.sendMessage(MessageAlertColor.NOTIY_SUCCESS + "Listed " + queuedQuantity  + " items at $" + queuedQuantity * _price);
          if (_isBuy)
          {
            this._dbResponse.exerciseOrders(_economy);
            if (this._dbResponse.totalQuantity > 0)
            {
              player.sendMessage(MessageAlertColor.NOTIY_SUCCESS + "Bought " + this._dbResponse.totalQuantity + " items for $"
                + this._dbResponse.totalMoney);
              ItemPackage pkg = new ItemPackage(_playerUuid, _material, this._dbResponse.totalQuantity);
              Bukkit.getScheduler().runTaskAsynchronously(_plugin, new DeliverItemPackageAsyncTask(this._itemDb, pkg));
              (new NotifyItemPackageTask(player)).run();
            }
          } else
          {
            if (this._dbResponse.totalQuantity > 0)
            {
              player.sendMessage(MessageAlertColor.NOTIY_SUCCESS + "Sold " + this._dbResponse.totalQuantity + " items for $"
                  + this._dbResponse.totalMoney);
              _economy.depositPlayer(player, this._dbResponse.totalMoney);
            }
          }
          break;
        case FAILURE_SQL:
          player.sendMessage(MessageAlertColor.ERROR + "SQL error: please report to staff.");
          break;
        case FAILURE_NOT_FOUND:
          player.sendMessage(MessageAlertColor.ERROR + "Item not registered for trade.");
          break;
      }
    }
  }
}
