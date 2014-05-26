package squeek.spiceoflife.foodtracker;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.ForgeSubscribe;
import net.minecraftforge.event.entity.EntityEvent.EntityConstructing;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import squeek.spiceoflife.network.PacketConfigSync;
import squeek.spiceoflife.network.PacketFoodEatenAllTime;
import squeek.spiceoflife.network.PacketFoodHistory;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.IPlayerTracker;
import cpw.mods.fml.common.network.PacketDispatcher;
import cpw.mods.fml.common.network.Player;

public class FoodTracker implements IPlayerTracker
{
	/**
	 * Add relevant extended entity data whenever an entity comes into existence
	 */
	@ForgeSubscribe
	public void onEntityConstructing(EntityConstructing event)
	{
		if (event.entity instanceof EntityPlayer && FoodHistory.get((EntityPlayer) event.entity) == null)
		{
			EntityPlayer player = (EntityPlayer) event.entity;
			new FoodHistory(player);
		}
	}

	/**
	 * Sync savedata/config whenever a player joins the server
	 */
	@Override
	public void onPlayerLogin(EntityPlayer player)
	{
		if (FMLCommonHandler.instance().getEffectiveSide().isClient())
			return;

		// server needs to send any loaded data to the client
		FoodHistory foodHistory = FoodHistory.get(player);
		syncFoodHistory(foodHistory);

		// server needs to send config settings to the client
		PacketDispatcher.sendPacketToPlayer(new PacketConfigSync().getPacket(), (Player) player);
	}

	/**
	 * Save death-persistent data to avoid any rollbacks on respawn
	 */
	@ForgeSubscribe
	public void onLivingDeathEvent(LivingDeathEvent event)
	{
		if (FMLCommonHandler.instance().getEffectiveSide().isClient() || !(event.entity instanceof EntityPlayer))
			return;
		
		EntityPlayer player = (EntityPlayer) event.entity;

		FoodHistory foodHistory = FoodHistory.get(player);
		foodHistory.saveNBTData(null);
	}

	/**
	 * Load any death-persistent savedata on respawn and sync it
	 */
	@Override
	public void onPlayerRespawn(EntityPlayer player)
	{
		if (FMLCommonHandler.instance().getEffectiveSide().isClient())
			return;

		// load any persistent food history data
		FoodHistory foodHistory = FoodHistory.get(player);
		foodHistory.loadNBTData(null);
		
		// server needs to send any loaded data to the client
		syncFoodHistory(foodHistory);
	}
	
	public void syncFoodHistory(FoodHistory foodHistory)
	{
		PacketDispatcher.sendPacketToPlayer(new PacketFoodEatenAllTime(foodHistory.totalFoodsEatenAllTime).getPacket(), (Player) foodHistory.player);
		PacketDispatcher.sendPacketToPlayer(new PacketFoodHistory(foodHistory, true).getPacket(), (Player) foodHistory.player);
	}

	public static boolean addFoodEatenByPlayer(ItemStack food, EntityPlayer player)
	{
		ItemStack foodCopy = food.copy();
		foodCopy.stackSize = 1;

		// client needs to be told by the server otherwise the client can get out of sync easily
		if (!player.worldObj.isRemote)
			PacketDispatcher.sendPacketToPlayer(new PacketFoodHistory(foodCopy).getPacket(), (Player) player);

		return FoodHistory.get(player).addFood(foodCopy);
	}

	public static int getFoodHistoryCountOf(ItemStack food, EntityPlayer player)
	{
		return FoodHistory.get(player).getFoodCount(food);
	}

	public static int getFoodHistoryCountOfLastEatenBy(EntityPlayer player)
	{
		return FoodHistory.get(player).getFoodCount(getFoodLastEatenBy(player));
	}

	public static int getFoodHistorySize(EntityPlayer player)
	{
		return FoodHistory.get(player).getHistorySize();
	}

	public static ItemStack getFoodLastEatenBy(EntityPlayer player)
	{
		return FoodHistory.get(player).getLastEatenFood();
	}

	@Override
	public void onPlayerLogout(EntityPlayer player)
	{
	}

	@Override
	public void onPlayerChangedDimension(EntityPlayer player)
	{
	}
}
