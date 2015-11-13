package xyz.mcex.plugin.equity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PutOrderResponse
{
  public final int totalMoney;
  public final int totalQuantity;
  public final Map<UUID, Integer> playerUuidToMoney = new HashMap<>();

  PutOrderResponse()
  {
    this.totalMoney = 0;
    this.totalQuantity = 0;
  }

  PutOrderResponse(int money, int totalQuantity)
  {
    this.totalMoney = money;
    this.totalQuantity = totalQuantity;
  }
}
