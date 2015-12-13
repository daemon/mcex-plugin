package xyz.mcex.plugin.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class GuiVisibilityChangeEvent extends Event
{
  private static final HandlerList handlers = new HandlerList();

  public final Player player;
  public final Panel panel;
  public final boolean open;

  public GuiVisibilityChangeEvent(Player player, Panel panel, boolean open)
  {
    this.player = player;
    this.panel = panel;
    this.open = open;
  }

  @Override
  public HandlerList getHandlers()
  {
    return handlers;
  }
}
