package xyz.mcex.plugin.gui;

import org.bukkit.event.inventory.InventoryClickEvent;

public class CancellingSlotListener implements SequentialPanel.Listener
{
  @Override
  public void onClick(SequentialPanel panel, SequentialPanel.Action action, InventoryClickEvent event)
  {
    event.setCancelled(true);
  }
}
