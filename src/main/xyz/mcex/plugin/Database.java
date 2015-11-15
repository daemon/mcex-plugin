package xyz.mcex.plugin;

public abstract class Database
{
  private final DatabaseManager _manager;

  public Database(DatabaseManager manager)
  {
    this._manager = manager;
  }

  public DatabaseManager manager()
  {
    return this._manager;
  }
}
