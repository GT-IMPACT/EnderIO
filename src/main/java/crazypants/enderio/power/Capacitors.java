package crazypants.enderio.power;

import net.minecraft.item.ItemStack;
import crazypants.enderio.EnderIO;

public enum Capacitors {

  BASIC_CAPACITOR(
	new BasicCapacitor(1, 80, 100000, 20),
      "basicCapacitor",
      "Basic"),

  ACTIVATED_CAPACITOR(
	new BasicCapacitor(2, 240, 200000, 60),
      "activatedCapacitor",
      "Advanced"),

  ENDER_CAPACITOR(
	new BasicCapacitor(3, 400, 500000, 100),
      "enderCapacitor",
      "Ender");


  public final ICapacitor capacitor;
  public final String unlocalisedName;
  public final String iconKey;
  public final String oreDict;

  private Capacitors(ICapacitor capacitor, String unlocalisedName, String oreDict) {
    this.capacitor = capacitor;
    this.unlocalisedName = "enderio." + unlocalisedName;
    this.iconKey = "enderio:" + unlocalisedName;
    this.oreDict = oreDict;
  }

  public String getOreTag() {
	  return "capacitor"+oreDict;
  }

  public ItemStack getItemStack() {
    return new ItemStack(EnderIO.itemBasicCapacitor, 1, ordinal());
  }

}
