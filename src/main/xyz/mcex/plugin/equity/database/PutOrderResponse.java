package xyz.mcex.plugin.equity.database;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;

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

  void exerciseOrders(Economy economy)
  {
    playerUuidToMoney.forEach((uuid, orderVal) -> {
      {
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        if (player == null)
          return;
        economy.depositPlayer(player, orderVal);
      }
    });
  }
}
