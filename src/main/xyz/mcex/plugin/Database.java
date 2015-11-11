package xyz.mcex.plugin;

public abstract class Database
{
  private final DatabaseManager _manager;

  public Database(DatabaseManager manager)
  {
    this._manager = manager;
  }

  protected DatabaseManager manager()
  {
    return this._manager;
  }
}
