package com.stagesqx.neoforge.integration.curios;

import com.stagesqx.neoforge.PlayerStageStore;
import com.stagesqx.neoforge.StageFeedback;
import com.stagesqx.stage.StageAccess;
import com.stagesqx.stage.StageRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.TriState;
import top.theillusivec4.curios.api.event.CurioCanEquipEvent;

/**
 * Optional Curios: loaded via reflection only when the Curios mod is present.
 */
public final class CuriosStages {
	private CuriosStages() {
	}

	public static void register() {
		NeoForge.EVENT_BUS.addListener(CuriosStages::onCurioCanEquip);
	}

	@SubscribeEvent
	public static void onCurioCanEquip(CurioCanEquipEvent event) {
		if (!(event.getSlotContext().entity() instanceof ServerPlayer sp) || sp.level().isClientSide()) {
			return;
		}
		if (StageAccess.bypassesLocks(sp)) {
			return;
		}
		ItemStack stack = event.getStack();
		if (stack.isEmpty()) {
			return;
		}
		if (StageAccess.isItemBlocked(StageRegistry.getCatalog(), PlayerStageStore.get(sp), stack)) {
			event.setEquipResult(TriState.FALSE);
			StageFeedback.notifyBlocked(sp, stack);
		}
	}
}
