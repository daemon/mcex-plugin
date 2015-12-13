package xyz.mcex.plugin.gui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import xyz.mcex.plugin.McexPlugin;

public abstract class Panel
{
  private final Player _player;
  private final String _title;
  private volatile boolean _dirty = true;
  private Inventory _gui;

  public Panel(Player player, String title)
  {
    this._player = player;
    this._title = title;
  }

  public void hide()
  {
    Bukkit.getPluginManager().callEvent(new GuiVisibilityChangeEvent(this._player, this, false));
    this._player.closeInventory();
  }

  public void change(Panel other)
  {
    Bukkit.getPluginManager().callEvent(new GuiVisibilityChangeEvent(this._player, other, false));
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

  public Player player()
  {
    return this._player;
  }

  public String title()
  {
    return this._title;
  }

  public void show()
  {
    if (!this._dirty)
      return;

    Bukkit.getScheduler().runTaskAsynchronously(McexPlugin.instance, () -> {
      try
      {
        this._gui = this.makeInventory();
      } catch (Exception e) {
        return;
      }

      if (this._gui == null)
      {
        this.player().closeInventory();
        return;
      }

      Bukkit.getScheduler().runTask(McexPlugin.instance, () -> {
        Bukkit.getPluginManager().callEvent(new GuiVisibilityChangeEvent(this._player, this, true));
        this._player.openInventory(this._gui);
        this._dirty = false;
      });
    });
  }

  public abstract Inventory makeInventory() throws Exception;
  public abstract void onClickEvent(int rawSlotNo);
}
