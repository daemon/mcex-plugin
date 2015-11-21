package xyz.mcex.plugin.equity.database;

import java.util.LinkedList;
import java.util.List;

public class GetOrderResponse
{
  public enum ResponseCode { OK, FAILURE_NOT_FOUND, FAILURE_SQL };
  public final List<Order> orders;
  public final ResponseCode code;

  public GetOrderResponse(ResponseCode code)
  {
    this.code = code;
    this.orders = new LinkedList<>();
  }

  public GetOrderResponse(ResponseCode code, List<Order> orders)
  {
    this.code = code;
    this.orders = orders;
  }
}
