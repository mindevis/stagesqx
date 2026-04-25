package com.stagesqx.neoforge.integration.ftbquests;

import com.stagesqx.neoforge.ClientStageData;
import com.stagesqx.neoforge.PlayerStageStore;
import com.stagesqx.neoforge.StagesQXModConfig;
import net.neoforged.fml.loading.FMLEnvironment;

import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;

/**
 * Stage checks for the optional FTB Quests integration.
 * <p>
 * This class must remain safe to load even when FTB mods are absent: use reflection for FTB APIs.
 */
public final class StagesQXFtbQuestsStageGate {
	private StagesQXFtbQuestsStageGate() {
	}

	public static boolean requiredStageMetForVisibility(String requiredStageId) {
		if (requiredStageId == null || requiredStageId.isBlank()) {
			return true;
		}
		if (StagesQXModConfig.FTBQUESTS_TEAM_MODE.get()) {
			return hasTeamStageForLocalPlayer(requiredStageId.trim());
		}
		return ClientStageData.getOwnedStages().contains(requiredStageId.trim());
	}

	public static boolean requiredStageMetForServerLogic(String requiredStageId) {
		if (requiredStageId == null || requiredStageId.isBlank()) {
			return true;
		}
		String stage = requiredStageId.trim();
		if (StagesQXModConfig.FTBQUESTS_TEAM_MODE.get()) {
			return hasTeamStageForCurrentServerPlayer(stage);
		}
		ServerPlayer sp = currentFtbQuestsServerPlayer();
		return sp != null && PlayerStageStore.get(sp).contains(stage);
	}

	private static boolean hasTeamStageForCurrentServerPlayer(String stage) {
		ServerPlayer sp = currentFtbQuestsServerPlayer();
		if (sp == null) {
			return false;
		}
		return hasTeamStageForPlayer(sp, stage);
	}

	private static boolean hasTeamStageForLocalPlayer(String stage) {
		if (!FMLEnvironment.dist.isClient()) {
			return false;
		}
		Player p = currentClientPlayer();
		if (p == null) {
			return false;
		}
		return hasTeamStageForPlayer(p, stage);
	}

	private static boolean hasTeamStageForPlayer(Player player, String stage) {
		try {
			Class<?> helper = Class.forName("dev.ftb.mods.ftbteams.api.TeamStagesHelper");
			return (boolean) helper.getMethod("hasTeamStage", Player.class, String.class).invoke(null, player, stage);
		} catch (Throwable ignored) {
			return false;
		}
	}

	private static ServerPlayer currentFtbQuestsServerPlayer() {
		try {
			Class<?> serverQuestFile = Class.forName("dev.ftb.mods.ftbquests.quest.ServerQuestFile");
			Object inst = serverQuestFile.getMethod("getInstance").invoke(null);
			Object p = serverQuestFile.getMethod("getCurrentPlayer").invoke(inst);
			return p instanceof ServerPlayer sp ? sp : null;
		} catch (Throwable ignored) {
			return null;
		}
	}

	private static Player currentClientPlayer() {
		try {
			Class<?> mc = Class.forName("net.minecraft.client.Minecraft");
			Object inst = mc.getMethod("getInstance").invoke(null);
			Object p = mc.getField("player").get(inst);
			return p instanceof Player pl ? pl : null;
		} catch (Throwable ignored) {
			return null;
		}
	}
}

