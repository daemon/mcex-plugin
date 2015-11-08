package xyz.mcex.plugin.ui;

import org.bukkit.entity.Player;
import xyz.mcex.plugin.internals.Nullable;

import java.util.List;
import java.util.Map;

public class InventoryGui
{
  private List<InventoryButton> _buttons;
  private Map<String, Object> _arguments;
  private Player _currOpenPlayer = null;

  public InventoryGui()
  {
    this(null);
  }

  public InventoryGui(@Nullable List<InventoryButton> buttons)
  {
    this(buttons, null);
  }

  public InventoryGui(@Nullable List<InventoryButton> buttons, @Nullable Map<String, Object> args)
  {
    this._buttons = buttons;
    this._arguments = args;
  }

  public void setArguments(@Nullable Map<String, Object> arguments)
  {
    this._arguments = arguments;
  }

  public void addArguments(@Nullable Map<String, Object> arguments)
  {
    if (this._arguments == null)
    {
      this._arguments = arguments;
      return;
    }

    arguments.forEach(this._arguments::put);
  }

  public void renderTo(Player player)
  {
    this._currOpenPlayer = player;
    player.openInventory(inventory);
  }

  public void switchGui(InventoryGui gui)
  {
    if (this._currOpenPlayer == null)
      return;

    gui.renderTo()
  }
}
