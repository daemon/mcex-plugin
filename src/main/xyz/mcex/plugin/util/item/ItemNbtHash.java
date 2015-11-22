package xyz.mcex.plugin.util.item;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import xyz.mcex.plugin.internals.Nullable;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;

public class ItemNbtHash
{
  private byte[] _digest = null;

  private ItemNbtHash() {}

  public static @Nullable ItemNbtHash from(ItemStack item)
  {
    ItemNbtHash hash = new ItemNbtHash();
    hash.digest(item);
    if (hash._digest == null)
      return null;

    return hash;
  }

  public static @Nullable ItemNbtHash from(String b64Encode)
  {
    ItemNbtHash hash = new ItemNbtHash();
    hash._digest = Base64.getDecoder().decode(b64Encode.getBytes());
    return hash;
  }

  public String base64Digest()
  {
    return new String(Base64.getEncoder().encode(this._digest));
  }

  private @Nullable byte[] digest(ItemStack item)
  {
    if (this._digest != null)
      return this._digest;
    try
    {
      MessageDigest digest = MessageDigest.getInstance("SHA-1");
      ItemMeta meta = item.getItemMeta();
      if (meta != null)
      {
        if (meta.hasDisplayName())
          digest.update(meta.getDisplayName().getBytes());

        if (meta.hasLore())
          for (String s : meta.getLore())
            digest.update(s.getBytes());
      }

      ByteBuffer buf = ByteBuffer.allocate(4);
      buf.putShort(item.getDurability());
      digest.update(buf.array());
      digest.update(item.getData().getItemType().name().getBytes());

      return (this._digest = digest.digest());
    } catch (NoSuchAlgorithmException e)
    {
      return null;
    }
  }

  @Override
  public boolean equals(Object other)
  {
    if (other == null || !(other instanceof ItemNbtHash))
      return false;

    byte[] otherDigest = ((ItemNbtHash) other)._digest;
    if (otherDigest == null)
      return false;
    if (this._digest == null)
      return false;
    return Arrays.equals(otherDigest, this._digest);
  }
}