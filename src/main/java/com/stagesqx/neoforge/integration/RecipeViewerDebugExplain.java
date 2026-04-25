package com.stagesqx.neoforge.integration;

import com.stagesqx.stage.StageAccess;
import com.stagesqx.stage.StageCatalog;
import net.minecraft.resources.ResourceLocation;

import java.util.HashSet;
import java.util.Set;

/**
 * Shared "why is this blocked / not blocked" strings for JEI and EMI debug logs.
 */
public final class RecipeViewerDebugExplain {
	private RecipeViewerDebugExplain() {
	}

	public static String explainItemPackGating(StageCatalog cat, Set<String> owned, ResourceLocation id) {
		Set<String> access = StageAccess.playerAccessStagesForResourceNamespace(cat, owned, id.getNamespace());
		Set<String> gating = cat.effectiveStagesGatingItem(id, id.getNamespace());
		if (gating.isEmpty()) {
			return "no stage targets item id or namespace in [locks] (item id not in locks.items and mod ns not in locks.mods / minecraft)";
		}
		Set<String> missing = new HashSet<>(gating);
		missing.removeAll(access);
		if (!missing.isEmpty()) {
			return "gatingStages=" + gating + " playerAccessStagesForNs=" + access + " → missingRequiredStages=" + missing;
		}
		return "gatingStages=" + gating + " playerAccessStagesForNs=" + access + " → all requirements satisfied (should be unblocked)";
	}

	public static String explainFluidPackGating(StageCatalog cat, Set<String> owned, ResourceLocation fluidRl) {
		if (fluidRl == null) {
			return "fluid ResourceLocation=null (registry/JEI id unknown)";
		}
		Set<String> access = StageAccess.playerAccessStagesForResourceNamespace(cat, owned, fluidRl.getNamespace());
		Set<String> gating = new HashSet<>();
		gating.addAll(cat.effectiveStagesGatingFluid(fluidRl));
		gating.addAll(cat.effectiveStagesGatingItem(fluidRl, fluidRl.getNamespace()));
		if (gating.isEmpty()) {
			return "no stage targets fluid id or fluid namespace in [locks]";
		}
		Set<String> missing = new HashSet<>(gating);
		missing.removeAll(access);
		if (!missing.isEmpty()) {
			return "gatingStages=" + gating + " playerAccessStagesForNs=" + access + " → missingRequiredStages=" + missing;
		}
		return "gatingStages=" + gating + " playerAccessStagesForNs=" + access + " → all requirements satisfied";
	}

	public static String explainGenericPackGating(StageCatalog cat, Set<String> owned, ResourceLocation rl, String ns) {
		if (rl != null) {
			Set<String> access = StageAccess.playerAccessStagesForResourceNamespace(cat, owned, rl.getNamespace());
			Set<String> gating = new HashSet<>();
			gating.addAll(cat.effectiveStagesGatingFluid(rl));
			gating.addAll(cat.effectiveStagesGatingItem(rl, rl.getNamespace()));
			if (gating.isEmpty()) {
				return "no [locks] for rl=" + rl;
			}
			Set<String> missing = new HashSet<>(gating);
			missing.removeAll(access);
			if (!missing.isEmpty()) {
				return "gating=" + gating + " accessStages=" + access + " → missingRequiredStages=" + missing;
			}
			return "gating=" + gating + " accessStages=" + access + " → all requirements satisfied";
		}
		Set<String> access = StageAccess.playerAccessStagesForResourceNamespace(cat, owned, ns);
		Set<String> gating = cat.effectiveStagesGatingMod(ns);
		if (gating.isEmpty()) {
			return "no mod-level [locks] for ns=" + ns + " (only item-specific?)";
		}
		Set<String> missing = new HashSet<>(gating);
		missing.removeAll(access);
		if (!missing.isEmpty()) {
			return "gatingModsStages=" + gating + " accessStages=" + access + " → missingRequiredStages=" + missing;
		}
		return "gatingModsStages=" + gating + " accessStages=" + access + " → all requirements satisfied";
	}
}
