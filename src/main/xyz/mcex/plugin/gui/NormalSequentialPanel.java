package xyz.mcex.plugin.gui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.sql.SQLException;

public class NormalSequentialPanel extends SequentialPanel
{
  public NormalSequentialPanel(Player player, String title)
  {
    super(player, title);
  }

  @Override
  public Inventory makeInventory() throws SQLException
  {
    Inventory inv = Bukkit.createInventory(this.player(), 27, this.title());
    inv.setItem(18, Buttons.makeButton(Buttons.INACTIVE, "No previous page!"));
    inv.setItem(26, Buttons.makeButton(Buttons.INACTIVE, "No next page!"));

    if (this.backClickListener() != null)
      inv.setItem(18, Buttons.makeButton(Buttons.ACTION, "Previous page"));
    if (this.nextClickListener() != null)
      inv.setItem(26, Buttons.makeButton(Buttons.ACTION, "Next page"));

    return inv;
  }

  @Override
  public void onClickEvent(int rawSlotNo)
  {
    if (rawSlotNo == 18 && this.backClickListener() != null)
      this.backClickListener().onClick(this, Action.BACK);
    else if (rawSlotNo == 26 && this.nextClickListener() != null)
      this.nextClickListener().onClick(this, Action.NEXT);
    else if (this.slotListeners().get(rawSlotNo) != null)
      this.slotListeners().get(rawSlotNo).onClick(this, Action.CLICK);
  }
}
