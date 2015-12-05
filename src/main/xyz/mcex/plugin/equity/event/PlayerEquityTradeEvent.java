package xyz.mcex.plugin.equity.event;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import xyz.mcex.plugin.equity.database.RegisteredItem;

import java.util.UUID;

public final class PlayerEquityTradeEvent extends Event
{
  private static final HandlerList handlers = new HandlerList();
  public final OfflinePlayer seller, buyer;
  public final RegisteredItem item;
  public final int quantity;
  public double offerValue;

  public PlayerEquityTradeEvent(OfflinePlayer seller, OfflinePlayer buyer, RegisteredItem item, int quantity, double offerValue)
  {
    this.seller = seller;
    this.buyer = buyer;
    this.item = item;
    this.quantity = quantity;
    this.offerValue = offerValue;
  }

  public HandlerList getHandlers()
  {
    return handlers;
  }
}
