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
  public enum ResponseCode { OK, FAILURE_NOT_FOUND, FAILURE_SQL };
  public final int totalMoney;
  public final int totalQuantity;
  public final ResponseCode responseCode;

  public final Map<UUID, Double> playerUuidToMoney = new HashMap<>();

  PutOrderResponse(ResponseCode code)
  {
    this.responseCode = code;
    this.totalMoney = 0;
    this.totalQuantity = 0;
  }

  PutOrderResponse(ResponseCode code, int money, int totalQuantity)
  {
    this.totalMoney = money;
    this.totalQuantity = totalQuantity;
    this.responseCode = code;
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
