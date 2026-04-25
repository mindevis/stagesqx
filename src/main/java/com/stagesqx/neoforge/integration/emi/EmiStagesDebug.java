package com.stagesqx.neoforge.integration.emi;

import com.stagesqx.neoforge.ClientStageData;
import com.stagesqx.neoforge.StagesQXModConfig;
import com.stagesqx.neoforge.integration.RecipeViewerDebugExplain;
import com.stagesqx.neoforge.integration.RecipeViewerModHints;
import com.stagesqx.stage.StageAccess;
import com.stagesqx.stage.StageCatalog;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.registry.EmiStackList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * When {@link StagesQXModConfig#DEBUG} is true, logs EMI index entries under stage-locked namespaces that stay
 * visible (not hidden by {@link StagesQXEmiPlugin}), with the same gating explanations as JEI debug.
 */
public final class EmiStagesDebug {
	private static final Logger EMI_DEBUG_LOG = LoggerFactory.getLogger("stagesqx/emi/debug");

	private EmiStagesDebug() {
	}

	public static void maybeLogSnapshot() {
		if (!StagesQXModConfig.DEBUG.get()) {
			return;
		}
		StageCatalog cat = ClientStageData.getCatalog();
		Set<String> owned = ClientStageData.getOwnedStages();
		Set<String> lockStagesLeaf = StageAccess.effectiveAccessStages(cat, owned);
		Set<String> lockStagesVanilla = lockStagesLeaf;
		int max = StagesQXModConfig.DEBUG_EMI_MAX_LOG_LINES.get();
		boolean logVanilla = StagesQXModConfig.DEBUG_EMI_LOG_VANILLA.get();
		Set<String> lockedNs = cat.namespacesUnderStageModLocks();
		List<EmiStack> visible = EmiStackList.filteredStacks;
		EMI_DEBUG_LOG.info(
			"EMI debug snapshot: catalogEmpty={} ownedStages={} effectiveAccessStages={} namespacesUnderStageModLocks(count)={} filteredStacksSize={} maxLines={} logVanilla={}",
			cat.isEmpty(),
			owned,
			lockStagesLeaf,
			lockedNs.size(),
			visible != null ? visible.size() : -1,
			max,
			logVanilla
		);
		if (max <= 0 || cat.isEmpty() || visible == null || visible.isEmpty()) {
			return;
		}
		Set<String> dedup = new HashSet<>();
		int logged = 0;
		outer:
		for (EmiStack top : visible) {
			if (logged >= max) {
				break;
			}
			if (top == null || top.isEmpty()) {
				continue;
			}
			List<EmiStack> variants = top.getEmiStacks();
			for (EmiStack sub : variants) {
				if (sub != top) {
					logged = logFlatIfNeeded(sub, cat, owned, lockedNs, logVanilla, dedup, logged, max);
					if (logged >= max) {
						break outer;
					}
				}
			}
			logged = logFlatIfNeeded(top, cat, owned, lockedNs, logVanilla, dedup, logged, max);
		}
		if (logged >= max) {
			EMI_DEBUG_LOG.info("EMI debug: hit debug_emi_max_log_lines={} (more entries omitted)", max);
		}
	}

	private static int logFlatIfNeeded(
		EmiStack stack,
		StageCatalog cat,
		Set<String> owned,
		Set<String> lockedNs,
		boolean logVanilla,
		Set<String> dedup,
		int logged,
		int max
	) {
		if (logged >= max || stack == null || stack.isEmpty()) {
			return logged;
		}
		String dedupKey = stack.toString();
		if (!dedup.add(dedupKey)) {
			return logged;
		}
		String ns = primaryNamespace(stack);
		boolean blockedMod = RecipeViewerModHints.isEmiStackFromBlockedMod(stack, cat, owned);
		boolean underLockScope = blockedMod || (ns != null && lockedNs.contains(ns));
		if (!underLockScope) {
			return logged;
		}
		if ("minecraft".equals(ns) && !logVanilla && !blockedMod) {
			return logged;
		}
		boolean hookHides = StagesQXEmiPlugin.isFlatHiddenByStages(stack);
		ItemStack item = stack.getItemStack();
		if (!item.isEmpty()) {
			ResourceLocation id = BuiltInRegistries.ITEM.getKey(item.getItem());
			if (id == null) {
				return logged;
			}
			boolean packBlocked = StageAccess.isItemBlocked(cat, owned, item);
			String detail = RecipeViewerDebugExplain.explainItemPackGating(cat, owned, id)
				+ " | emiHookHides=" + hookHides;
			if (packBlocked && hookHides) {
				return logged;
			}
			if (packBlocked && !hookHides) {
				EMI_DEBUG_LOG.warn("[emi/leak] item pack-blocked but EMI hook shows flat stack: {} | {}", id, detail);
			} else if (!packBlocked) {
				EMI_DEBUG_LOG.info("[emi/visible] item not pack-blocked (under lock scope): {} | {}", id, detail);
			}
			return logged + 1;
		}
		ResourceLocation stackId = stack.getId();
		if (stackId != null && BuiltInRegistries.FLUID.containsKey(stackId)) {
			int n = logFluidLine(cat, owned, stackId, hookHides, logged, max);
			if (n > logged) {
				return n;
			}
		}
		Object rawKey = stack.getKey();
		if (rawKey instanceof FluidStack nfs && !nfs.isEmpty()) {
			ResourceLocation fid = BuiltInRegistries.FLUID.getKey(nfs.getFluid());
			if (fid != null) {
				int n = logFluidLine(cat, owned, fid, hookHides, logged, max);
				if (n > logged) {
					return n;
				}
			}
		}
		if (rawKey instanceof Fluid mf && mf != Fluids.EMPTY) {
			ResourceLocation fid = BuiltInRegistries.FLUID.getKey(mf);
			if (fid != null) {
				int n = logFluidLine(cat, owned, fid, hookHides, logged, max);
				if (n > logged) {
					return n;
				}
			}
		}
		Optional<ResourceLocation> rid = StagesQXEmiPlugin.resourceIdForEmiStack(stack);
		if (rid.isPresent()) {
			ResourceLocation rl = rid.get();
			boolean packBlocked = StageAccess.isAbstractIngredientBlocked(cat, owned, rl);
			String detail = RecipeViewerDebugExplain.explainGenericPackGating(cat, owned, rl, rl.getNamespace())
				+ " | emiHookHides=" + hookHides;
			if (packBlocked && hookHides) {
				return logged;
			}
			if (packBlocked && !hookHides) {
				EMI_DEBUG_LOG.warn("[emi/leak] abstract id pack-blocked but EMI hook shows: {} | {}", rl, detail);
			} else if (!packBlocked) {
				EMI_DEBUG_LOG.info("[emi/visible] abstract id not pack-blocked: {} | {}", rl, detail);
			}
			return logged + 1;
		}
		if (blockedMod && !hookHides) {
			EMI_DEBUG_LOG.info("[emi/visible] stack tied to blocked mod (no registry id): {} | emiHookHides={}", stack, hookHides);
			return logged + 1;
		}
		return logged;
	}

	private static int logFluidLine(
		StageCatalog cat,
		Set<String> owned,
		ResourceLocation fluidRl,
		boolean hookHides,
		int logged,
		int max
	) {
		if (logged >= max) {
			return logged;
		}
		boolean packBlocked = StageAccess.isFluidBlocked(cat, owned, fluidRl);
		String detail = RecipeViewerDebugExplain.explainFluidPackGating(cat, owned, fluidRl)
			+ " | emiHookHides=" + hookHides;
		if (packBlocked && hookHides) {
			return logged;
		}
		if (packBlocked && !hookHides) {
			EMI_DEBUG_LOG.warn("[emi/leak] fluid pack-blocked but EMI hook shows: {} | {}", fluidRl, detail);
		} else if (!packBlocked) {
			EMI_DEBUG_LOG.info("[emi/visible] fluid not pack-blocked: {} | {}", fluidRl, detail);
		}
		return logged + 1;
	}

	private static String primaryNamespace(EmiStack stack) {
		ItemStack item = stack.getItemStack();
		if (!item.isEmpty()) {
			ResourceLocation id = BuiltInRegistries.ITEM.getKey(item.getItem());
			return id != null ? id.getNamespace() : null;
		}
		ResourceLocation id = stack.getId();
		if (id != null && BuiltInRegistries.FLUID.containsKey(id)) {
			return id.getNamespace();
		}
		Object rawKey = stack.getKey();
		if (rawKey instanceof FluidStack nfs && !nfs.isEmpty()) {
			ResourceLocation fid = BuiltInRegistries.FLUID.getKey(nfs.getFluid());
			if (fid != null) {
				return fid.getNamespace();
			}
		}
		if (rawKey instanceof Fluid mf && mf != Fluids.EMPTY) {
			ResourceLocation fid = BuiltInRegistries.FLUID.getKey(mf);
			if (fid != null) {
				return fid.getNamespace();
			}
		}
		return StagesQXEmiPlugin.resourceIdForEmiStack(stack).map(ResourceLocation::getNamespace).orElse(null);
	}
}
