package crazypants.enderio.machines.config.config;

import crazypants.enderio.base.config.factory.IValue;
import crazypants.enderio.base.config.factory.IValueFactory;
import crazypants.enderio.machines.config.Config;

public final class EnderGenConfig {

  public static final IValueFactory F = Config.F.section("generator.ender");

  public static final IValue<Integer> ticksPerBucketOfFuel = F.make("ticksPerBucketOfFuel", 10 * 120 * 20, //
      "The number of ticks one bucket of fuel lasts.").setMin(1).sync();
  public static final IValue<Float> minimumTankLevel = F.make("minimumTankLevel", 0.7f, //
      "How full does the tank need to be for the enderman head to produce energy?. (0.0-0.9995)").setRange(0, 0.9995).sync();

}
