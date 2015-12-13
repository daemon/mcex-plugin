package xyz.mcex.plugin.gui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class NormalSequentialPanel extends SequentialPanel
{
  public NormalSequentialPanel(Player player, String title)
  {
    super(player, title);
  }

  @Override
  public Inventory makeInventory()
  {
    Inventory inv = Bukkit.createInventory(this.player(), 27, this.title());
    inv.setItem(17, Buttons.makeButton(Buttons.INACTIVE, "No previous page!"));
    inv.setItem(26, Buttons.makeButton(Buttons.INACTIVE, "No next page!"));

    if (this.backClickListener() != null)
      inv.setItem(17, Buttons.makeButton(Buttons.ACTION, "Previous page"));
    if (this.nextClickListener() != null)
      inv.setItem(26, Buttons.makeButton(Buttons.ACTION, "Next page"));

    return inv;
  }

  @Override
  public void onClickEvent(int rawSlotNo)
  {
    if (rawSlotNo == 17 && this.backClickListener() != null)
      this.backClickListener().onClick(this, Action.BACK);
    else if (rawSlotNo == 26 && this.nextClickListener() != null)
      this.nextClickListener().onClick(this, Action.NEXT);
  }
}
