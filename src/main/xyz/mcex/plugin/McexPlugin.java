package xyz.mcex.plugin;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.mcex.plugin.equity.BuyCommand;
import xyz.mcex.plugin.equity.SellCommand;
import xyz.mcex.plugin.equity.database.EquityDatabase;
import xyz.mcex.plugin.util.item.AcceptItemCommand;
import xyz.mcex.plugin.util.item.ListItemCommand;

import java.beans.PropertyVetoException;
import java.sql.Connection;
import java.sql.SQLException;

public class McexPlugin extends JavaPlugin
{
  @Override
  public void onEnable()
  {
    this.saveDefaultConfig();
    this.getLogger().info("Enabling MCEX...");
    this.getLogger().info("Initializing tables...");
    DatabaseManager manager = null;
    try
    {
      manager = new DatabaseManager(this.getConfig());
      manager.createDefaultTables();
      Connection conn = manager.getConnection();
      conn.close();
    } catch (PropertyVetoException e)
    {
      this.getLogger().warning("Failed to initialize database manager.");
      e.printStackTrace();
      return;
    } catch (SQLException e)
    {
      this.getLogger().warning("Connecting to database failed!");
      e.printStackTrace();
      return;
    }

    RegisteredServiceProvider<Economy> provider = Bukkit.getServicesManager().getRegistration(Economy.class);
    if (provider == null)
    {
      this.getLogger().warning("Vault not installed!");
      return;
    }

    EquityDatabase eqDb = new EquityDatabase(manager);

    BaseCommand baseCmd = new BaseCommand();
    baseCmd.registerCommand("buy", new BuyCommand(provider.getProvider(), this, eqDb));
    baseCmd.registerCommand("sell", new SellCommand(this, eqDb));
    baseCmd.registerCommand("accept", new AcceptItemCommand(manager));
    baseCmd.registerCommand("mailbox", new ListItemCommand(manager));
    this.getCommand("mcex").setExecutor(baseCmd);
  }

  @Override
  public void onDisable()
  {

  }
}
