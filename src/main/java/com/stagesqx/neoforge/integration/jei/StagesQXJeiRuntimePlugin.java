package com.stagesqx.neoforge.integration.jei;

import com.stagesqx.StagesQXConstants;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@JeiPlugin
public final class StagesQXJeiRuntimePlugin implements IModPlugin {
	private static final Logger LOGGER = LoggerFactory.getLogger("stagesqx/jei");

	@Override
	public ResourceLocation getPluginUid() {
		return StagesQXConstants.id("jei");
	}

	@Override
	public void onRuntimeAvailable(IJeiRuntime runtime) {
		JeiStagesSupport.setRuntime(runtime);
		JeiStagesSupport.refresh();
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

	@Override
	public void onRuntimeUnavailable() {
		JeiStagesSupport.setRuntime(null);
	}
}
