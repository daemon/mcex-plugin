package xyz.mcex.plugin.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.UUID;

public class PlayerUtils
{
  public static ByteArrayInputStream uuidToStream(UUID uuid) throws IOException
  {
    ByteArrayOutputStream ba = new ByteArrayOutputStream(16);
    DataOutputStream os = new DataOutputStream(ba);
    os.writeLong(uuid.getMostSignificantBits());
    os.writeLong(uuid.getLeastSignificantBits());
    return new ByteArrayInputStream(ba.toByteArray());
  }
}
