package com.stagesqx.neoforge;

import com.stagesqx.stage.StageAccess;
import com.stagesqx.stage.StageRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.fluids.FluidUtil;
import com.stagesqx.StagesQXConstants;

@EventBusSubscriber(modid = StagesQXConstants.MOD_ID)
public final class StageServerEvents {
	private StageServerEvents() {
	}

	@SubscribeEvent
	public static void onPickupPre(ItemEntityPickupEvent.Pre event) {
		if (!(event.getPlayer() instanceof ServerPlayer sp)) {
			return;
		}
		if (StageAccess.bypassesLocks(sp)) {
			return;
		}
		ItemStack stack = event.getItemEntity().getItem();
		if (StageAccess.isItemBlocked(StageRegistry.getCatalog(), PlayerStageStore.get(sp), stack)) {
			event.setCanPickup(TriState.FALSE);
			StageFeedback.notifyBlocked(sp, stack);
		}
	}

	@SubscribeEvent
	public static void onDrop(ItemTossEvent event) {
		if (!(event.getPlayer() instanceof ServerPlayer sp)) {
			return;
		}
		if (StageAccess.bypassesLocks(sp)) {
			return;
		}
		ItemStack stack = event.getEntity().getItem();
		if (StageAccess.isItemBlocked(StageRegistry.getCatalog(), PlayerStageStore.get(sp), stack)) {
			event.setCanceled(true);
			StageFeedback.notifyBlocked(sp, stack);
		}
	}

	@SubscribeEvent
	public static void onTravel(EntityTravelToDimensionEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer sp)) {
			return;
		}
		if (StageAccess.bypassesLocks(sp)) {
			return;
		}
		ResourceLocation dim = event.getDimension().location();
		if (StageAccess.isDimensionBlocked(StageRegistry.getCatalog(), PlayerStageStore.get(sp), dim)) {
			event.setCanceled(true);
			StageFeedback.notifyBlockedDimension(sp, dim);
		}
	}

	@SubscribeEvent
	public static void onAttack(LivingIncomingDamageEvent event) {
		if (!(event.getSource().getEntity() instanceof ServerPlayer sp)) {
			return;
		}
		if (StageAccess.bypassesLocks(sp)) {
			return;
		}
		ItemStack weapon = sp.getMainHandItem();
		if (!weapon.isEmpty() && StageAccess.isItemBlocked(StageRegistry.getCatalog(), PlayerStageStore.get(sp), weapon)) {
			event.setCanceled(true);
			StageFeedback.notifyBlocked(sp, weapon);
			return;
		}
		Entity target = event.getEntity();
		ResourceLocation typeId = EntityType.getKey(target.getType());
		if (typeId != null && StageAccess.isEntityBlocked(StageRegistry.getCatalog(), PlayerStageStore.get(sp), typeId)) {
			event.setCanceled(true);
		}
	}

	@SubscribeEvent
	public static void onUseItem(PlayerInteractEvent.RightClickItem event) {
		if (!(event.getEntity() instanceof ServerPlayer sp) || event.getLevel().isClientSide()) {
			return;
		}
		if (StageAccess.bypassesLocks(sp)) {
			return;
		}
		ItemStack stack = event.getItemStack();
		if (shouldCancelItemOrFluidUse(sp, stack)) {
			event.setCancellationResult(InteractionResult.FAIL);
			event.setCanceled(true);
			notifyBlockedForHeldStack(sp, stack);
		}
	}

	/**
	 * Right-click on a block runs {@link PlayerInteractEvent.RightClickBlock}, not {@link PlayerInteractEvent.RightClickItem}.
	 */
	@SubscribeEvent
	public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
		if (!(event.getEntity() instanceof ServerPlayer sp) || event.getLevel().isClientSide()) {
			return;
		}
		if (StageAccess.bypassesLocks(sp)) {
			return;
		}
		ItemStack stack = event.getItemStack();
		if (shouldCancelItemOrFluidUse(sp, stack)) {
			event.setCancellationResult(InteractionResult.FAIL);
			event.setCanceled(true);
			notifyBlockedForHeldStack(sp, stack);
			return;
		}
		var targetState = event.getLevel().getBlockState(event.getPos());
		if (StageAccess.isBlockBlocked(StageRegistry.getCatalog(), PlayerStageStore.get(sp), targetState)) {
			event.setCancellationResult(InteractionResult.FAIL);
			event.setCanceled(true);
			StageFeedback.notifyBlockedForBlock(sp, targetState);
		}
	}

	@SubscribeEvent
	public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
		if (!(event.getEntity() instanceof ServerPlayer sp) || event.getLevel().isClientSide()) {
			return;
		}
		if (StageAccess.bypassesLocks(sp)) {
			return;
		}
		ItemStack stack = event.getItemStack();
		if (shouldCancelItemOrFluidUse(sp, stack)) {
			event.setCancellationResult(InteractionResult.FAIL);
			event.setCanceled(true);
			notifyBlockedForHeldStack(sp, stack);
		}
	}

	@SubscribeEvent
	public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
		if (!(event.getEntity() instanceof ServerPlayer sp) || event.getLevel().isClientSide()) {
			return;
		}
		if (StageAccess.bypassesLocks(sp)) {
			return;
		}
		ItemStack stack = event.getItemStack();
		if (shouldCancelItemOrFluidUse(sp, stack)) {
			event.setCancellationResult(InteractionResult.FAIL);
			event.setCanceled(true);
			notifyBlockedForHeldStack(sp, stack);
		}
	}

	/**
	 * Left-click on a block (start mining / block attack) uses the main hand stack.
	 */
	@SubscribeEvent
	public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
		if (!(event.getEntity() instanceof ServerPlayer sp) || event.getLevel().isClientSide()) {
			return;
		}
		if (StageAccess.bypassesLocks(sp)) {
			return;
		}
		if (event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.START) {
			return;
		}
		ItemStack stack = event.getItemStack();
		if (shouldCancelItemOrFluidUse(sp, stack)) {
			event.setCanceled(true);
			notifyBlockedForHeldStack(sp, stack);
			return;
		}
		var targetState = event.getLevel().getBlockState(event.getPos());
		if (StageAccess.isBlockBlocked(StageRegistry.getCatalog(), PlayerStageStore.get(sp), targetState)) {
			event.setCanceled(true);
			StageFeedback.notifyBlockedForBlock(sp, targetState);
		}
	}

	private static boolean shouldCancelItemOrFluidUse(ServerPlayer sp, ItemStack stack) {
		if (stack.isEmpty()) {
			return false;
		}
		if (StageAccess.isItemBlocked(StageRegistry.getCatalog(), PlayerStageStore.get(sp), stack)) {
			return true;
		}
		return FluidUtil.getFluidContained(stack).map(fs -> {
			Fluid f = fs.getFluid();
			ResourceLocation rid = BuiltInRegistries.FLUID.getKey(f);
			return rid != null && StageAccess.isFluidBlocked(StageRegistry.getCatalog(), PlayerStageStore.get(sp), rid);
		}).orElse(false);
	}

	private static void notifyBlockedForHeldStack(ServerPlayer sp, ItemStack stack) {
		if (stack.isEmpty()) {
			return;
		}
		if (StageAccess.isItemBlocked(StageRegistry.getCatalog(), PlayerStageStore.get(sp), stack)) {
			StageFeedback.notifyBlocked(sp, stack);
			return;
		}
		FluidUtil.getFluidContained(stack).ifPresent(fs -> {
			Fluid f = fs.getFluid();
			ResourceLocation rid = BuiltInRegistries.FLUID.getKey(f);
			if (rid != null && StageAccess.isFluidBlocked(StageRegistry.getCatalog(), PlayerStageStore.get(sp), rid)) {
				StageFeedback.notifyBlockedFluid(sp, rid);
			}
		});
	}

	@SubscribeEvent
	public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
		if (event.getEntity() instanceof ServerPlayer sp) {
			PlayerStageStore.applyStartingStagesFromConfig(sp);
			StageNetwork.syncAllTo(sp);
		}
	}

	@SubscribeEvent
	public static void onPlayerTickPost(PlayerTickEvent.Post event) {
		if (!(event.getEntity() instanceof ServerPlayer sp)) {
			return;
		}
		if (!StagesQXModConfig.EJECT_BLOCKED_INVENTORY_ITEMS.get()) {
			return;
		}
		int interval = StagesQXModConfig.EJECT_BLOCKED_INVENTORY_INTERVAL_TICKS.get();
		if (interval <= 0 || sp.tickCount % interval != 0) {
			return;
		}
		if (StageAccess.bypassesLocks(sp)) {
			return;
		}
		StageInventoryEject.ejectBlockedStacks(sp);
	}
}
