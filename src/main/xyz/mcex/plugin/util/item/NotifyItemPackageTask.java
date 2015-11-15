package xyz.mcex.plugin.util.item;

import org.bukkit.entity.Player;
import xyz.mcex.plugin.message.MessageAlertColor;

public class NotifyItemPackageTask implements Runnable
{
  private final Player _player;

  public NotifyItemPackageTask(Player player)
  {
    this._player = player;
  }

  @Override
  public void run()
  {
    this._player.sendMessage(MessageAlertColor.NOTIFY_AGNOSTIC + "You have new item packages waiting for you!");
    this._player.sendMessage(MessageAlertColor.NOTIFY_AGNOSTIC + "Do /mcex mailbox to view.");
  }
}
