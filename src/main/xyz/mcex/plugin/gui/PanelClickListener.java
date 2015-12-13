package xyz.mcex.plugin.gui;

import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

import java.util.HashMap;
import java.util.Map;

public class PanelClickListener implements Listener
{
  private final Map<HumanEntity, Panel> _openGuiPlayers = new HashMap<>();

  @EventHandler(priority = EventPriority.LOWEST)
  public void onClickEvent(InventoryClickEvent event)
  {
    System.out.println(this._openGuiPlayers.containsKey(event.getWhoClicked()));
    this._openGuiPlayers.forEach((human, panel) -> System.out.println(human.getName()));
    if (!this._openGuiPlayers.containsKey(event.getWhoClicked()))
      return;

    event.setCancelled(true);
    this._openGuiPlayers.get(event.getWhoClicked()).onClickEvent(event.getRawSlot());
  }


  @EventHandler(priority = EventPriority.LOWEST)
  public void onInventoryCloseEvent(InventoryCloseEvent event)
  {
    this._openGuiPlayers.remove(event.getPlayer());
  }

  @EventHandler()
  public void onGuiOpenEvent(GuiVisibilityChangeEvent event)
  {
    if (event.open)
      this._openGuiPlayers.put(event.player, event.panel);
    else
      this._openGuiPlayers.remove(event.player);
  }
}
