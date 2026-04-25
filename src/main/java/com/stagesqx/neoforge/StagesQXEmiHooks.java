package com.stagesqx.neoforge;

import com.stagesqx.neoforge.integration.emi.EmiStagesDebug;
import net.minecraft.client.Minecraft;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * EMI reload so the index re-evaluates stack visibility after stage sync.
 */
public final class StagesQXEmiHooks {
	private static final Logger LOGGER = LoggerFactory.getLogger("stagesqx/emi");
	private static final int EMI_DEBUG_POLL_CAP = 400;

	private StagesQXEmiHooks() {
	}

	public static void refreshHidden() {
		if (!ModList.get().isLoaded("emi")) {
			return;
		}
		try {
			Class.forName("dev.emi.emi.runtime.EmiReloadManager")
				.getMethod("reload")
				.invoke(null);
		} catch (Throwable t) {
			LOGGER.debug("EMI reload skipped: {}", t.toString());
		}
		scheduleEmiDebugWhenReady();
	}

	private static void scheduleEmiDebugWhenReady() {
		if (!StagesQXModConfig.DEBUG.get()) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		if (mc == null) {
			return;
		}
		mc.execute(() -> pollEmiDebugSnapshot(mc, 0));
	}

	private static void pollEmiDebugSnapshot(Minecraft mc, int attempt) {
		try {
			Class<?> erm = Class.forName("dev.emi.emi.runtime.EmiReloadManager");
			boolean loaded = (Boolean) erm.getMethod("isLoaded").invoke(null);
			if (!loaded && attempt < EMI_DEBUG_POLL_CAP) {
				mc.execute(() -> pollEmiDebugSnapshot(mc, attempt + 1));
				return;
			}
			EmiStagesDebug.maybeLogSnapshot();
		} catch (Throwable t) {
			LOGGER.debug("EMI debug snapshot skipped: {}", t.toString());
		}
	}
}
