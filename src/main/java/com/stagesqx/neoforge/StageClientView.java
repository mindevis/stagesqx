package com.stagesqx.neoforge;

import com.stagesqx.stage.StageAccess;
import com.stagesqx.stage.StageDefinition;
import net.minecraft.world.item.ItemStack;

/**
 * Client-side lock checks using synced {@link ClientStageData}.
 */
public final class StageClientView {
	private StageClientView() {
	}

	public static boolean isBlocked(ItemStack stack) {
		return StageAccess.isItemBlocked(ClientStageData.getCatalog(), ClientStageData.getOwnedStages(), stack);
	}

	public static String blockingStageName(ItemStack stack) {
		return StageAccess.primaryRestrictingStage(ClientStageData.getCatalog(), ClientStageData.getOwnedStages(), stack)
			.map(StageDefinition::effectiveDisplayName)
			.orElse("?");
	}
}
