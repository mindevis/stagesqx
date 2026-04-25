package com.stagesqx.neoforge;

import com.stagesqx.stage.StageRegistry;
import net.minecraft.server.MinecraftServer;

public final class StageTemplateFactory {
	private StageTemplateFactory() {
	}

	public static StageRegistry.TemplateContent collect(MinecraftServer server) {
		java.util.ArrayList<String> mods = new java.util.ArrayList<>();
		for (var mod : net.neoforged.fml.ModList.get().getMods()) {
			mods.add(mod.getModId());
		}
		mods.sort(String.CASE_INSENSITIVE_ORDER);

		java.util.ArrayList<net.minecraft.resources.ResourceLocation> items = new java.util.ArrayList<>();
		for (var key : net.minecraft.core.registries.BuiltInRegistries.ITEM.registryKeySet()) {
			items.add(key.location());
		}
		items.sort(java.util.Comparator.comparing(net.minecraft.resources.ResourceLocation::toString));

		java.util.ArrayList<net.minecraft.resources.ResourceLocation> fluids = new java.util.ArrayList<>();
		for (var key : net.minecraft.core.registries.BuiltInRegistries.FLUID.registryKeySet()) {
			fluids.add(key.location());
		}
		fluids.sort(java.util.Comparator.comparing(net.minecraft.resources.ResourceLocation::toString));

		java.util.ArrayList<net.minecraft.resources.ResourceLocation> dims = new java.util.ArrayList<>();
		for (var levelKey : server.levelKeys()) {
			dims.add(levelKey.location());
		}
		dims.sort(java.util.Comparator.comparing(net.minecraft.resources.ResourceLocation::toString));

		java.util.ArrayList<net.minecraft.resources.ResourceLocation> entities = new java.util.ArrayList<>();
		for (var key : net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.registryKeySet()) {
			entities.add(key.location());
		}
		entities.sort(java.util.Comparator.comparing(net.minecraft.resources.ResourceLocation::toString));

		return new StageRegistry.TemplateContent(mods, items, fluids, dims, entities);
	}
}
