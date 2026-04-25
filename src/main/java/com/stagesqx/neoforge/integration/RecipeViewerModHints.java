package com.stagesqx.neoforge.integration;

import com.stagesqx.stage.StageAccess;
import com.stagesqx.stage.StageCatalog;
import dev.emi.emi.api.stack.EmiStack;
import net.neoforged.fml.ModList;

import java.util.Optional;
import java.util.Set;

/**
 * Resolves owning mod id for recipe viewer stacks when registry ids point at vanilla (e.g. XP from library mods).
 */
public final class RecipeViewerModHints {
	private RecipeViewerModHints() {
	}

	public static Optional<String> owningModIdForClass(Class<?> clazz) {
		if (clazz == null) {
			return Optional.empty();
		}
		Module module = clazz.getModule();
		if (module != null && module.isNamed()) {
			String name = module.getName();
			if (name != null && !name.isEmpty() && ModList.get().isLoaded(name)) {
				return Optional.of(name);
			}
		}
		return Optional.empty();
	}

	public static boolean isEmiStackFromBlockedMod(EmiStack stack, StageCatalog cat, Set<String> owned) {
		if (owningModIdForClass(stack.getClass()).filter(mid -> StageAccess.isModIdBlocked(cat, owned, mid)).isPresent()) {
			return true;
		}
		Object key = stack.getKey();
		return key != null
			&& owningModIdForClass(key.getClass()).filter(mid -> StageAccess.isModIdBlocked(cat, owned, mid)).isPresent();
	}
}
