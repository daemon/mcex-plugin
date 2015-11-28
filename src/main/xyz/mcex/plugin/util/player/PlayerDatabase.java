package xyz.mcex.plugin.util.player;

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
    PreparedStatement getPlayerIdStmt = this._createGetPlayerId(connection);
    getPlayerIdStmt.setBinaryStream(1, PlayerUtils.uuidToStream(playerUuid), 16);
    ResultSet playerRs = getPlayerIdStmt.executeQuery();

    if (!playerRs.next())
    {
      PreparedStatement insertPlayerStmt = this._createInsertPlayerId(connection);
      insertPlayerStmt.setBinaryStream(1, PlayerUtils.uuidToStream(playerUuid), 16);
      insertPlayerStmt.execute();
    }

    playerRs.close();
    getPlayerIdStmt.setBinaryStream(1, PlayerUtils.uuidToStream(playerUuid));
    playerRs = getPlayerIdStmt.executeQuery();
    if (!playerRs.next())
      throw new SQLException("Failed to insert new player");

    return playerRs.getInt(1);
  }

  public UUID getPlayerUuid(int playerId, Connection connection) throws SQLException, IOException
  {
    PreparedStatement stmt = this._createGetPlayerUuid(connection);
    stmt.setInt(1, playerId);
    ResultSet rs = stmt.executeQuery();
    if (!rs.next())
      throw new SQLException("Couldn't get player");
    return PlayerUtils.streamToUuid(rs.getBinaryStream(1));
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
