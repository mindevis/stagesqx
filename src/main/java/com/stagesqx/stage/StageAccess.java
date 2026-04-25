package com.stagesqx.stage;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public final class StageAccess {
	private StageAccess() {
	}

	public static boolean bypassesLocks(Player player) {
		return player == null || player.isCreative() || player.isSpectator();
	}

	/**
	 * Stages that satisfy {@code [locks]} requirements for a player: owned stages plus any prerequisites implied
	 * by {@link StageDefinition#dependency()}.
	 * <p>
	 * Packs often grant only the leaf stage (e.g. {@code stage_2}) while modelling progression via dependencies;
	 * treating prerequisites as satisfied keeps gating consistent with that intent.
	 */
	public static Set<String> effectiveAccessStages(StageCatalog catalog, Set<String> playerStagesOwned) {
		if (playerStagesOwned.isEmpty()) {
			return Set.of();
		}
		if (catalog.isEmpty()) {
			return Set.copyOf(playerStagesOwned);
		}
		Set<String> acc = new HashSet<>(playerStagesOwned);
		for (String s : playerStagesOwned) {
			acc.addAll(transitiveDependencyIds(catalog, s));
		}
		return Set.copyOf(acc);
	}

	/**
	 * All direct and indirect {@link StageDefinition#dependency()} ids for {@code stageId} (not including {@code stageId}).
	 */
	public static Set<String> transitiveDependencyIds(StageCatalog catalog, String stageId) {
		Set<String> seen = new HashSet<>();
		ArrayDeque<String> q = new ArrayDeque<>();
		StageDefinition root = catalog.get(stageId);
		if (root == null) {
			return Set.of();
		}
		for (String d : root.dependency()) {
			if (d == null) {
				continue;
			}
			String t = d.trim();
			if (!t.isEmpty()) {
				q.add(t);
			}
		}
		while (!q.isEmpty()) {
			String x = q.poll();
			if (!seen.add(x)) {
				continue;
			}
			StageDefinition dx = catalog.get(x);
			if (dx == null) {
				continue;
			}
			for (String d : dx.dependency()) {
				if (d == null) {
					continue;
				}
				String tt = d.trim();
				if (!tt.isEmpty()) {
					q.add(tt);
				}
			}
		}
		return Set.copyOf(seen);
	}

	/**
	 * Stages whose {@code [locks]} apply (legacy semantics): owned stages minus any that are a (transitive) prerequisite of another owned stage.
	 * <p>
	 * Kept for compatibility with older integrations/debug. Do not use for progression-style "require stage to unlock".
	 */
	public static Set<String> effectiveLockStages(StageCatalog catalog, Set<String> playerStagesOwned) {
		if (playerStagesOwned.isEmpty()) {
			return Set.of();
		}
		if (catalog.isEmpty()) {
			return Set.copyOf(playerStagesOwned);
		}
		Set<String> superseded = new HashSet<>();
		for (String s : playerStagesOwned) {
			for (String t : playerStagesOwned) {
				if (t.equals(s)) {
					continue;
				}
				if (transitiveDependencyIds(catalog, t).contains(s)) {
					superseded.add(s);
					break;
				}
			}
		}
		Set<String> eff = new HashSet<>(playerStagesOwned);
		eff.removeAll(superseded);
		return eff.isEmpty() ? Set.copyOf(playerStagesOwned) : Set.copyOf(eff);
	}

	/**
	 * Lock stages that apply when checking {@code minecraft:} registry ids (items, fluids, dimensions, entities).
	 * <p>
	 * Child stages replace parent {@code [locks]} for mod content, but {@code minecraft = true} on a prerequisite
	 * must still gate vanilla namespace — otherwise water, pigments shown as vanilla-linked stacks, etc. leak
	 * through after granting a dependent stage.
	 */
	public static Set<String> effectiveLockStagesForVanillaNamespace(
		StageCatalog catalog,
		Set<String> playerStagesOwned
	) {
		Set<String> eff = effectiveLockStages(catalog, playerStagesOwned);
		if (eff.isEmpty() || playerStagesOwned.isEmpty()) {
			return eff;
		}
		Set<String> active = new HashSet<>(eff);
		for (String t : eff) {
			for (String s : transitiveDependencyIds(catalog, t)) {
				if (!playerStagesOwned.contains(s)) {
					continue;
				}
				StageDefinition d = catalog.get(s);
				if (d != null && d.minecraft()) {
					active.add(s);
				}
			}
		}
		return Set.copyOf(active);
	}

	/**
	 * Which of the player's stages count as "holding" locks for this resource namespace: leaf stages for mods,
	 * plus inherited prerequisite stages with {@link StageDefinition#minecraft()} for {@code minecraft} namespace.
	 */
	public static Set<String> playerLockStagesForResourceNamespace(
		StageCatalog catalog,
		Set<String> playerStagesOwned,
		String resourceNamespace
	) {
		if (resourceNamespace != null && "minecraft".equals(resourceNamespace)) {
			return effectiveLockStagesForVanillaNamespace(catalog, playerStagesOwned);
		}
		return effectiveLockStages(catalog, playerStagesOwned);
	}

	/**
	 * Which stages count as satisfying {@code [locks]} requirements for this resource namespace.
	 * <p>
	 * Currently identical for all namespaces, but kept as a single chokepoint for any future namespace-specific rules.
	 */
	public static Set<String> playerAccessStagesForResourceNamespace(
		StageCatalog catalog,
		Set<String> playerStagesOwned,
		String resourceNamespace
	) {
		return effectiveAccessStages(catalog, playerStagesOwned);
	}

	private static boolean blockedByMissingRequiredStages(Set<String> gatingStages, Set<String> effectiveAccessStages) {
		return !gatingStages.isEmpty() && !effectiveAccessStages.containsAll(gatingStages);
	}

	public static boolean isItemBlocked(StageCatalog catalog, Set<String> playerStagesOwned, ItemStack stack) {
		if (stack.isEmpty()) {
			return false;
		}
		ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
		Set<String> accessStages = playerAccessStagesForResourceNamespace(
			catalog,
			playerStagesOwned,
			itemId.getNamespace()
		);
		return isItemBlockedForEffectiveStages(catalog, accessStages, stack);
	}

	/**
	 * JEI refresh: pass a precomputed {@link #effectiveAccessStages} to avoid recomputing per item.
	 */
	public static boolean isItemBlockedForEffectiveStages(StageCatalog catalog, Set<String> effectiveAccessStages, ItemStack stack) {
		if (stack.isEmpty()) {
			return false;
		}
		ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
		Set<String> gating = catalog.effectiveStagesGatingItem(itemId, itemId.getNamespace());
		return blockedByMissingRequiredStages(gating, effectiveAccessStages);
	}

	/**
	 * First missing stage to show in tooltip/chat (catalog order) when an item is blocked because
	 * the player does not satisfy all required stages.
	 */
	public static Optional<StageDefinition> primaryRestrictingStage(StageCatalog catalog, Set<String> playerStagesOwned, ItemStack stack) {
		if (stack.isEmpty()) {
			return Optional.empty();
		}
		ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
		Set<String> access = playerAccessStagesForResourceNamespace(catalog, playerStagesOwned, itemId.getNamespace());
		Set<String> gating = catalog.effectiveStagesGatingItem(itemId, itemId.getNamespace());
		if (gating.isEmpty()) {
			return Optional.empty();
		}
		for (String sid : catalog.orderedStageIds()) {
			if (gating.contains(sid) && !access.contains(sid)) {
				StageDefinition def = catalog.get(sid);
				return Optional.of(def != null ? def : StageDefinition.empty(sid));
			}
		}
		return Optional.empty();
	}

	/**
	 * Like {@link #isAbstractIngredientBlocked}: item + fluid gates for messaging when a block id is locked.
	 */
	public static Optional<StageDefinition> primaryRestrictingStageForAbstractId(
		StageCatalog catalog,
		Set<String> playerStagesOwned,
		ResourceLocation id
	) {
		if (id == null) {
			return Optional.empty();
		}
		Set<String> access = playerAccessStagesForResourceNamespace(catalog, playerStagesOwned, id.getNamespace());
		Set<String> gating = new HashSet<>();
		gating.addAll(catalog.effectiveStagesGatingFluid(id));
		gating.addAll(catalog.effectiveStagesGatingItem(id, id.getNamespace()));
		if (gating.isEmpty()) {
			return Optional.empty();
		}
		for (String sid : catalog.orderedStageIds()) {
			if (gating.contains(sid) && !access.contains(sid)) {
				StageDefinition def = catalog.get(sid);
				return Optional.of(def != null ? def : StageDefinition.empty(sid));
			}
		}
		return Optional.empty();
	}

	/**
	 * Block interaction (e.g. open GUI) with empty hands: same gates as {@link #isAbstractIngredientBlocked} for the block id.
	 * <p>
	 * Visual Workbench replaces {@code minecraft:crafting_table} with its own registry id; {@code convertToVanillaBlock}
	 * maps back so pack locks on the vanilla id still apply (optional reflection, no hard dependency).
	 */
	public static boolean isBlockBlocked(StageCatalog catalog, Set<String> playerStagesOwned, BlockState state) {
		if (state == null || state.isAir()) {
			return false;
		}
		ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
		if (id == null) {
			return false;
		}
		if (isAbstractIngredientBlocked(catalog, playerStagesOwned, id)) {
			return true;
		}
		BlockState vanillaSubstitute = visualWorkbenchVanillaSubstitute(state);
		if (vanillaSubstitute == null || vanillaSubstitute.getBlock() == state.getBlock()) {
			return false;
		}
		ResourceLocation vanillaId = BuiltInRegistries.BLOCK.getKey(vanillaSubstitute.getBlock());
		if (vanillaId == null || vanillaId.equals(id)) {
			return false;
		}
		return isAbstractIngredientBlocked(catalog, playerStagesOwned, vanillaId);
	}

	private static BlockState visualWorkbenchVanillaSubstitute(BlockState state) {
		try {
			Class<?> handler = Class.forName("fuzs.visualworkbench.handler.BlockConversionHandler");
			Object out = handler.getMethod("convertToVanillaBlock", BlockState.class).invoke(null, state);
			return out instanceof BlockState bs ? bs : null;
		} catch (Throwable ignored) {
			return null;
		}
	}

	/**
	 * EMI/JEI entries that are not a vanilla item stack (custom stack keys, fluids): combine fluid gates,
	 * explicit item-id gates, and mod-namespace gates for {@code id}.
	 */
	public static boolean isAbstractIngredientBlocked(StageCatalog catalog, Set<String> playerStagesOwned, ResourceLocation id) {
		if (id == null) {
			return false;
		}
		return isAbstractIngredientBlockedForEffectiveStages(
			catalog,
			playerAccessStagesForResourceNamespace(catalog, playerStagesOwned, id.getNamespace()),
			id
		);
	}

	public static boolean isAbstractIngredientBlockedForEffectiveStages(
		StageCatalog catalog,
		Set<String> effectiveAccessStages,
		ResourceLocation id
	) {
		if (id == null) {
			return false;
		}
		Set<String> gating = new HashSet<>();
		gating.addAll(catalog.effectiveStagesGatingFluid(id));
		gating.addAll(catalog.effectiveStagesGatingItem(id, id.getNamespace()));
		return blockedByMissingRequiredStages(gating, effectiveAccessStages);
	}

	/**
	 * True when one or more stages require this mod namespace (or {@code minecraft} via {@code minecraft=true})
	 * and the player is missing at least one required stage.
	 */
	public static boolean isModIdBlocked(StageCatalog catalog, Set<String> playerStagesOwned, String modId) {
		return isModIdBlockedForEffectiveStages(catalog, effectiveAccessStages(catalog, playerStagesOwned), modId);
	}

	public static boolean isModIdBlockedForEffectiveStages(StageCatalog catalog, Set<String> effectiveAccessStages, String modId) {
		if (modId == null || modId.isEmpty()) {
			return false;
		}
		Set<String> gating = catalog.effectiveStagesGatingMod(modId);
		return blockedByMissingRequiredStages(gating, effectiveAccessStages);
	}

	/**
	 * Same rules as {@link #isAbstractIngredientBlocked}: fluid id, mod namespace, or matching item gate.
	 */
	public static boolean isFluidBlocked(StageCatalog catalog, Set<String> playerStagesOwned, ResourceLocation fluidId) {
		if (fluidId == null) {
			return false;
		}
		return isFluidBlockedForEffectiveStages(
			catalog,
			playerAccessStagesForResourceNamespace(catalog, playerStagesOwned, fluidId.getNamespace()),
			fluidId
		);
	}

	public static boolean isFluidBlockedForEffectiveStages(StageCatalog catalog, Set<String> effectiveAccessStages, ResourceLocation fluidId) {
		if (fluidId == null) {
			return false;
		}
		Set<String> gating = new HashSet<>();
		gating.addAll(catalog.effectiveStagesGatingFluid(fluidId));
		gating.addAll(catalog.effectiveStagesGatingItem(fluidId, fluidId.getNamespace()));
		return blockedByMissingRequiredStages(gating, effectiveAccessStages);
	}

	public static boolean isDimensionBlocked(StageCatalog catalog, Set<String> playerStagesOwned, ResourceLocation dimensionId) {
		if (dimensionId == null) {
			return false;
		}
		return isDimensionBlockedForEffectiveStages(
			catalog,
			playerAccessStagesForResourceNamespace(catalog, playerStagesOwned, dimensionId.getNamespace()),
			dimensionId
		);
	}

	public static boolean isDimensionBlockedForEffectiveStages(
		StageCatalog catalog,
		Set<String> effectiveAccessStages,
		ResourceLocation dimensionId
	) {
		Set<String> gating = catalog.effectiveStagesGatingDimension(dimensionId);
		return blockedByMissingRequiredStages(gating, effectiveAccessStages);
	}

	public static boolean isEntityBlocked(StageCatalog catalog, Set<String> playerStagesOwned, ResourceLocation entityTypeId) {
		if (entityTypeId == null) {
			return false;
		}
		return isEntityBlockedForEffectiveStages(
			catalog,
			playerAccessStagesForResourceNamespace(catalog, playerStagesOwned, entityTypeId.getNamespace()),
			entityTypeId
		);
	}

	public static boolean isEntityBlockedForEffectiveStages(
		StageCatalog catalog,
		Set<String> effectiveAccessStages,
		ResourceLocation entityTypeId
	) {
		Set<String> gating = catalog.effectiveStagesGatingEntity(entityTypeId);
		return blockedByMissingRequiredStages(gating, effectiveAccessStages);
	}
}
