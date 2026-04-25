package com.stagesqx.stage;

import net.minecraft.resources.ResourceLocation;

import java.util.Set;

/**
 * Resource lists for stage locks or unlocks (same shape in TOML).
 */
public record StageGateLists(
	Set<ResourceLocation> items,
	Set<String> mods,
	Set<ResourceLocation> fluids,
	Set<ResourceLocation> dimensions,
	Set<ResourceLocation> entities
) {
	public StageGateLists {
		items = Set.copyOf(items);
		mods = Set.copyOf(mods);
		fluids = Set.copyOf(fluids);
		dimensions = Set.copyOf(dimensions);
		entities = Set.copyOf(entities);
	}

	public static StageGateLists empty() {
		return new StageGateLists(Set.of(), Set.of(), Set.of(), Set.of(), Set.of());
	}
}
