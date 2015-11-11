package xyz.mcex.plugin;

import org.bukkit.plugin.java.JavaPlugin;

import java.beans.PropertyVetoException;
import java.sql.SQLException;

public class McexPlugin extends JavaPlugin
{
  @Override
  public void onEnable()
  {
    this.getLogger().info("Enabling MCEX...");
    this.getLogger().info("Initializing tables...");
    DatabaseManager manager = null;
    try
    {
      manager = new DatabaseManager(this.getConfig());
      manager.createDefaultTables();
    } catch (PropertyVetoException e)
    {
      this.getLogger().warning("Failed to initialize database manager.");
      return;
    } catch (SQLException e)
    {
      this.getLogger().warning("Creating tables failed!");
      return;
    }


  }

  @Override
  public void onDisable()
  {

  }
}
