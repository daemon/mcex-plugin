package xyz.mcex.plugin.util.item;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import xyz.mcex.plugin.DatabaseManager;
import xyz.mcex.plugin.McexPlugin;
import xyz.mcex.plugin.equity.database.EquityDatabase;
import xyz.mcex.plugin.equity.database.ItemNotFoundException;
import xyz.mcex.plugin.equity.database.RegisteredItem;
import xyz.mcex.plugin.gui.*;
import xyz.mcex.plugin.message.MessageAlertColor;
import xyz.mcex.plugin.message.Messages;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

public class MailboxGui extends NormalSequentialPanel implements Listener
{
  private final int _pageNo;
  private final ItemPackageDatabase _pkgDb;
  private final Map<Integer, ItemPackage> _slotToPackage = new HashMap<>();
  private int _lastItemPkgNo = 18;
  private int _remainder = 0;

  public MailboxGui(Player player, DatabaseManager manager, int pageNo)
  {
    this(player, manager, pageNo, 18, 0);
  }

  MailboxGui(Player player, DatabaseManager manager, int pageNo, int lastItemPkgNo, int remainder)
  {
    super(player, "Mailbox for " + player.getName());
    this._pkgDb = new ItemPackageDatabase(manager, new EquityDatabase(manager));
    this._pageNo = pageNo;
    this._lastItemPkgNo = lastItemPkgNo;
    this._remainder = remainder;
  }

  private void unregisterAll()
  {
    InventoryCloseEvent.getHandlerList().unregister(this);
    InventoryClickEvent.getHandlerList().unregister(this);
    GuiVisibilityChangeEvent.getHandlerList().unregister(this);
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onEvent(InventoryCloseEvent event)
  {
    if (!event.getPlayer().getUniqueId().equals(this.player().getUniqueId()))
      return;

    if (this.gui() == null)
    {
      this.unregisterAll();
      return;
    }

    Bukkit.getScheduler().runTaskAsynchronously(McexPlugin.instance, () -> {
      try
      {
        this._pkgDb.cleanupNullPackages();
      } catch (SQLException | ItemNotFoundException | IOException e)
      {
        e.printStackTrace();
      }
    });
    this.unregisterAll();
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onEvent(InventoryClickEvent event)
  {
    if (!event.getWhoClicked().getUniqueId().equals(this.player().getUniqueId()))
      return;

    String actionName = event.getAction().name();
    if ((actionName.startsWith("PLACE") || actionName.startsWith("DROP") ||
        (actionName.startsWith("PICKUP") && !actionName.equals("PICKUP_ALL"))) && event.getRawSlot() < 27)
      event.setCancelled(true);
    else if (actionName.startsWith("MOVE_TO") && event.getRawSlot() >= 27)
      event.setCancelled(true);
    else if (event.getRawSlot() < 27)
    {
      ItemPackage pkg = this._slotToPackage.get(event.getRawSlot());
      if (pkg == null)
        return;

      ItemPackage clone = new ItemPackage(pkg.receiver, pkg.item, event.getCurrentItem().getAmount());
      Bukkit.getScheduler().runTaskAsynchronously(McexPlugin.instance, () -> {
        try
        {
          this._pkgDb.reducePackage(clone);
        } catch (SQLException | ItemNotFoundException | IOException e)
        {
          e.printStackTrace();
        }
      });
    }
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onEvent(GuiVisibilityChangeEvent event)
  {
    if (!event.open && event.player.getUniqueId().equals(player().getUniqueId()))
    {
      if (this.gui() == null)
      {
        this.unregisterAll();
        return;
      }

      Bukkit.getScheduler().runTaskAsynchronously(McexPlugin.instance, () -> {
        try
        {
          this._pkgDb.cleanupNullPackages();
        } catch (SQLException | ItemNotFoundException | IOException e)
        {
          e.printStackTrace();
        }
      });
      this.unregisterAll();
    }
  }


  @Override
  public Inventory makeInventory() throws Exception
  {
    Inventory inv = super.makeInventory();
    int i = 0;
    List<ItemPackage> packages;
    try
    {
      packages = this._pkgDb.getPackages(player().getUniqueId(), this._pageNo * 18, 18);
    } catch (SQLException e) {
      e.printStackTrace();
      Bukkit.getScheduler().runTask(McexPlugin.instance, () -> player().sendMessage(MessageAlertColor.ERROR + Messages.DATABASE_ERROR));
      throw e;
    }

    inv.setItem(26, Buttons.makeButton(Buttons.INACTIVE, "Paging disabled."));

    int k = 1;
    for (ItemPackage pkg : packages)
    {
      int j = 0;
      for (ItemStack item : pkg.toItemStacks())
      {
        if (i == 18)
          return inv;

        inv.setItem(i, item);
        _slotToPackage.put(i, pkg);
        ++i;
        j += item.getAmount();
      }
      ++k;
    }

    if (i == 0)
    {
      Bukkit.getScheduler().runTask(McexPlugin.instance, () -> player().sendMessage(MessageAlertColor.NOTIFY_AGNOSTIC + "You have no packages."));
      return null;
    }

    return inv;
  }

  public Listener createListener(MenuFlow flow, int pageNo)
  {
    return new Listener(flow, pageNo);
  }

  public class Listener implements SequentialPanel.Listener
  {
    private final MenuFlow _flow;
    private int _pageNo;

    public Listener(MenuFlow flow, int pageNo)
    {
      this._flow = flow;
      this._pageNo = pageNo;
    }

    @Override
    public void onClick(SequentialPanel panel, Action action, InventoryClickEvent event)
    {
      if (action == Action.NEXT)
      {
        MailboxGui newGui = new MailboxGui(player(), _pkgDb.manager(), ++this._pageNo);
        newGui.setNextClickListener(newGui.createListener(_flow, this._pageNo));
        newGui.setBackClickListener(newGui.createListener(_flow, this._pageNo));
        Bukkit.getPluginManager().registerEvents(newGui, McexPlugin.instance);
        this._flow.switchPanel(newGui);
      } else if (action == Action.BACK)
      {
        MailboxGui newGui = new MailboxGui(player(), _pkgDb.manager(), --this._pageNo);
        newGui.setNextClickListener(newGui.createListener(_flow, this._pageNo));
        if (_pageNo - 1 > 0)
          newGui.setBackClickListener(newGui.createListener(_flow, this._pageNo));
        Bukkit.getPluginManager().registerEvents(newGui, McexPlugin.instance);
        this._flow.switchPanel(newGui);
      }
    }
  }
}
