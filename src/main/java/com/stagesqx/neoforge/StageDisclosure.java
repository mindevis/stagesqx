package com.stagesqx.neoforge;

import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;

/**
 * Whether restricting stage display names may be shown to a player (operators only when configured).
 */
public final class StageDisclosure {
	private StageDisclosure() {
	}

	public static boolean mayShowRestrictingStageName(ServerPlayer player) {
		if (!StagesQXModConfig.REVEAL_STAGE_NAMES_ONLY_TO_OPERATORS.get()) {
			return true;
		}
		return player.hasPermissions(2);
	}

	public static boolean mayShowRestrictingStageNameClient() {
		if (!ClientStageData.hideStageNamesFromNonOps()) {
			return true;
		}
		var p = Minecraft.getInstance().player;
		return p != null && p.hasPermissions(2);
	}
}
