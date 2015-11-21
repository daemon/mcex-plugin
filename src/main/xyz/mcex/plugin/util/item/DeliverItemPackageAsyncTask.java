package xyz.mcex.plugin.util.item;

import xyz.mcex.plugin.equity.database.ItemNotFoundException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Observable;

public class DeliverItemPackageAsyncTask extends Observable implements Runnable
{
  public enum Response { OK, SQL_EXCEPTION, ITEM_NOT_FOUND, UUID_EXCEPTION }
  private final ItemPackageDatabase _db;
  private final ItemPackage _itemPackage;

  public DeliverItemPackageAsyncTask(ItemPackageDatabase database, ItemPackage itemPackage)
  {
    this._db = database;
    this._itemPackage = itemPackage;
  }

  public void run()
  {
    try
    {
      this._db.queuePackage(this._itemPackage);
      this.notifyObservers(Response.OK);
    } catch (SQLException e)
    {
      e.printStackTrace();
      this.notifyObservers(Response.SQL_EXCEPTION);
    } catch (ItemNotFoundException e)
    {
      e.printStackTrace();
      this.notifyObservers(Response.ITEM_NOT_FOUND);
    } catch (IOException e)
    {
      e.printStackTrace();
      this.notifyObservers(Response.UUID_EXCEPTION);
    }
  }
}
