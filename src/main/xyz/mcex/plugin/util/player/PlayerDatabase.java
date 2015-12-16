package xyz.mcex.plugin.util.player;

import org.bukkit.Bukkit;
import xyz.mcex.plugin.Database;
import xyz.mcex.plugin.DatabaseManager;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class PlayerDatabase extends Database
{
  public PlayerDatabase(DatabaseManager manager)
  {
    super(manager);
  }

  public int fetchPlayerId(UUID playerUuid, Connection connection) throws SQLException, IOException
  {
    ResultSet playerRs = null;
    PreparedStatement insertPlayerStmt = null;
    PreparedStatement getPlayerIdStmt = null;

    try
    {
      getPlayerIdStmt = this._createGetPlayerId(connection);
      getPlayerIdStmt.setBinaryStream(1, PlayerUtils.uuidToStream(playerUuid), 16);
      playerRs = getPlayerIdStmt.executeQuery();

      if (!playerRs.next())
      {
        insertPlayerStmt = this._createInsertPlayerId(connection);
        insertPlayerStmt.setBinaryStream(1, PlayerUtils.uuidToStream(playerUuid), 16);
        insertPlayerStmt.execute();
        insertPlayerStmt.close();
      }

      playerRs.close();
      getPlayerIdStmt.setBinaryStream(1, PlayerUtils.uuidToStream(playerUuid), 16);
      playerRs = getPlayerIdStmt.executeQuery();
      if (!playerRs.next())
        throw new SQLException("Failed to insert new player");

      return playerRs.getInt(1);
    } finally {
      if (getPlayerIdStmt != null)
        getPlayerIdStmt.close();
      if (insertPlayerStmt != null)
        insertPlayerStmt.close();
      if (playerRs != null)
        playerRs.close();
    }
  }

  public UUID getPlayerUuid(int playerId, Connection connection) throws SQLException, IOException
  {
    PreparedStatement stmt = null;
    ResultSet rs = null;
    try
    {
      stmt = this._createGetPlayerUuid(connection);
      stmt.setInt(1, playerId);
      rs = stmt.executeQuery();
      if (!rs.next())
        throw new SQLException("Couldn't get player");
      return PlayerUtils.streamToUuid(rs.getBinaryStream(1));
    } finally {
      if (rs != null)
        rs.close();
      if (stmt != null)
        stmt.close();
    }
  }

  private PreparedStatement _createGetPlayerUuid(Connection connection) throws SQLException
  {
    return connection.prepareStatement("SELECT uuid FROM player_uuids WHERE id = ? LOCK IN SHARE MODE");
  }

  private PreparedStatement _createGetPlayerId(Connection connection) throws SQLException
  {
    return connection.prepareStatement("SELECT id FROM player_uuids WHERE uuid = ? LOCK IN SHARE MODE");
  }

  private PreparedStatement _createInsertPlayerId(Connection connection) throws SQLException
  {
    return connection.prepareStatement("INSERT INTO player_uuids (uuid) VALUES (?)");
  }
}
