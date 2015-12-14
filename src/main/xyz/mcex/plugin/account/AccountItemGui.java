package xyz.mcex.plugin.account;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import xyz.mcex.plugin.DatabaseManager;
import xyz.mcex.plugin.McexPlugin;
import xyz.mcex.plugin.equity.database.EquityDatabase;
import xyz.mcex.plugin.equity.database.Order;
import xyz.mcex.plugin.equity.database.RegisteredItem;
import xyz.mcex.plugin.gui.*;
import xyz.mcex.plugin.message.MessageAlertColor;
import xyz.mcex.plugin.message.Messages;
import xyz.mcex.plugin.util.item.DeliverItemPackageAsyncTask;
import xyz.mcex.plugin.util.item.ItemPackage;
import xyz.mcex.plugin.util.item.ItemPackageDatabase;
import xyz.mcex.plugin.util.item.NotifyItemPackageTask;

import java.sql.SQLException;

public class AccountItemGui extends NormalSequentialPanel
{
  private final DatabaseManager _manager;
  private final RegisteredItem _item;
  private final ItemPackageDatabase _pkgDb;
  private final Order _order;
  private final boolean _isBuy;
  private final EquityDatabase _eqDb;
  private final Economy _economy;

  public AccountItemGui(Player player, String title, DatabaseManager manager, Order order, boolean isBuy, Economy economy)
  {
    super(player, title);
    this._manager = manager;
    this._order = order;
    this._item = order.item;
    this._isBuy = isBuy;
    this._eqDb = new EquityDatabase(manager);
    this._pkgDb = new ItemPackageDatabase(manager, this._eqDb);
    this._economy = economy;
  }

  @Override
  public Inventory makeInventory() throws Exception
  {
    Inventory inv = super.makeInventory();
    inv.setItem(13, this._item.createInfoItem(this._order.quantity, this._order.price));
    this.setSlotListener(new CancellingSlotListener(), 13);
    inv.setItem(22, Buttons.makeButton(Buttons.DENY, "Cancel order"));
    this.setSlotListener((panel, action, event) -> {
      boolean success;
      try
      {
        success = this._eqDb.deleteOrder(this.player().getUniqueId(), this._order.rowId, this._isBuy);
      } catch (SQLException e)
      {
        Bukkit.getScheduler().runTask(McexPlugin.instance, () -> {
          this.player().sendMessage(MessageAlertColor.ERROR + Messages.DATABASE_ERROR);
        });
        this.hide();
        return;
      }

      Bukkit.getScheduler().runTask(McexPlugin.instance, () -> {
        if (success)
        {
          if (this._isBuy)
            this._economy.depositPlayer(player(), this._order.price * this._order.quantity);
          else
          {
            ItemPackage pkg = new ItemPackage(player().getUniqueId(), this._order.item, this._order.quantity);
            Bukkit.getScheduler().runTaskAsynchronously(McexPlugin.instance, new DeliverItemPackageAsyncTask(this._pkgDb, pkg));
            (new NotifyItemPackageTask(player())).run();
          }

          this.player().sendMessage(MessageAlertColor.NOTIFY_SUCCESS + "Order was cancelled successfully!");
        }
        else
          this.player().sendMessage(MessageAlertColor.NOTIFY_AGNOSTIC + "That order doesn't exist.");
      });

      this.hide();
    }, 22);

    return inv;
  }
}
