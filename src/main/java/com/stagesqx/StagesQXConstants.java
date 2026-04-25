package com.stagesqx;

import net.minecraft.resources.ResourceLocation;

public final class StagesQXConstants {
	public static final String MOD_ID = "stagesqx";

	private StagesQXConstants() {
	}

	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}
}
