package com.stagesqx.neoforge;

import com.stagesqx.stage.StageAccess;
import com.stagesqx.stage.StageCatalog;
import com.stagesqx.stage.StageRegistry;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.Set;

/**
 * Drops stage-blocked stacks from vanilla player inventory (main, armor, offhand).
 */
public final class StageInventoryEject {
	private StageInventoryEject() {
	}

	public static void ejectBlockedStacks(ServerPlayer sp) {
		StageCatalog cat = StageRegistry.getCatalog();
		if (cat.isEmpty()) {
			return;
		}
		Set<String> owned = PlayerStageStore.get(sp);
		Inventory inv = sp.getInventory();
		ejectFromList(sp, cat, owned, inv.items);
		ejectFromList(sp, cat, owned, inv.armor);
		ejectFromList(sp, cat, owned, inv.offhand);
	}

	private static void ejectFromList(
		ServerPlayer sp,
		StageCatalog cat,
		Set<String> owned,
		NonNullList<ItemStack> list
	) {
		for (int i = 0; i < list.size(); i++) {
			ItemStack st = list.get(i);
			if (st.isEmpty()) {
				continue;
			}
			if (!StageAccess.isItemBlocked(cat, owned, st)) {
				continue;
			}
			sp.drop(st.copy(), true);
			list.set(i, ItemStack.EMPTY);
		}
	}
}
