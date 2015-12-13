package xyz.mcex.plugin.gui;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public abstract class SequentialPanel extends Panel
{
  public enum Action { NEXT, BACK, CLICK };
  private Listener _nextClickListener = null;
  private Listener _backClickListener = null;
  private Map<Integer, Listener> _slotListeners = new HashMap<>();

  public SequentialPanel(Player player, String title)
  {
    super(player, title);
  }

  protected Listener nextClickListener()
  {
    return this._nextClickListener;
  }

  protected Listener backClickListener()
  {
    return this._backClickListener;
  }

  protected Map<Integer, Listener> slotListeners()
  {
    return this._slotListeners;
  }

  public void setNextClickListener(Listener listener)
  {
    this._nextClickListener = listener;
  }

  public void setBackClickListener(Listener listener)
  {
    this._backClickListener = listener;
  }

  public void setSlotListener(Listener listener, int rawSlot)
  {
    this._slotListeners.put(rawSlot, listener);
  }

  @FunctionalInterface
  public interface Listener
  {
    void onClick(SequentialPanel panel, Action action);
  }
}
