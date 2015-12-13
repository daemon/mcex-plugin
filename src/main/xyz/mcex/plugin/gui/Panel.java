package xyz.mcex.plugin.gui;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public abstract class Panel
{
  private final Player _player;
  private final String _title;
  private boolean _dirty = true;
  private Inventory gui;

  public Panel(Player player, String title)
  {
    this._player = player;
    this._title = title;
  }

  public void hide()
  {
    this._player.closeInventory();
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
    if (this._dirty)
      gui = this.makeInventory();
    this._player.openInventory(this.gui);
    this._dirty = false;
  }

  public abstract Inventory makeInventory();
  public abstract void onClickEvent(int rawSlotNo);
}
