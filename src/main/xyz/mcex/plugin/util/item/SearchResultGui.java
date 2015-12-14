package xyz.mcex.plugin.util.item;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import xyz.mcex.plugin.DatabaseManager;
import xyz.mcex.plugin.McexPlugin;
import xyz.mcex.plugin.equity.database.ItemDatabase;
import xyz.mcex.plugin.equity.database.RegisteredItem;
import xyz.mcex.plugin.gui.CancellingSlotListener;
import xyz.mcex.plugin.gui.MenuFlow;
import xyz.mcex.plugin.gui.NormalSequentialPanel;
import xyz.mcex.plugin.gui.SequentialPanel;
import xyz.mcex.plugin.internals.Nullable;
import xyz.mcex.plugin.message.MessageAlertColor;
import xyz.mcex.plugin.message.Messages;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

public class SearchResultGui extends NormalSequentialPanel
{
  private final int _pageNo;
  private final ItemDatabase _itemDb;
  private final String _query;

  public SearchResultGui(DatabaseManager manager, String query, Player player, int pageNo)
  {
    super(player, "Results for " + query);
    this._query = query;
    this._itemDb = new ItemDatabase(manager);
    this._pageNo = pageNo;
  }

  @Override
  public @Nullable Inventory makeInventory() throws Exception
  {
    Inventory inv = super.makeInventory();
    List<RegisteredItem> items;
    try
    {
      items = this._itemDb.findByName(this._query, this._pageNo * 18, 18);
    } catch (SQLException e)
    {
      Bukkit.getScheduler().runTask(McexPlugin.instance, () -> player().sendMessage(MessageAlertColor.ERROR + Messages.DATABASE_ERROR));
      throw e;
    }

    if (items.size() == 0)
    {
      Bukkit.getScheduler().runTask(McexPlugin.instance, () -> player().sendMessage(MessageAlertColor.NOTIFY_AGNOSTIC + "You've reached the end of of this database."));
      return null;
    }

    int i = 0;
    for (RegisteredItem item : items)
    {
      ItemStack[] itemStack = item.createItemStacks(1);
      ItemMeta meta = itemStack[0].getItemMeta();
      if (meta.getLore() == null)
        meta.setLore(new LinkedList<>());

      List<String> lores = meta.getLore();
      lores.add(0, ChatColor.RESET + "" + ChatColor.GRAY + "MCEX name: " + ChatColor.AQUA + item.alias);
      meta.setLore(lores);
      itemStack[0].setItemMeta(meta);
      inv.setItem(i, itemStack[0]);
      this.setSlotListener(new CancellingSlotListener(), i);

      ++i;
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
        SearchResultGui newGui = new SearchResultGui(_itemDb.manager(), _query, player(), ++this._pageNum);
        newGui.setNextClickListener(newGui.createListener(this._flow, this._pageNum));
        newGui.setBackClickListener(newGui.createListener(this._flow, this._pageNum));
        this._flow.switchPanel(newGui);
      } else if (action == Action.BACK) {
        SearchResultGui newGui = new SearchResultGui(_itemDb.manager(), _query, player(), --this._pageNum);
        newGui.setNextClickListener(newGui.createListener(this._flow, this._pageNum));
        if (_pageNum - 1 > 0)
          newGui.setBackClickListener(newGui.createListener(this._flow, this._pageNum));
        this._flow.switchPanel(newGui);
      }
    }
  }
}
