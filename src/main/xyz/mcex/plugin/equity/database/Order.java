package xyz.mcex.plugin.equity.database;

import java.util.UUID;

class Order
{
  public final UUID playerUuid;
  public final int quantity;
  public final double price;
  public final int rowId;

  public Order(int rowId, UUID playerUuid, int quantity, double price)
  {
    this.playerUuid = playerUuid;
    this.quantity = quantity;
    this.price = price;
    this.rowId = rowId;
  }
}
