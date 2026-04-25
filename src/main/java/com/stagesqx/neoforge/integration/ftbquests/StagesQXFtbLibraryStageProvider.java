package com.stagesqx.neoforge.integration.ftbquests;

import com.stagesqx.neoforge.PlayerStageStore;
import com.stagesqx.neoforge.StageNetwork;
import com.stagesqx.neoforge.StagesQXModConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Registers StagesQX as the FTB Library stage provider so FTB Quests can use its native
 * stage UI/requirements (Stage Required field, Stage Task, Stage Reward).
 *
 * <p>Implemented via reflection to keep FTB mods optional at runtime.</p>
 */
public final class StagesQXFtbLibraryStageProvider {
	private static final Logger LOG = LoggerFactory.getLogger("stagesqx/ftbquests/provider");

	private static final String PROVIDER_ID = "StagesQXStageProvider";
	private static volatile boolean registered = false;

	private StagesQXFtbLibraryStageProvider() {
	}

	public static boolean isRegistered() {
		return registered;
	}

	public static void tryRegister() {
		if (registered) {
			return;
		}
		try {
			Class<?> stageHelperClass = Class.forName("dev.ftb.mods.ftblibrary.integration.stages.StageHelper");
			Object stageHelper = stageHelperClass.getField("INSTANCE").get(null);

			Class<?> providerInterface = Class.forName("dev.ftb.mods.ftblibrary.integration.stages.StageProvider");
			Object providerProxy = Proxy.newProxyInstance(
				StagesQXFtbLibraryStageProvider.class.getClassLoader(),
				new Class<?>[]{providerInterface},
				(proxy, method, args) -> handleProviderMethod(method.getName(), args)
			);

			Method setProviderImpl = stageHelper.getClass().getMethod("setProviderImpl", providerInterface);
			setProviderImpl.invoke(stageHelper, providerProxy);

			registered = true;
			LOG.info("FTB Library stage provider registered: {}", PROVIDER_ID);
		} catch (ClassNotFoundException e) {
			// FTB Library not installed -> expected in many packs
			LOG.debug("FTB Library not present; stage provider not registered.");
		} catch (Throwable t) {
			LOG.warn("Failed to register FTB Library stage provider: {}", t.toString());
		}
	}

	private static Object handleProviderMethod(String methodName, Object[] args) {
		try {
			return switch (methodName) {
				case "has" -> handleHas(args);
				case "add" -> {
					handleAdd(args);
					yield null;
				}
				case "remove" -> {
					handleRemove(args);
					yield null;
				}
				case "sync" -> {
					handleSync(args);
					yield null;
				}
				case "getName" -> PROVIDER_ID;
				case "toString" -> PROVIDER_ID;
				case "hashCode" -> System.identityHashCode(StagesQXFtbLibraryStageProvider.class);
				case "equals" -> args != null && args.length == 1 && args[0] == StagesQXFtbLibraryStageProvider.class;
				default -> null;
			};
		} catch (Throwable t) {
			// has() must be conservative for gating; other methods can fail silently
			return "has".equals(methodName) ? false : null;
		}
	}

	private static boolean handleHas(Object[] args) {
		if (args == null || args.length != 2) {
			return false;
		}
		if (!(args[0] instanceof Player player) || !(args[1] instanceof String stage)) {
			return false;
		}
		String s = stage.trim();
		if (s.isEmpty()) {
			return false;
		}

		// Team mode: delegate to FTB Teams stages so Quests share progress as configured.
		if (StagesQXModConfig.FTBQUESTS_TEAM_MODE.get()) {
			return hasTeamStage(player, s);
		}

		// Solo mode: server uses persistent store; client can only answer if Player is a ServerPlayer (usually not).
		if (player instanceof ServerPlayer sp) {
			return PlayerStageStore.get(sp).contains(s);
		}
		// FTB can call has() on client for visibility; StageTasks and Rewards are server-side.
		return false;
	}

	private static void handleAdd(Object[] args) {
		if (args == null || args.length != 2) {
			return;
		}
		if (!(args[0] instanceof ServerPlayer sp) || !(args[1] instanceof String stage)) {
			return;
		}
		String s = stage.trim();
		if (s.isEmpty()) {
			return;
		}
		if (StagesQXModConfig.FTBQUESTS_TEAM_MODE.get()) {
			addTeamStage(sp, s);
			return;
		}
		PlayerStageStore.grant(sp, s);
	}

	private static void handleRemove(Object[] args) {
		if (args == null || args.length != 2) {
			return;
		}
		if (!(args[0] instanceof ServerPlayer sp) || !(args[1] instanceof String stage)) {
			return;
		}
		String s = stage.trim();
		if (s.isEmpty()) {
			return;
		}
		if (StagesQXModConfig.FTBQUESTS_TEAM_MODE.get()) {
			removeTeamStage(sp, s);
			return;
		}
		PlayerStageStore.revoke(sp, s);
	}

	private static void handleSync(Object[] args) {
		if (args == null || args.length != 1) {
			return;
		}
		if (args[0] instanceof ServerPlayer sp) {
			StageNetwork.syncPlayerStages(sp);
		}
	}

	private static boolean hasTeamStage(Player player, String stage) {
		try {
			Class<?> helper = Class.forName("dev.ftb.mods.ftbteams.api.TeamStagesHelper");
			return (boolean) helper.getMethod("hasTeamStage", Player.class, String.class).invoke(null, player, stage);
		} catch (Throwable ignored) {
			return false;
		}
	}

	private static void addTeamStage(ServerPlayer player, String stage) {
		try {
			Class<?> helper = Class.forName("dev.ftb.mods.ftbteams.api.TeamStagesHelper");
			// Most versions: addTeamStage(ServerPlayer, String)
			helper.getMethod("addTeamStage", ServerPlayer.class, String.class).invoke(null, player, stage);
		} catch (Throwable ignored) {
		}
	}

	private static void removeTeamStage(ServerPlayer player, String stage) {
		try {
			Class<?> helper = Class.forName("dev.ftb.mods.ftbteams.api.TeamStagesHelper");
			// Most versions: removeTeamStage(ServerPlayer, String)
			helper.getMethod("removeTeamStage", ServerPlayer.class, String.class).invoke(null, player, stage);
		} catch (Throwable ignored) {
		}
	}
}

