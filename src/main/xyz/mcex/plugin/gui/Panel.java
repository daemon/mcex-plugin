package xyz.mcex.plugin.gui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import xyz.mcex.plugin.McexPlugin;

public abstract class Panel
{
  private final Player _player;
  private String _title;
  private volatile boolean _dirty = true;
  private Inventory _gui;

  public Panel(Player player, String title)
  {
    this._player = player;
    this._title = title;
  }

  protected Inventory gui()
  {
    return this._gui;
  }

  public void hide()
  {
    Bukkit.getPluginManager().callEvent(new GuiVisibilityChangeEvent(this._player, this, false));
    this._player.closeInventory();
  }

  public void change(Panel other)
  {
    Bukkit.getPluginManager().callEvent(new GuiVisibilityChangeEvent(this._player, this, false));
    other.show();
  }

  public void update()
  {
    this._dirty = true;
    this.show();
  }

  public boolean isOpen()
  {
    // TODO: stub
    return false;
  }

  public void setTitle(String title)
  {
    this._title = title;
  }

  public Player player()
  {
    return this._player;
  }

  public String title()
  {
    return this._title;
  }

  private void showInv()
  {
    Bukkit.getScheduler().runTask(McexPlugin.instance, () -> {
      this._player.openInventory(this._gui);
      Bukkit.getPluginManager().callEvent(new GuiVisibilityChangeEvent(this._player, this, true));
      this._dirty = false;
    });
  }

  public void show()
  {
    if (!this._dirty)
    {
      this.showInv();
      return;
    }

    Bukkit.getScheduler().runTaskAsynchronously(McexPlugin.instance, () -> {
      try
      {
        this._gui = this.makeInventory();
      } catch (Exception e) {
        e.printStackTrace();
        return;
      }

      if (this._gui == null)
      {
        this.player().closeInventory();
        return;
      }

      this.showInv();
    });
  }

  public abstract Inventory makeInventory() throws Exception;
  public abstract void onClickEvent(InventoryClickEvent event);
}
