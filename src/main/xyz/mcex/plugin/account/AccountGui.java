package xyz.mcex.plugin.account;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import xyz.mcex.plugin.DatabaseManager;
import xyz.mcex.plugin.McexPlugin;
import xyz.mcex.plugin.equity.database.EquityDatabase;
import xyz.mcex.plugin.equity.database.GetOrderResponse;
import xyz.mcex.plugin.equity.database.Order;
import xyz.mcex.plugin.gui.*;
import xyz.mcex.plugin.internals.Nullable;
import xyz.mcex.plugin.message.MessageAlertColor;
import xyz.mcex.plugin.message.Messages;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class AccountGui extends NormalSequentialPanel
{
  private final int _pageNo;
  private final boolean _isBuy;
  private final DatabaseManager _manager;
  private final EquityDatabase _eqDb;
  private final Map<Integer, Order> _slotToOrder = new HashMap<>();
  private final MenuFlow _flow;
  private final Economy _economy;

  public AccountGui(Player player, DatabaseManager manager, int pageNo, boolean isBuy, Economy economy)
  {
    super(player, "");
    if (isBuy)
      this.setTitle("Buy orders for " + player.getName());
    else
      this.setTitle("Sell orders for " + player.getName());

    this._pageNo = pageNo;
    this._isBuy = isBuy;
    this._eqDb = new EquityDatabase(manager);
    this._manager = manager;
    this._flow = new MenuFlow(this);
    this._economy = economy;
  }

  public AccountGui(Player player, DatabaseManager manager, int pageNo, boolean isBuy, Economy economy, MenuFlow flow)
  {
    super(player, "");
    if (isBuy)
      this.setTitle("Buy orders for " + player.getName());
    else
      this.setTitle("Sell orders for " + player.getName());

    this._pageNo = pageNo;
    this._isBuy = isBuy;
    this._eqDb = new EquityDatabase(manager);
    this._manager = manager;
    this._flow = flow;
    this._economy = economy;
  }

  @Override
  public @Nullable
  Inventory makeInventory() throws Exception
  {
    Inventory inv = super.makeInventory();
    GetOrderResponse response;

    try
    {
      response = this._eqDb.getOrders(player().getUniqueId(), this._pageNo * 18, 18, this._isBuy);
    } catch (SQLException e)
    {
      Bukkit.getScheduler().runTask(McexPlugin.instance, () -> player().sendMessage(MessageAlertColor.ERROR + Messages.DATABASE_ERROR));
      return null;
    }

    if (response.orders.size() == 0)
    {
      if (this._pageNo == 0)
      {
        Bukkit.getScheduler().runTask(McexPlugin.instance, () -> player().sendMessage(MessageAlertColor.NOTIFY_AGNOSTIC + "You have no outstanding orders."));
        return null;
      }
      Bukkit.getScheduler().runTask(McexPlugin.instance, () -> player().sendMessage(MessageAlertColor.NOTIFY_AGNOSTIC + "You've reached the end of this database."));
      return null;
    } else if (response.orders.size() < 18)
      inv.setItem(26, Buttons.makeButton(Buttons.INACTIVE, "No next page!"));

    int i = 0;
    for (Order order : response.orders)
    {
      this._slotToOrder.put(i, order);

      inv.setItem(i, order.item.createInfoItem(order.quantity, order.price));
      this.setSlotListener(this.createListener(_pageNo), i);
      ++i;
    }

    return inv;
  }

  public Listener createListener(int pageNo)
  {
    return new Listener(this._flow, pageNo);
  }

  public class Listener implements SequentialPanel.Listener
  {
    private final MenuFlow _flow;
    private volatile int _pageNum;

    public Listener(MenuFlow flow, int pageNo)
    {
      this._pageNum = pageNo;
      this._flow = flow;
    }

    @Override
    public void onClick(SequentialPanel panel, Action action, InventoryClickEvent event)
    {
      event.setCancelled(true);
      if (action == Action.NEXT)
      {
        AccountGui newGui = new AccountGui(player(), _manager, ++this._pageNum, _isBuy, _economy, this._flow);
        newGui.setNextClickListener(newGui.createListener(this._pageNum));
        newGui.setBackClickListener(newGui.createListener(this._pageNum));
        this._flow.switchPanel(newGui);
      } else if (action == Action.BACK)
      {
        AccountGui newGui = new AccountGui(player(), _manager, --this._pageNum, _isBuy, _economy, this._flow);
        newGui.setNextClickListener(newGui.createListener(this._pageNum));
        if (_pageNum - 1 > 0)
          newGui.setBackClickListener(newGui.createListener(this._pageNum));
        this._flow.switchPanel(newGui);
      } else if (action == Action.CLICK) {
        AccountItemGui gui = new AccountItemGui(player(), title(), _manager, _slotToOrder.get(event.getRawSlot()), _isBuy, _economy);
        this._flow.pushToStack();
        gui.setBackClickListener((p, a, e) -> {
          Panel lastPanel = this._flow.popFromStack();
          this._flow.switchPanel(lastPanel);
        });
        this._flow.switchPanel(gui);
      }
    }
  }
}
