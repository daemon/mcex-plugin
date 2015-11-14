package xyz.mcex.plugin.util.item;

import org.bukkit.Material;

import java.util.UUID;

public class ItemPackage
{
  final UUID _receiver;
  final int _quantity;
  final Material _material;

  public ItemPackage(UUID receiver, Material material, int quantity)
  {
    this._receiver = receiver;
    this._material = material;
    this._quantity = quantity;
  }
}
