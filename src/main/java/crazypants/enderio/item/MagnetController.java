package crazypants.enderio.item;

import cpw.mods.fml.common.eventhandler.Cancelable;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import crazypants.enderio.config.Config;
import crazypants.enderio.item.PacketMagnetState.SlotType;
import crazypants.enderio.network.PacketHandler;
import crazypants.util.BaublesUtil;
import crazypants.util.ItemNBTHelper;
import crazypants.util.Vector3;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.event.entity.item.ItemTossEvent;

import java.util.ArrayList;
import java.util.List;

import static crazypants.enderio.item.darksteel.DarkSteelItems.itemMagnet;
import static crazypants.util.BotaniaUtil.hasSolegnoliaAround;
import static net.minecraft.item.Item.itemRegistry;

public class MagnetController {

  public MagnetController() {
    PacketHandler.INSTANCE.registerMessage(PacketMagnetState.class, PacketMagnetState.class, PacketHandler.nextID(), Side.SERVER);
  }

  @SubscribeEvent
  public void onPlayerTick(TickEvent.PlayerTickEvent event) {
    if (event.phase != TickEvent.Phase.END) {
      return;
    }
    ActiveMagnet mag = getActiveMagnet(event.player);
    if (mag != null && event.player.getHealth() > 0f) {
      doHoover(event.player, mag.item);
      if(event.side == Side.SERVER && event.player.worldObj.getTotalWorldTime() % 20 == 0) {
        ItemMagnet.drainPerSecondPower(mag.item);
        event.player.inventory.setInventorySlotContents(mag.slot, mag.item);
        event.player.inventory.markDirty();
      }
    }
  }

  private ActiveMagnet getActiveMagnet(EntityPlayer player) {
    ItemStack[] inv = player.inventory.mainInventory;
    int maxSlot = Config.magnetAllowInMainInventory ? 4 * 9 : 9;
    for (int i = 0; i < maxSlot;i++) {
      if(ItemMagnet.isActive(inv[i]) && ItemMagnet.hasPower(inv[i])) {
        return new ActiveMagnet(inv[i], i);
      }
    }
    return null;
  }
  
  private static final double collisionDistanceSq = 1.25 * 1.25;
  private static final double speed = 0.035;
  private static final double speed4 = speed * 4;
  
  public void doHoover(EntityPlayer player, ItemStack stack) {
    int cooldown = getCooldown(stack);

    if(hasSolegnoliaAround(player)) {
      if(cooldown < 0)
        setCooldown(stack, 2);
      return;
    }
    
    if (blacklist == null) {
      initBlacklist();
    }

    if(cooldown <= 0) {
      if(!player.isSneaking()) {

        double x = player.posX;
        double y = player.posY - (player.worldObj.isRemote ? 1.62 : 0) + 0.75;
        double z = player.posZ;
        int range = 16;
        List<EntityItem> items = player.worldObj.getEntitiesWithinAABB(EntityItem.class, AxisAlignedBB.getBoundingBox(x - range, y - range, z - range, x + range, y + range, z + range));
        int pulled = 0;
        for(EntityItem item : items)
          if(canPullItem(item)) {

            if(pulled > 200)
              break;

            setEntityMotionFromVector(item, new Vector3(x, y, z), 0.45F);
            pulled++;
          }
      }
    } else setCooldown(stack, cooldown - 1);
  }

  public static void setEntityMotionFromVector(Entity entity, Vector3 originalPosVector, float modifier) {
    Vector3 entityVector = Vector3.fromEntityCenter(entity);
    Vector3 finalVector = originalPosVector.copy().subtract(entityVector);

    if(finalVector.mag() > 1)
      finalVector.normalize();

    entity.motionX = finalVector.x * modifier;
    entity.motionY = finalVector.y * modifier;
    entity.motionZ = finalVector.z * modifier;
  }

  public static int getCooldown(ItemStack stack) {
    return ItemNBTHelper.getInt(stack, "cooldown", 0);
  }

  public static void setCooldown(ItemStack stack, int cooldown) {
    ItemNBTHelper.setInt(stack, "cooldown", cooldown);
  }

  private boolean canPullItem(EntityItem item) {
    if(item.isDead || hasSolegnoliaAround(item))
      return false;

    ItemStack stack = item.getEntityItem();
    if(stack == null || blacklist.contains(itemRegistry.getNameForObject(stack.getItem())))
      return false;

    return true;
  }

  private static List<Item> blacklist = null;

  private static void initBlacklist() {
    blacklist = new ArrayList<Item>();
    for (String name : Config.magnetBlacklist) {
      String[] parts = name.split(":");
      if (parts.length == 2) {
        Item item = GameRegistry.findItem(parts[0], parts[1]);
        if (item != null) {
          blacklist.add(item);
        }
      }
    }
  }

  private List<Entity> selectEntitiesWithinAABB(World world, AxisAlignedBB bb) {
    List<Entity> arraylist = null;

    int itemsRemaining = Config.magnetMaxItems;
    if (itemsRemaining <= 0) {
      itemsRemaining = Integer.MAX_VALUE;
    }

    final int minChunkX = MathHelper.floor_double((bb.minX) / 16.0D);
    final int maxChunkX = MathHelper.floor_double((bb.maxX) / 16.0D);
    final int minChunkZ = MathHelper.floor_double((bb.minZ) / 16.0D);
    final int maxChunkZ = MathHelper.floor_double((bb.maxZ) / 16.0D);
    final int minChunkY = MathHelper.floor_double((bb.minY) / 16.0D);
    final int maxChunkY = MathHelper.floor_double((bb.maxY) / 16.0D);

    for (int chunkX = minChunkX; chunkX <= maxChunkX; ++chunkX) {
      for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; ++chunkZ) {
        Chunk chunk = world.getChunkFromChunkCoords(chunkX, chunkZ);
        final int minChunkYClamped = MathHelper.clamp_int(minChunkY, 0, chunk.entityLists.length - 1);
        final int maxChunkYClamped = MathHelper.clamp_int(maxChunkY, 0, chunk.entityLists.length - 1);
        for (int chunkY = minChunkYClamped; chunkY <= maxChunkYClamped; ++chunkY) {
          for (Entity entity : (List<Entity>) chunk.entityLists[chunkY]) {
            if (!entity.isDead) {
              boolean gotOne = false;
              if (entity instanceof EntityItem && entity.boundingBox.intersectsWith(bb)) {
                gotOne = !hasSolegnoliaAround(entity);
                if (gotOne && !blacklist.isEmpty()) {
                  final Item item = ((EntityItem) entity).getEntityItem().getItem();
                  for (Item blacklisted : blacklist) {
                    if (blacklisted == item) {
                      gotOne = false;
                      break;
                    }
                  }
                }
              } else if (entity instanceof EntityXPOrb && entity.boundingBox.intersectsWith(bb)) {
                gotOne = true;
              }
              if (gotOne) {
                if (arraylist == null) {
                  arraylist = new ArrayList<Entity>(Config.magnetMaxItems > 0 ? Config.magnetMaxItems : 20);
                }
                arraylist.add(entity);
                if (itemsRemaining-- <= 0) {
                  return arraylist;
                }
              }
            }
          }
        }
      }
    }

    return arraylist;
  }

  private static class ActiveMagnet {
    ItemStack item;
    int slot;
    
    ActiveMagnet(ItemStack item, int slot) {    
      this.item = item;
      this.slot = slot;
    }        
  }

  public static void setMagnetActive(EntityPlayerMP player, SlotType type, int slot, boolean isActive) {
    ItemStack stack = null;
    IInventory baubles = null;
    int dropOff = -1;
    switch (type) {
    case INVENTORY:
      stack = player.inventory.getStackInSlot(slot);
      break;
    case ARMOR:
      return;
    case BAUBLES:
      baubles = BaublesUtil.instance().getBaubles(player);
      if (baubles != null) {
        stack = baubles.getStackInSlot(slot);
      }
      break;
    }
    if (stack == null || stack.getItem() == null || stack.getItem() != itemMagnet || ItemMagnet.isActive(stack) == isActive) {
      return;
    }
    if (!Config.magnetAllowDeactivatedInBaublesSlot && type == SlotType.BAUBLES && !isActive) {
      ItemStack[] inv = player.inventory.mainInventory;
      for (int i = 0; i < inv.length && dropOff < 0; i++) {
        if (inv[i] == null) {
          dropOff = i;
        }
      }
      if (dropOff < 0) {
        return;
      }
    }
    ItemMagnet.setActive(stack, isActive);
    switch (type) {
    case INVENTORY:
      player.inventory.setInventorySlotContents(slot, stack);
      player.inventory.markDirty();
      break;
    case ARMOR:
      return;
    case BAUBLES:
      if (dropOff < 0) {
        baubles.setInventorySlotContents(slot, stack);
      } else {
        baubles.setInventorySlotContents(slot, null);
        player.inventory.setInventorySlotContents(dropOff, stack);
      }
      player.inventory.markDirty();
      break;
    }
  }
}
