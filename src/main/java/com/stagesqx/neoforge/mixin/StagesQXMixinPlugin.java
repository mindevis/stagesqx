package com.stagesqx.neoforge.mixin;

import net.neoforged.fml.ModList;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.objectweb.asm.tree.ClassNode;

import java.util.List;
import java.util.Set;

/**
 * Ensures optional integrations' mixins do not load when the target mod is absent.
 * <p>
 * Mixin plugins are instantiated very early; avoid referencing game classes here.
 */
public final class StagesQXMixinPlugin implements IMixinConfigPlugin {
	private static final String FTBQUESTS_MIXIN_PREFIX = "com.stagesqx.neoforge.mixin.ftbquests.";

	private boolean ftbQuestsLoaded;

	@Override
	public void onLoad(String mixinPackage) {
		this.ftbQuestsLoaded = ModList.get().isLoaded("ftbquests");
	}

	@Override
	public String getRefMapperConfig() {
		return null;
	}

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		if (mixinClassName != null && mixinClassName.startsWith(FTBQUESTS_MIXIN_PREFIX)) {
			return ftbQuestsLoaded;
		}
		return true;
	}

	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
	}

	@Override
	public List<String> getMixins() {
		return null;
	}

	@Override
	public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}

	@Override
	public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}
}

