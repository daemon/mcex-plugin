package xyz.mcex.plugin.util.item;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import xyz.mcex.plugin.DatabaseManager;
import xyz.mcex.plugin.McexPlugin;
import xyz.mcex.plugin.equity.database.EquityDatabase;
import xyz.mcex.plugin.message.MessageAlertColor;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class MailboxLoginListener implements Listener
{
  private final ItemPackageDatabase _ipDb;

  public MailboxLoginListener(DatabaseManager manager)
  {
    this._ipDb = new ItemPackageDatabase(manager, new EquityDatabase(manager));
  }

  @EventHandler()
  public void onLogin(PlayerLoginEvent event)
  {
    Bukkit.getScheduler().runTaskAsynchronously(McexPlugin.instance, () -> {
      List<ItemPackage> packages = null;
      try
      {
        packages = this._ipDb.getPackages(event.getPlayer().getUniqueId(), 0, 1);
      } catch (SQLException | IOException e) {
        return;
      }

      if (packages.isEmpty())
        return;

      Bukkit.getScheduler().runTask(McexPlugin.instance, new NotifyItemPackageTask(event.getPlayer()));
    });
  }
}
