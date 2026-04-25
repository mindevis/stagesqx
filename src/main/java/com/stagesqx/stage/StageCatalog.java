package com.stagesqx.stage;

import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Immutable snapshot: stage definitions plus reverse indexes for fast gating checks.
 */
public final class StageCatalog {
	private final Map<String, StageDefinition> byId;
	private final List<String> orderedStageIds;
	private final Map<ResourceLocation, Set<String>> itemToStages;
	private final Map<String, Set<String>> modToStages;
	private final Map<ResourceLocation, Set<String>> fluidToStages;
	private final Map<ResourceLocation, Set<String>> dimensionToStages;
	private final Map<ResourceLocation, Set<String>> entityToStages;

	public StageCatalog(Map<String, StageDefinition> byId) {
		this.byId = Map.copyOf(byId);
		this.orderedStageIds = List.copyOf(byId.keySet());
		this.itemToStages = buildItemIndex(byId);
		this.modToStages = buildModIndex(byId);
		this.fluidToStages = buildFluidIndex(byId);
		this.dimensionToStages = buildDimensionIndex(byId);
		this.entityToStages = buildEntityIndex(byId);
	}

	public static StageCatalog empty() {
		return new StageCatalog(Map.of());
	}

	public Map<String, StageDefinition> stagesById() {
		return Collections.unmodifiableMap(byId);
	}

	/**
	 * Stage ids in load/sync order (stable tie-break for tooltip / restriction messaging).
	 */
	public List<String> orderedStageIds() {
		return orderedStageIds;
	}

	public StageDefinition get(String id) {
		return byId.get(id);
	}

	public boolean isEmpty() {
		return byId.isEmpty();
	}

	/**
	 * Every mod id that appears in any stage's {@code locks.mods} plus {@code minecraft} when any stage has
	 * {@code minecraft = true}. Used for JEI debug: "under lock rules" namespaces.
	 */
	public Set<String> namespacesUnderStageModLocks() {
		return Set.copyOf(modToStages.keySet());
	}

	public Set<String> stagesGatingItem(ResourceLocation itemId, String modNamespace) {
		if (itemId == null || modNamespace == null) {
			return Set.of();
		}
		Set<String> acc = new HashSet<>();
		Set<String> a = itemToStages.get(itemId);
		if (a != null) {
			acc.addAll(a);
		}
		Set<String> b = modToStages.get(modNamespace);
		if (b != null) {
			acc.addAll(b);
		}
		return acc.isEmpty() ? Set.of() : Set.copyOf(acc);
	}

	/**
	 * Stages that require this mod id under {@code locks.mods} (or {@code minecraft = true} for {@code minecraft} namespace).
	 */
	public Set<String> stagesGatingMod(String modId) {
		if (modId == null || modId.isEmpty()) {
			return Set.of();
		}
		Set<String> s = modToStages.get(modId);
		return s == null ? Set.of() : s;
	}

	public Set<String> stagesGatingFluid(ResourceLocation fluidId) {
		if (fluidId == null) {
			return Set.of();
		}
		Set<String> acc = new HashSet<>();
		Set<String> a = fluidToStages.get(fluidId);
		if (a != null) {
			acc.addAll(a);
		}
		Set<String> b = modToStages.get(fluidId.getNamespace());
		if (b != null) {
			acc.addAll(b);
		}
		return acc.isEmpty() ? Set.of() : Set.copyOf(acc);
	}

	public Set<String> stagesGatingDimension(ResourceLocation dimId) {
		if (dimId == null) {
			return Set.of();
		}
		Set<String> acc = new HashSet<>();
		Set<String> a = dimensionToStages.get(dimId);
		if (a != null) {
			acc.addAll(a);
		}
		Set<String> b = modToStages.get(dimId.getNamespace());
		if (b != null) {
			acc.addAll(b);
		}
		return acc.isEmpty() ? Set.of() : Set.copyOf(acc);
	}

	public Set<String> stagesGatingEntity(ResourceLocation entityId) {
		if (entityId == null) {
			return Set.of();
		}
		Set<String> acc = new HashSet<>();
		Set<String> a = entityToStages.get(entityId);
		if (a != null) {
			acc.addAll(a);
		}
		Set<String> b = modToStages.get(entityId.getNamespace());
		if (b != null) {
			acc.addAll(b);
		}
		return acc.isEmpty() ? Set.of() : Set.copyOf(acc);
	}

	public Set<String> effectiveStagesGatingItem(ResourceLocation itemId, String modNamespace) {
		Set<String> gating = new HashSet<>(stagesGatingItem(itemId, modNamespace));
		gating.removeIf(sid -> itemUnlockedByStage(sid, itemId, modNamespace));
		return gating.isEmpty() ? Set.of() : Set.copyOf(gating);
	}

	public Set<String> effectiveStagesGatingFluid(ResourceLocation fluidId) {
		if (fluidId == null) {
			return Set.of();
		}
		String ns = fluidId.getNamespace();
		Set<String> gating = new HashSet<>(stagesGatingFluid(fluidId));
		gating.removeIf(sid -> fluidUnlockedByStage(sid, fluidId, ns));
		return gating.isEmpty() ? Set.of() : Set.copyOf(gating);
	}

	public Set<String> effectiveStagesGatingDimension(ResourceLocation dimId) {
		if (dimId == null) {
			return Set.of();
		}
		String ns = dimId.getNamespace();
		Set<String> gating = new HashSet<>(stagesGatingDimension(dimId));
		gating.removeIf(sid -> dimensionUnlockedByStage(sid, dimId, ns));
		return gating.isEmpty() ? Set.of() : Set.copyOf(gating);
	}

	public Set<String> effectiveStagesGatingEntity(ResourceLocation entityId) {
		if (entityId == null) {
			return Set.of();
		}
		String ns = entityId.getNamespace();
		Set<String> gating = new HashSet<>(stagesGatingEntity(entityId));
		gating.removeIf(sid -> entityUnlockedByStage(sid, entityId, ns));
		return gating.isEmpty() ? Set.of() : Set.copyOf(gating);
	}

	public Set<String> effectiveStagesGatingMod(String modId) {
		if (modId == null || modId.isEmpty()) {
			return Set.of();
		}
		Set<String> gating = new HashSet<>(stagesGatingMod(modId));
		gating.removeIf(sid -> modUnlockedByStage(sid, modId));
		return gating.isEmpty() ? Set.of() : Set.copyOf(gating);
	}

	private boolean itemUnlockedByStage(String stageId, ResourceLocation itemId, String modNamespace) {
		StageDefinition def = byId.get(stageId);
		if (def == null) {
			return false;
		}
		StageGateLists u = def.unlocks();
		return u.items().contains(itemId) || u.mods().contains(modNamespace);
	}

	private boolean fluidUnlockedByStage(String stageId, ResourceLocation fluidId, String namespace) {
		StageDefinition def = byId.get(stageId);
		if (def == null) {
			return false;
		}
		StageGateLists u = def.unlocks();
		return u.fluids().contains(fluidId) || u.mods().contains(namespace);
	}

	private boolean dimensionUnlockedByStage(String stageId, ResourceLocation dimId, String namespace) {
		StageDefinition def = byId.get(stageId);
		if (def == null) {
			return false;
		}
		StageGateLists u = def.unlocks();
		return u.dimensions().contains(dimId) || u.mods().contains(namespace);
	}

	private boolean entityUnlockedByStage(String stageId, ResourceLocation entityId, String namespace) {
		StageDefinition def = byId.get(stageId);
		if (def == null) {
			return false;
		}
		StageGateLists u = def.unlocks();
		return u.entities().contains(entityId) || u.mods().contains(namespace);
	}

	private boolean modUnlockedByStage(String stageId, String modId) {
		StageDefinition def = byId.get(stageId);
		return def != null && def.unlocks().mods().contains(modId);
	}

	private static Map<ResourceLocation, Set<String>> buildItemIndex(Map<String, StageDefinition> byId) {
		Map<ResourceLocation, Set<String>> map = new HashMap<>();
		for (StageDefinition def : byId.values()) {
			for (ResourceLocation rl : def.locks().items()) {
				map.computeIfAbsent(rl, k -> new HashSet<>()).add(def.id());
			}
		}
		return freezeNested(map);
	}

	private static Map<String, Set<String>> buildModIndex(Map<String, StageDefinition> byId) {
		Map<String, Set<String>> map = new HashMap<>();
		for (StageDefinition def : byId.values()) {
			for (String mod : def.locks().mods()) {
				map.computeIfAbsent(mod, k -> new HashSet<>()).add(def.id());
			}
			if (def.minecraft()) {
				map.computeIfAbsent("minecraft", k -> new HashSet<>()).add(def.id());
			}
		}
		return freezeNested(map);
	}

	private static Map<ResourceLocation, Set<String>> buildFluidIndex(Map<String, StageDefinition> byId) {
		Map<ResourceLocation, Set<String>> map = new HashMap<>();
		for (StageDefinition def : byId.values()) {
			for (ResourceLocation rl : def.locks().fluids()) {
				map.computeIfAbsent(rl, k -> new HashSet<>()).add(def.id());
			}
		}
		return freezeNested(map);
	}

	private static Map<ResourceLocation, Set<String>> buildDimensionIndex(Map<String, StageDefinition> byId) {
		Map<ResourceLocation, Set<String>> map = new HashMap<>();
		for (StageDefinition def : byId.values()) {
			for (ResourceLocation rl : def.locks().dimensions()) {
				map.computeIfAbsent(rl, k -> new HashSet<>()).add(def.id());
			}
		}
		return freezeNested(map);
	}

	private static Map<ResourceLocation, Set<String>> buildEntityIndex(Map<String, StageDefinition> byId) {
		Map<ResourceLocation, Set<String>> map = new HashMap<>();
		for (StageDefinition def : byId.values()) {
			for (ResourceLocation rl : def.locks().entities()) {
				map.computeIfAbsent(rl, k -> new HashSet<>()).add(def.id());
			}
		}
		return freezeNested(map);
	}

	private static <K> Map<K, Set<String>> freezeNested(Map<K, Set<String>> map) {
		Map<K, Set<String>> out = new HashMap<>();
		for (Map.Entry<K, Set<String>> e : map.entrySet()) {
			out.put(e.getKey(), Set.copyOf(e.getValue()));
		}
		return Map.copyOf(out);
	}
}
