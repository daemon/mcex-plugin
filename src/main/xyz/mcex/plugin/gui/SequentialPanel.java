package xyz.mcex.plugin.gui;

import org.bukkit.entity.Player;

public abstract class SequentialPanel extends Panel
{
  public SequentialPanel(Player player, String title)
  {
    super(player, title);
  }

  public enum Action { NEXT, BACK };
  private Listener _nextClickListener = null;
  private Listener _backClickListener = null;

  protected Listener nextClickListener()
  {
    return this._nextClickListener;
  }

  protected Listener backClickListener()
  {
    return this._backClickListener;
  }

  public void setNextClickListener(Listener listener)
  {
    this._nextClickListener = listener;
  }

  public void setBackClickListener(Listener listener)
  {
    this._backClickListener = listener;
  }

  public interface Listener
  {
    void onClick(SequentialPanel panel, Action action);
  }
}
