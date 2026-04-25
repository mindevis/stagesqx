package com.stagesqx.neoforge;

import com.stagesqx.neoforge.integration.jei.JeiStagesSupport;
import net.minecraft.client.Minecraft;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Refreshes JEI hidden ingredients when stage data updates (only if JEI is loaded).
 */
public final class StagesQXJeiHooks {
	private static final Logger LOGGER = LoggerFactory.getLogger("stagesqx/jei");

	private StagesQXJeiHooks() {
	}

	public static void refreshHidden() {
		if (!ModList.get().isLoaded("jei")) {
			return;
		}
		try {
			JeiStagesSupport.refresh();
		} catch (Throwable t) {
			LOGGER.warn("JEI stage visibility refresh failed: {}", t.toString());
		}
		Minecraft mc = Minecraft.getInstance();
		if (mc != null) {
			mc.execute(() -> {
				try {
					JeiStagesSupport.refresh();
				} catch (Throwable t) {
					LOGGER.warn("JEI stage visibility refresh (deferred) failed: {}", t.toString());
				}
			});
		}
	}
}
