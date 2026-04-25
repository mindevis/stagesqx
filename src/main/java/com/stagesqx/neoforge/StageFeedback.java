package com.stagesqx.neoforge;

import com.stagesqx.stage.StageAccess;
import com.stagesqx.stage.StageCatalog;
import com.stagesqx.stage.StageDefinition;
import com.stagesqx.stage.StageRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

public final class StageFeedback {
	private StageFeedback() {
	}

	public static void notifyBlockedForBlock(ServerPlayer player, BlockState state) {
		ItemStack asStack = new ItemStack(state.getBlock().asItem());
		if (!asStack.isEmpty()) {
			notifyBlocked(player, asStack);
			return;
		}
		Component msg;
		if (StageDisclosure.mayShowRestrictingStageName(player)) {
			StageCatalog cat = StageRegistry.getCatalog();
			var blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
			Optional<StageDefinition> need = blockId == null
				? Optional.empty()
				: StageAccess.primaryRestrictingStageForAbstractId(cat, PlayerStageStore.get(player), blockId);
			String name = need.map(StageDefinition::effectiveDisplayName).orElse("?");
			msg = Component.translatable("message.stagesqx.block_locked_active_stage", name).withStyle(ChatFormatting.RED);
		} else {
			msg = Component.translatable("message.stagesqx.block_locked_generic").withStyle(ChatFormatting.RED);
		}
		player.sendSystemMessage(Component.translatable("message.stagesqx.prefix").append(msg));
		LockSounds.tryPlayFor(player);
	}

	public static void notifyBlocked(ServerPlayer player, ItemStack stack) {
		Component msg;
		if (StageDisclosure.mayShowRestrictingStageName(player)) {
			StageCatalog cat = StageRegistry.getCatalog();
			Optional<StageDefinition> need = StageAccess.primaryRestrictingStage(cat, PlayerStageStore.get(player), stack);
			String name = need.map(StageDefinition::effectiveDisplayName).orElse("?");
			msg = Component.translatable("message.stagesqx.item_locked_active_stage", name).withStyle(ChatFormatting.RED);
		} else {
			msg = Component.translatable("message.stagesqx.item_locked_generic").withStyle(ChatFormatting.RED);
		}
		player.sendSystemMessage(Component.translatable("message.stagesqx.prefix").append(msg));
		LockSounds.tryPlayFor(player);
	}

	public static void notifyBlockedFluid(ServerPlayer player, net.minecraft.resources.ResourceLocation fluidId) {
		Component msg = StageDisclosure.mayShowRestrictingStageName(player)
			? Component.translatable("message.stagesqx.fluid_blocked", fluidId.toString()).withStyle(ChatFormatting.RED)
			: Component.translatable("message.stagesqx.fluid_blocked_generic").withStyle(ChatFormatting.RED);
		player.sendSystemMessage(Component.translatable("message.stagesqx.prefix").append(msg));
		LockSounds.tryPlayFor(player);
	}

	public static void notifyBlockedDimension(ServerPlayer player, net.minecraft.resources.ResourceLocation dimId) {
		Component msg = StageDisclosure.mayShowRestrictingStageName(player)
			? Component.translatable("message.stagesqx.dimension_blocked", dimId.toString()).withStyle(ChatFormatting.RED)
			: Component.translatable("message.stagesqx.dimension_blocked_generic").withStyle(ChatFormatting.RED);
		player.sendSystemMessage(Component.translatable("message.stagesqx.prefix").append(msg));
		LockSounds.tryPlayFor(player);
	}
}
