package com.stagesqx.neoforge;

import net.minecraft.client.Minecraft;

/**
 * Called on the client when sync payloads refresh stage data (JEI/EMI refresh hooks).
 */
public final class StagesQXClientHooks {
	private StagesQXClientHooks() {
	}

	public static void onCatalogUpdated() {
		Minecraft mc = Minecraft.getInstance();
		if (mc != null) {
			mc.execute(StagesQXClientHooks::refreshRecipeViewers);
		} else {
			refreshRecipeViewers();
		}
	}

	private static void refreshRecipeViewers() {
		StagesQXEmiHooks.refreshHidden();
		StagesQXJeiHooks.refreshHidden();
	}
}
