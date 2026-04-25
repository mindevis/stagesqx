package com.stagesqx.neoforge.integration.jei;

import com.stagesqx.neoforge.ClientStageData;
import com.stagesqx.neoforge.StagesQXModConfig;
import com.stagesqx.neoforge.integration.RecipeViewerDebugExplain;
import com.stagesqx.stage.StageAccess;
import com.stagesqx.stage.StageCatalog;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.api.runtime.IIngredientVisibility;
import mezz.jei.gui.ingredients.IngredientFilter;
import mezz.jei.library.ingredients.IngredientBlacklistInternal;
import mezz.jei.library.ingredients.IngredientVisibility;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.lang.reflect.Field;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class JeiStagesSupport {
	private static final Logger JEI_DEBUG_LOG = LoggerFactory.getLogger("stagesqx/jei/debug");
	/**
	 * JEI fluid UIDs look like {@code fluid:minecraft:water} or {@code fluid:create:potion:…}; other mods embed
	 * {@code namespace:path} in arbitrary unique ids — extract and gate on those namespaces / ids.
	 */
	private static final Pattern JEI_UID_LOCATION_PATTERN = Pattern.compile("([a-z0-9_.-]+):([a-z0-9_./-]+)");

	private static IJeiRuntime runtime;
	private static IIngredientManager.IIngredientListener ingredientListener;
	private static final Set<ResourceLocation> hiddenItemIds = new HashSet<>();
	private static final Set<ResourceLocation> hiddenFluidIds = new HashSet<>();
	private static final Map<String, Set<String>> hiddenGenericIngredientKeys = new HashMap<>();
	private static final Map<String, Map<String, Object>> genericIngredientRecovery = new HashMap<>();
	private static final AtomicBoolean refreshQueued = new AtomicBoolean(false);
	/** JEI {@link IngredientVisibility} field name — drives list visibility; {@code removeIngredientsAtRuntime} does not remove overlay rows (empty {@code onIngredientsRemoved}). */
	private static Field jeiIngredientVisibilityBlacklistField;

	private JeiStagesSupport() {
	}

	public static void setRuntime(IJeiRuntime r) {
		runtime = r;
		hiddenItemIds.clear();
		hiddenFluidIds.clear();
		hiddenGenericIngredientKeys.clear();
		genericIngredientRecovery.clear();
		ingredientListener = null;
		if (r == null) {
			return;
		}
		ingredientListener = new IIngredientManager.IIngredientListener() {
			@Override
			public <V> void onIngredientsAdded(IIngredientHelper<V> helper, Collection<ITypedIngredient<V>> ingredients) {
				scheduleRefresh();
			}

			@Override
			public <V> void onIngredientsRemoved(IIngredientHelper<V> helper, Collection<ITypedIngredient<V>> ingredients) {
				scheduleRefresh();
			}
		};
		r.getIngredientManager().registerIngredientListener(ingredientListener);
	}

	public static void refresh() {
		if (runtime == null) {
			return;
		}
		var im = runtime.getIngredientManager();
		IngredientBlacklistInternal apiBlacklist = jeiApiBlacklist(runtime);
		var cat = ClientStageData.getCatalog();
		var owned = ClientStageData.getOwnedStages();
		Set<String> lockStagesLeaf = StageAccess.effectiveAccessStages(cat, owned);
		Set<String> lockStagesVanilla = lockStagesLeaf;
		refreshItemIngredients(im, apiBlacklist, cat, lockStagesLeaf, lockStagesVanilla);
		refreshAllFluidStackIngredientTypes(im, apiBlacklist, cat, lockStagesLeaf, lockStagesVanilla);
		refreshOtherJeiIngredientTypes(im, apiBlacklist, cat, lockStagesLeaf, lockStagesVanilla);
		notifyJeiIngredientFilter(runtime);
		maybeLogJeiDebugSnapshot(im, cat, owned, lockStagesLeaf, lockStagesVanilla);
	}

	private static IngredientBlacklistInternal jeiApiBlacklist(IJeiRuntime r) {
		IIngredientVisibility vis = r.getJeiHelpers().getIngredientVisibility();
		if (vis instanceof IngredientVisibility iv) {
			try {
				if (jeiIngredientVisibilityBlacklistField == null) {
					jeiIngredientVisibilityBlacklistField = IngredientVisibility.class.getDeclaredField("blacklist");
					jeiIngredientVisibilityBlacklistField.setAccessible(true);
				}
				IngredientBlacklistInternal bl = (IngredientBlacklistInternal) jeiIngredientVisibilityBlacklistField.get(iv);
				if (bl != null) {
					return bl;
				}
			} catch (ReflectiveOperationException ignored) {
			}
		}
		IngredientBlacklistInternal bl = scanBlacklistField(vis);
		if (bl != null) {
			return bl;
		}
		return blacklistFromIngredientFilter(r);
	}

	private static IngredientBlacklistInternal blacklistFromIngredientFilter(IJeiRuntime r) {
		try {
			if (!(r.getIngredientFilter() instanceof IngredientFilter filter)) {
				return null;
			}
			Field f = IngredientFilter.class.getDeclaredField("ingredientVisibility");
			f.setAccessible(true);
			Object v = f.get(filter);
			return scanBlacklistField(v);
		} catch (ReflectiveOperationException ignored) {
			return null;
		}
	}

	/**
	 * Walk the class hierarchy — JEI may use a subclass of {@link IngredientVisibility} that keeps {@code blacklist} on a parent.
	 */
	private static IngredientBlacklistInternal scanBlacklistField(Object vis) {
		if (vis == null) {
			return null;
		}
		for (Class<?> c = vis.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
			for (Field f : c.getDeclaredFields()) {
				if (!IngredientBlacklistInternal.class.isAssignableFrom(f.getType())) {
					continue;
				}
				try {
					f.setAccessible(true);
					Object val = f.get(vis);
					if (val instanceof IngredientBlacklistInternal ibl) {
						return ibl;
					}
				} catch (ReflectiveOperationException ignored) {
				}
			}
		}
		return null;
	}

	/** Access stages used for gating checks; kept split for historical reasons (legacy vanilla namespace quirks). */
	private static Set<String> lockStagesForRegistryNs(
		Set<String> lockStagesLeaf,
		Set<String> lockStagesVanilla,
		String ns
	) {
		return "minecraft".equals(ns) ? lockStagesVanilla : lockStagesLeaf;
	}

	private static void refreshOtherJeiIngredientTypes(
		IIngredientManager im,
		IngredientBlacklistInternal apiBlacklist,
		StageCatalog cat,
		Set<String> lockStagesLeaf,
		Set<String> lockStagesVanilla
	) {
		for (IIngredientType<?> rawType : im.getRegisteredIngredientTypes()) {
			if (rawType == VanillaTypes.ITEM_STACK || isFluidStackIngredientType(rawType)) {
				continue;
			}
			refreshGenericIngredientType(im, apiBlacklist, cat, lockStagesLeaf, lockStagesVanilla, rawType);
		}
	}

	private static boolean isFluidStackIngredientType(IIngredientType<?> rawType) {
		return FluidStack.class.isAssignableFrom(rawType.getIngredientClass());
	}

	private static <V> void refreshGenericIngredientType(
		IIngredientManager im,
		IngredientBlacklistInternal apiBlacklist,
		StageCatalog cat,
		Set<String> lockStagesLeaf,
		Set<String> lockStagesVanilla,
		IIngredientType<V> type
	) {
		IIngredientHelper<V> helper = im.getIngredientHelper(type);
		String typeUid = type.getUid();
		Collection<V> universe = im.getAllIngredients(type);

		Map<String, Object> cache = genericIngredientRecovery.computeIfAbsent(typeUid, k -> new HashMap<>());
		for (V ing : universe) {
			if (!helper.isValidIngredient(ing)) {
				continue;
			}
			String stableKey = genericStableKey(helper, ing);
			if (stableKey != null) {
				cache.put(stableKey, helper.copyIngredient(ing));
			}
		}

		Set<String> prevHidden = new HashSet<>(hiddenGenericIngredientKeys.getOrDefault(typeUid, Collections.emptySet()));

		Set<String> nextHidden = new HashSet<>();
		for (String key : prevHidden) {
			if (genericKeyStillBlocked(key, cat, lockStagesLeaf, lockStagesVanilla)) {
				nextHidden.add(key);
			}
		}
		for (V ing : universe) {
			if (!helper.isValidIngredient(ing)) {
				continue;
			}
			if (isGenericJeiIngredientBlocked(helper, ing, cat, lockStagesLeaf, lockStagesVanilla)) {
				String k = genericStableKey(helper, ing);
				if (k != null) {
					nextHidden.add(k);
				}
			}
		}

		Set<String> toRestore = new HashSet<>(prevHidden);
		toRestore.removeAll(nextHidden);

		List<V> removeList = new ArrayList<>();
		for (V ing : universe) {
			if (!helper.isValidIngredient(ing)) {
				continue;
			}
			if (isGenericJeiIngredientBlocked(helper, ing, cat, lockStagesLeaf, lockStagesVanilla)) {
				removeList.add(helper.copyIngredient(ing));
			}
		}
		if (!removeList.isEmpty()) {
			im.removeIngredientsAtRuntime(type, removeList);
		}

		List<V> showList = new ArrayList<>();
		for (String key : toRestore) {
			Object sample = cache.get(key);
			if (sample != null) {
				V cast = type.getCastIngredient(sample);
				if (cast != null) {
					showList.add(helper.copyIngredient(cast));
				}
			}
		}
		if (!showList.isEmpty()) {
			im.addIngredientsAtRuntime(type, showList);
		}

		universe = im.getAllIngredients(type);
		syncTypedIngredientBlacklist(
			im,
			apiBlacklist,
			type,
			helper,
			universe,
			ing -> isGenericJeiIngredientBlocked(helper, ing, cat, lockStagesLeaf, lockStagesVanilla));

		hiddenGenericIngredientKeys.put(typeUid, nextHidden);
	}

	private static boolean genericKeyStillBlocked(
		String key,
		StageCatalog cat,
		Set<String> lockStagesLeaf,
		Set<String> lockStagesVanilla
	) {
		if (key.startsWith("rl:")) {
			try {
				ResourceLocation rl = ResourceLocation.parse(key.substring(3));
				Set<String> lock = lockStagesForRegistryNs(lockStagesLeaf, lockStagesVanilla, rl.getNamespace());
				return StageAccess.isAbstractIngredientBlockedForEffectiveStages(cat, lock, rl);
			} catch (Throwable t) {
				return false;
			}
		}
		if (key.startsWith("mod:")) {
			int bar = key.indexOf('|');
			if (bar > 4) {
				String modId = key.substring(4, bar);
				Set<String> lock = lockStagesForRegistryNs(lockStagesLeaf, lockStagesVanilla, modId);
				return StageAccess.isModIdBlockedForEffectiveStages(cat, lock, modId);
			}
		}
		return false;
	}

	private static <V> boolean isGenericJeiIngredientBlocked(
		IIngredientHelper<V> helper,
		V ing,
		StageCatalog cat,
		Set<String> lockStagesLeaf,
		Set<String> lockStagesVanilla
	) {
		try {
			String modId = helper.getDisplayModId(ing);
			if (modId != null && !modId.isEmpty()) {
				Set<String> lockForMod = lockStagesForRegistryNs(lockStagesLeaf, lockStagesVanilla, modId);
				if (StageAccess.isModIdBlockedForEffectiveStages(cat, lockForMod, modId)) {
					return true;
				}
			}
		} catch (Throwable ignored) {
		}
		ResourceLocation rl = safeResourceLocation(helper, ing);
		if (rl != null) {
			Set<String> lock = lockStagesForRegistryNs(lockStagesLeaf, lockStagesVanilla, rl.getNamespace());
			if (StageAccess.isAbstractIngredientBlockedForEffectiveStages(cat, lock, rl)) {
				return true;
			}
		}
		return jeiUidDeclaresBlockedContent(helper, ing, cat, lockStagesLeaf, lockStagesVanilla);
	}

	private static <V> String genericStableKey(IIngredientHelper<V> helper, V ing) {
		try {
			String modId = helper.getDisplayModId(ing);
			Object uidObj = helper.getUid(ing, UidContext.Ingredient);
			String uid = uidObj != null ? uidObj.toString() : null;
			if (modId != null && !modId.isEmpty() && !"minecraft".equals(modId) && uid != null && !uid.isEmpty()) {
				return "mod:" + modId + "|" + uid;
			}
		} catch (Throwable ignored) {
		}
		ResourceLocation rl = safeResourceLocation(helper, ing);
		if (rl != null) {
			return "rl:" + rl;
		}
		try {
			String modId = helper.getDisplayModId(ing);
			Object uidObj = helper.getUid(ing, UidContext.Ingredient);
			String uid = uidObj != null ? uidObj.toString() : null;
			if (modId != null && !modId.isEmpty() && uid != null && !uid.isEmpty()) {
				return "mod:" + modId + "|" + uid;
			}
		} catch (Throwable ignored) {
		}
		return null;
	}

	private static <V> ResourceLocation safeResourceLocation(IIngredientHelper<V> helper, V ingredient) {
		try {
			return helper.getResourceLocation(ingredient);
		} catch (Throwable t) {
			return null;
		}
	}

	/**
	 * JEI fluid identity (Create potions, mod elixirs): prefer {@link IIngredientHelper#getResourceLocation} so the id
	 * matches the sidebar; fall back to the fluid registry key.
	 */
	private static ResourceLocation fluidIngredientId(IIngredientHelper<FluidStack> helper, FluidStack fs) {
		if (fs == null || fs.isEmpty()) {
			return null;
		}
		try {
			ResourceLocation rl = helper.getResourceLocation(fs);
			if (rl != null) {
				return rl;
			}
		} catch (Throwable ignored) {
		}
		Fluid fluid = fs.getFluid();
		if (fluid == null || fluid == Fluids.EMPTY) {
			return null;
		}
		return BuiltInRegistries.FLUID.getKey(fluid);
	}

	/**
	 * Registry-based fluid gates use the fluid id namespace; mod-locked packs often show vanilla base fluids with a
	 * different {@link IIngredientHelper#getDisplayModId} (e.g. Create potions as {@code create:…} while
	 * {@link FluidStack#getFluid()} is still {@code minecraft:water}).
	 */
	private static boolean isJeiFluidStackBlocked(
		IIngredientHelper<FluidStack> helper,
		FluidStack fs,
		StageCatalog cat,
		Set<String> lockStagesLeaf,
		Set<String> lockStagesVanilla
	) {
		if (fs == null || fs.isEmpty()) {
			return false;
		}
		ResourceLocation rl = fluidIngredientId(helper, fs);
		if (rl != null) {
			Set<String> lock = lockStagesForRegistryNs(lockStagesLeaf, lockStagesVanilla, rl.getNamespace());
			if (StageAccess.isFluidBlockedForEffectiveStages(cat, lock, rl)) {
				return true;
			}
		}
		try {
			String displayMod = helper.getDisplayModId(fs);
			if (displayMod != null && !displayMod.isEmpty()) {
				Set<String> lockDm = lockStagesForRegistryNs(lockStagesLeaf, lockStagesVanilla, displayMod);
				if (StageAccess.isModIdBlockedForEffectiveStages(cat, lockDm, displayMod)) {
					return true;
				}
			}
		} catch (Throwable ignored) {
		}
		return jeiUidDeclaresBlockedContent(helper, fs, cat, lockStagesLeaf, lockStagesVanilla);
	}

	/**
	 * Parses {@link IIngredientHelper#getUniqueId} for embedded {@link ResourceLocation}-like segments (JEI fluids use
	 * {@code fluid:namespace:path}, subtypes append more text). Catches modded fluids whose {@link #fluidIngredientId}
	 * still resolves to a vanilla base fluid.
	 */
	@SuppressWarnings("removal")
	private static <V> boolean jeiUidDeclaresBlockedContent(
		IIngredientHelper<V> helper,
		V ing,
		StageCatalog cat,
		Set<String> lockStagesLeaf,
		Set<String> lockStagesVanilla
	) {
		try {
			String uid = null;
			Object uidObj = helper.getUid(ing, UidContext.Ingredient);
			if (uidObj != null) {
				uid = uidObj.toString();
			}
			if (uid == null || uid.isEmpty()) {
				uid = helper.getUniqueId(ing, UidContext.Ingredient);
			}
			if (uid == null || uid.isEmpty()) {
				return false;
			}
			String scan = uid.startsWith("fluid:") ? uid.substring("fluid:".length()) : uid;
			Matcher m = JEI_UID_LOCATION_PATTERN.matcher(scan);
			while (m.find()) {
				String ns = m.group(1);
				if (ns != null && !ns.isEmpty()) {
					Set<String> lockNs = lockStagesForRegistryNs(lockStagesLeaf, lockStagesVanilla, ns);
					if (StageAccess.isModIdBlockedForEffectiveStages(cat, lockNs, ns)) {
						return true;
					}
				}
				try {
					ResourceLocation id = ResourceLocation.parse(m.group());
					Set<String> lock = lockStagesForRegistryNs(lockStagesLeaf, lockStagesVanilla, id.getNamespace());
					if (StageAccess.isAbstractIngredientBlockedForEffectiveStages(cat, lock, id)) {
						return true;
					}
				} catch (Throwable ignored) {
				}
			}
		} catch (Throwable ignored) {
		}
		return false;
	}

	private static void refreshItemIngredients(
		IIngredientManager im,
		IngredientBlacklistInternal apiBlacklist,
		StageCatalog cat,
		Set<String> lockStagesLeaf,
		Set<String> lockStagesVanilla
	) {
		Set<ResourceLocation> prevHidden = new HashSet<>(hiddenItemIds);
		Set<ResourceLocation> nextHidden = new HashSet<>();
		for (Item item : BuiltInRegistries.ITEM) {
			if (item == Items.AIR) {
				continue;
			}
			ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
			if (id == null) {
				continue;
			}
			ItemStack st = new ItemStack(item);
			Set<String> lock = lockStagesForRegistryNs(lockStagesLeaf, lockStagesVanilla, id.getNamespace());
			if (StageAccess.isItemBlockedForEffectiveStages(cat, lock, st)) {
				nextHidden.add(id);
			}
		}

		Set<ResourceLocation> toRestore = new HashSet<>(prevHidden);
		toRestore.removeAll(nextHidden);

		IIngredientHelper<ItemStack> itemHelper = im.getIngredientHelper(VanillaTypes.ITEM_STACK);
		List<ItemStack> removeStacks = new ArrayList<>();
		for (ITypedIngredient<ItemStack> typed : im.getAllTypedIngredients(VanillaTypes.ITEM_STACK)) {
			ItemStack stack = typed.getIngredient();
			if (stack.isEmpty()) {
				continue;
			}
			ResourceLocation sid = BuiltInRegistries.ITEM.getKey(stack.getItem());
			if (sid == null) {
				continue;
			}
			Set<String> lock = lockStagesForRegistryNs(lockStagesLeaf, lockStagesVanilla, sid.getNamespace());
			if (StageAccess.isItemBlockedForEffectiveStages(cat, lock, stack)) {
				removeStacks.add(itemHelper.copyIngredient(stack));
			}
		}
		if (!removeStacks.isEmpty()) {
			im.removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, removeStacks);
		}

		Set<ResourceLocation> idsInJei = new HashSet<>();
		for (ItemStack st : im.getAllItemStacks()) {
			if (st.isEmpty()) {
				continue;
			}
			ResourceLocation id = BuiltInRegistries.ITEM.getKey(st.getItem());
			if (id != null) {
				idsInJei.add(id);
			}
		}

		List<ItemStack> showStacks = new ArrayList<>();
		for (ResourceLocation id : toRestore) {
			if (idsInJei.contains(id)) {
				continue;
			}
			Item item = BuiltInRegistries.ITEM.get(id);
			if (item != null && item != Items.AIR) {
				showStacks.add(new ItemStack(item));
			}
		}
		if (!showStacks.isEmpty()) {
			im.addIngredientsAtRuntime(VanillaTypes.ITEM_STACK, showStacks);
		}

		for (ITypedIngredient<ItemStack> typed : im.getAllTypedIngredients(VanillaTypes.ITEM_STACK)) {
			ItemStack stack = typed.getIngredient();
			if (stack.isEmpty()) {
				continue;
			}
			ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
			if (id == null) {
				continue;
			}
			boolean blocked = nextHidden.contains(id);
			syncBlacklistTypedIngredient(apiBlacklist, itemHelper, typed, blocked);
		}

		hiddenItemIds.clear();
		hiddenItemIds.addAll(nextHidden);
	}

	/**
	 * JEI may register {@link FluidStack} under more than one {@link IIngredientType} (not always the same
	 * reference as {@code NeoForgeTypes.FLUID_STACK}). Hiding only one type leaves fluids on the list.
	 */
	@SuppressWarnings("unchecked")
	private static void refreshAllFluidStackIngredientTypes(
		IIngredientManager im,
		IngredientBlacklistInternal apiBlacklist,
		StageCatalog cat,
		Set<String> lockStagesLeaf,
		Set<String> lockStagesVanilla
	) {
		Set<ResourceLocation> prevHidden = new HashSet<>(hiddenFluidIds);
		Set<ResourceLocation> nextHidden = new HashSet<>();
		for (Fluid fluid : BuiltInRegistries.FLUID) {
			if (fluid == null || fluid == Fluids.EMPTY) {
				continue;
			}
			ResourceLocation id = BuiltInRegistries.FLUID.getKey(fluid);
			if (id == null) {
				continue;
			}
			Set<String> lock = lockStagesForRegistryNs(lockStagesLeaf, lockStagesVanilla, id.getNamespace());
			if (StageAccess.isFluidBlockedForEffectiveStages(cat, lock, id)) {
				nextHidden.add(id);
			}
		}

		List<IIngredientType<FluidStack>> fluidTypes = new ArrayList<>();
		for (IIngredientType<?> raw : im.getRegisteredIngredientTypes()) {
			if (isFluidStackIngredientType(raw)) {
				fluidTypes.add((IIngredientType<FluidStack>) raw);
			}
		}

		for (IIngredientType<FluidStack> fluidType : fluidTypes) {
			IIngredientHelper<FluidStack> fluidHelper = im.getIngredientHelper(fluidType);
			for (ITypedIngredient<FluidStack> typed : im.getAllTypedIngredients(fluidType)) {
				FluidStack fs = typed.getIngredient();
				if (fs.isEmpty()) {
					continue;
				}
				if (isJeiFluidStackBlocked(fluidHelper, fs, cat, lockStagesLeaf, lockStagesVanilla)) {
					ResourceLocation k = fluidIngredientId(fluidHelper, fs);
					if (k != null) {
						nextHidden.add(k);
					}
				}
			}
		}

		Set<ResourceLocation> toRestore = new HashSet<>(prevHidden);
		toRestore.removeAll(nextHidden);

		for (IIngredientType<FluidStack> fluidType : fluidTypes) {
			IIngredientHelper<FluidStack> fluidHelper = im.getIngredientHelper(fluidType);
			List<FluidStack> removeFluids = new ArrayList<>();
			for (ITypedIngredient<FluidStack> typed : im.getAllTypedIngredients(fluidType)) {
				FluidStack fs = typed.getIngredient();
				if (fs.isEmpty()) {
					continue;
				}
				if (isJeiFluidStackBlocked(fluidHelper, fs, cat, lockStagesLeaf, lockStagesVanilla)) {
					removeFluids.add(fluidHelper.copyIngredient(fs));
				}
			}
			if (!removeFluids.isEmpty()) {
				im.removeIngredientsAtRuntime(fluidType, removeFluids);
			}

			Set<ResourceLocation> idsInJei = new HashSet<>();
			for (FluidStack fs : im.getAllIngredients(fluidType)) {
				if (fs.isEmpty()) {
					continue;
				}
				ResourceLocation id = BuiltInRegistries.FLUID.getKey(fs.getFluid());
				if (id != null) {
					idsInJei.add(id);
				}
			}

			List<FluidStack> showStacks = new ArrayList<>();
			for (ResourceLocation id : toRestore) {
				if (idsInJei.contains(id)) {
					continue;
				}
				Fluid fluid = BuiltInRegistries.FLUID.get(id);
				if (fluid != null && fluid != Fluids.EMPTY) {
					showStacks.add(new FluidStack(fluid, FluidType.BUCKET_VOLUME));
				}
			}
			if (!showStacks.isEmpty()) {
				im.addIngredientsAtRuntime(fluidType, showStacks);
			}

			for (ITypedIngredient<FluidStack> typed : im.getAllTypedIngredients(fluidType)) {
				FluidStack fs = typed.getIngredient();
				if (fs.isEmpty()) {
					continue;
				}
				boolean blocked = isJeiFluidStackBlocked(fluidHelper, fs, cat, lockStagesLeaf, lockStagesVanilla);
				syncBlacklistTypedIngredient(apiBlacklist, fluidHelper, typed, blocked);
			}
		}

		hiddenFluidIds.clear();
		hiddenFluidIds.addAll(nextHidden);
	}

	private static <V> void syncTypedIngredientBlacklist(
		IIngredientManager im,
		IngredientBlacklistInternal apiBlacklist,
		IIngredientType<V> type,
		IIngredientHelper<V> helper,
		Collection<V> universe,
		Predicate<V> blocked
	) {
		for (ITypedIngredient<V> typed : im.getAllTypedIngredients(type)) {
			V ing = typed.getIngredient();
			if (!helper.isValidIngredient(ing)) {
				continue;
			}
			syncBlacklistTypedIngredient(apiBlacklist, helper, typed, blocked.test(ing));
		}
	}

	private static <V> void syncBlacklistTypedIngredient(
		IngredientBlacklistInternal apiBlacklist,
		IIngredientHelper<V> helper,
		ITypedIngredient<V> typed,
		boolean blocked
	) {
		if (apiBlacklist == null) {
			return;
		}
		boolean isBl = apiBlacklist.isIngredientBlacklistedByApi(typed, helper);
		if (blocked && !isBl) {
			apiBlacklist.addIngredientToBlacklist(typed, helper);
		} else if (!blocked && isBl) {
			apiBlacklist.removeIngredientFromBlacklist(typed, helper);
		}
	}

	/**
	 * After {@link IIngredientManager#removeIngredientsAtRuntime}, JEI's {@link IngredientFilter#onIngredientsRemoved} is a
	 * no-op — stale {@code IListElement} rows stay until {@link IngredientFilter#rebuildItemFilter()} runs.
	 */
	private static void notifyJeiIngredientFilter(IJeiRuntime r) {
		try {
			if (r.getIngredientFilter() instanceof IngredientFilter filter) {
				filter.rebuildItemFilter();
				filter.updateHidden();
			}
		} catch (Throwable ignored) {
		}
	}

	private static void scheduleRefresh() {
		if (runtime == null) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		if (mc == null) {
			return;
		}
		if (!refreshQueued.compareAndSet(false, true)) {
			return;
		}
		mc.execute(() -> {
			refreshQueued.set(false);
			refresh();
			// IngredientBlacklistInternal.onIngredientsAdded clears API blacklist for added stacks; second pass reapplies hides.
			mc.execute(JeiStagesSupport::refresh);
		});
	}

	private static void maybeLogJeiDebugSnapshot(
		IIngredientManager im,
		StageCatalog cat,
		Set<String> owned,
		Set<String> lockStagesLeaf,
		Set<String> lockStagesVanilla
	) {
		if (!StagesQXModConfig.DEBUG.get()) {
			return;
		}
		int max = StagesQXModConfig.DEBUG_JEI_MAX_LOG_LINES.get();
		boolean logVanilla = StagesQXModConfig.DEBUG_JEI_LOG_VANILLA.get();
		Set<String> lockedNs = cat.namespacesUnderStageModLocks();
		JEI_DEBUG_LOG.info(
			"JEI debug snapshot: catalogEmpty={} ownedStages={} effectiveLockLeaf={} effectiveLockVanilla={} namespacesUnderStageModLocks(count)={} maxLines={} logVanilla={}",
			cat.isEmpty(),
			owned,
			lockStagesLeaf,
			lockStagesVanilla,
			lockedNs.size(),
			max,
			logVanilla
		);
		if (max <= 0 || cat.isEmpty()) {
			return;
		}
		int logged = 0;
		itemLoop:
		for (ItemStack stack : im.getAllItemStacks()) {
			if (logged >= max) {
				break itemLoop;
			}
			if (stack.isEmpty()) {
				continue;
			}
			ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
			if (id == null) {
				continue;
			}
			if (!lockedNs.contains(id.getNamespace())) {
				continue;
			}
			if ("minecraft".equals(id.getNamespace()) && !logVanilla) {
				continue;
			}
			boolean packBlocked = StageAccess.isItemBlocked(cat, owned, stack);
			String detail = RecipeViewerDebugExplain.explainItemPackGating(cat, owned, id);
			if (packBlocked) {
				JEI_DEBUG_LOG.warn("[jei/leak] item still in JEI ingredient manager but pack marks blocked: {} | {}", id, detail);
			} else {
				JEI_DEBUG_LOG.info("[jei/visible] item not pack-blocked (under locked namespace): {} | {}", id, detail);
			}
			logged++;
		}

		fluidOuter:
		for (IIngredientType<?> raw : im.getRegisteredIngredientTypes()) {
			if (!isFluidStackIngredientType(raw)) {
				continue;
			}
			@SuppressWarnings("unchecked")
			IIngredientType<FluidStack> fluidType = (IIngredientType<FluidStack>) raw;
			IIngredientHelper<FluidStack> fluidHelper = im.getIngredientHelper(fluidType);
			for (ITypedIngredient<FluidStack> typed : im.getAllTypedIngredients(fluidType)) {
				if (logged >= max) {
					break fluidOuter;
				}
				FluidStack fs = typed.getIngredient();
				if (fs.isEmpty()) {
					continue;
				}
				ResourceLocation fluidRl = fluidIngredientId(fluidHelper, fs);
				String ns = fluidRl != null ? fluidRl.getNamespace() : null;
				if (ns == null) {
					try {
						ns = fluidHelper.getDisplayModId(fs);
					} catch (Throwable ignored) {
					}
				}
				if (ns == null || !lockedNs.contains(ns)) {
					continue;
				}
				if ("minecraft".equals(ns) && !logVanilla) {
					continue;
				}
				boolean packBlocked = fluidRl != null && StageAccess.isFluidBlocked(cat, owned, fluidRl);
				boolean jeiTreatBlocked = isJeiFluidStackBlocked(fluidHelper, fs, cat, lockStagesLeaf, lockStagesVanilla);
				String detail = RecipeViewerDebugExplain.explainFluidPackGating(cat, owned, fluidRl)
					+ " | jeiBlocked=" + jeiTreatBlocked
					+ " uid=" + safeUidString(fluidHelper, fs);
				if (packBlocked && !jeiTreatBlocked) {
					JEI_DEBUG_LOG.warn("[jei/leak] fluid pack-blocked but JEI hook treats visible: {} | {}", fluidRl, detail);
				} else if (!packBlocked) {
					JEI_DEBUG_LOG.info("[jei/visible] fluid not pack-blocked: {} | {}", fluidRl, detail);
				} else {
					continue;
				}
				logged++;
			}
		}

		genericOuter:
		for (IIngredientType<?> rawType : im.getRegisteredIngredientTypes()) {
			if (rawType == VanillaTypes.ITEM_STACK || isFluidStackIngredientType(rawType)) {
				continue;
			}
			@SuppressWarnings("unchecked")
			IIngredientHelper<Object> helper = (IIngredientHelper<Object>) im.getIngredientHelper(rawType);
			for (ITypedIngredient<?> typed : im.getAllTypedIngredients(rawType)) {
				if (logged >= max) {
					break genericOuter;
				}
				Object ing = typed.getIngredient();
				if (!helper.isValidIngredient(ing)) {
					continue;
				}
				ResourceLocation rl = safeResourceLocation(helper, ing);
				String ns = rl != null ? rl.getNamespace() : null;
				if (ns == null) {
					try {
						ns = helper.getDisplayModId(ing);
					} catch (Throwable ignored) {
					}
				}
				if (ns == null || !lockedNs.contains(ns)) {
					continue;
				}
				if ("minecraft".equals(ns) && !logVanilla) {
					continue;
				}
				boolean jeiTreatBlocked = isGenericJeiIngredientBlocked(helper, ing, cat, lockStagesLeaf, lockStagesVanilla);
				String detail = RecipeViewerDebugExplain.explainGenericPackGating(cat, owned, rl, ns)
					+ " | typeUid=" + rawType.getUid()
					+ " jeiBlocked=" + jeiTreatBlocked
					+ " uid=" + safeUidString(helper, ing);
				if (jeiTreatBlocked) {
					JEI_DEBUG_LOG.warn("[jei/leak?] generic ingredient type={} marked blocked by hook but still listed — check remove path: rl={} | {}", rawType.getUid(), rl, detail);
				} else {
					JEI_DEBUG_LOG.info("[jei/visible] generic type={} rl={} | {}", rawType.getUid(), rl, detail);
				}
				logged++;
			}
		}
		if (logged >= max) {
			JEI_DEBUG_LOG.info("JEI debug: hit debug_jei_max_log_lines={} (more entries omitted)", max);
		}
	}

	@SuppressWarnings("removal")
	private static <V> String safeUidString(IIngredientHelper<V> helper, V ing) {
		try {
			Object u = helper.getUid(ing, UidContext.Ingredient);
			if (u != null) {
				return u.toString();
			}
		} catch (Throwable ignored) {
		}
		try {
			return helper.getUniqueId(ing, UidContext.Ingredient);
		} catch (Throwable ignored) {
		}
		return "?";
	}

}
