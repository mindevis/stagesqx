package com.stagesqx.neoforge.integration.emi;

import com.stagesqx.neoforge.ClientStageData;
import com.stagesqx.neoforge.StageClientView;
import com.stagesqx.neoforge.integration.RecipeViewerModHints;
import com.stagesqx.stage.StageAccess;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;

import java.util.List;
import java.util.Optional;

/**
 * Hides EMI sidebar/index stacks that are stage-locked for the local player, including non-item stacks
 * (fluids, mod-specific keys, gases/pigments when exposed as {@link EmiStack} with a registry id) via
 * {@link StageAccess#isAbstractIngredientBlocked}.
 */
@EmiEntrypoint
public final class StagesQXEmiPlugin implements EmiPlugin {

	/** Same predicate as EMI {@code removeEmiStacks} for a single flat stack (no variant expansion). */
	public static boolean isFlatHiddenByStages(EmiStack stack) {
		return shouldHideFlat(stack);
	}

	@Override
	public void register(EmiRegistry registry) {
		registry.removeEmiStacks(StagesQXEmiPlugin::shouldHide);
	}

	private static boolean shouldHide(EmiStack stack) {
		if (stack.isEmpty()) {
			return false;
		}
		List<EmiStack> variants = stack.getEmiStacks();
		for (EmiStack sub : variants) {
			if (sub != stack && shouldHideFlat(sub)) {
				return true;
			}
		}
		return shouldHideFlat(stack);
	}

	private static boolean shouldHideFlat(EmiStack stack) {
		if (stack.isEmpty()) {
			return false;
		}
		ItemStack item = stack.getItemStack();
		if (!item.isEmpty()) {
			if (StageClientView.isBlocked(item)) {
				return true;
			}
			Optional<FluidStack> contained = FluidUtil.getFluidContained(item);
			if (contained.isPresent()) {
				FluidStack fs = contained.get();
				if (!fs.isEmpty()) {
					ResourceLocation fid = BuiltInRegistries.FLUID.getKey(fs.getFluid());
					if (fid != null && StageAccess.isFluidBlocked(
							ClientStageData.getCatalog(),
							ClientStageData.getOwnedStages(),
							fid)) {
						return true;
					}
				}
			}
		}
		ResourceLocation stackId = stack.getId();
		if (stackId != null && BuiltInRegistries.FLUID.containsKey(stackId)) {
			Fluid fl = BuiltInRegistries.FLUID.get(stackId);
			if (fl != null && fl != Fluids.EMPTY
				&& StageAccess.isFluidBlocked(
					ClientStageData.getCatalog(),
					ClientStageData.getOwnedStages(),
					stackId)) {
				return true;
			}
		}
		Object rawKey = stack.getKey();
		if (rawKey instanceof FluidStack nfs && !nfs.isEmpty()) {
			ResourceLocation fid = BuiltInRegistries.FLUID.getKey(nfs.getFluid());
			if (fid != null && StageAccess.isFluidBlocked(
					ClientStageData.getCatalog(),
					ClientStageData.getOwnedStages(),
					fid)) {
				return true;
			}
		}
		if (rawKey instanceof Fluid mf && mf != Fluids.EMPTY) {
			ResourceLocation fid = BuiltInRegistries.FLUID.getKey(mf);
			if (fid != null && StageAccess.isFluidBlocked(
					ClientStageData.getCatalog(),
					ClientStageData.getOwnedStages(),
					fid)) {
				return true;
			}
		}
		if (RecipeViewerModHints.isEmiStackFromBlockedMod(stack, ClientStageData.getCatalog(), ClientStageData.getOwnedStages())) {
			return true;
		}
		Optional<ResourceLocation> rid = resourceIdForEmiStack(stack);
		return rid.filter(rl -> StageAccess.isAbstractIngredientBlocked(
				ClientStageData.getCatalog(),
				ClientStageData.getOwnedStages(),
				rl)).isPresent();
	}

	public static Optional<ResourceLocation> resourceIdForEmiStack(EmiStack stack) {
		ResourceLocation id = stack.getId();
		if (id != null) {
			return Optional.of(id);
		}
		Object key = stack.getKey();
		if (key instanceof ResourceLocation rl) {
			return Optional.of(rl);
		}
		if (key instanceof ResourceKey<?> rk) {
			return Optional.of(rk.location());
		}
		if (key instanceof Holder<?> holder) {
			return holder.unwrapKey().map(ResourceKey::location);
		}
		return Optional.empty();
	}
}
