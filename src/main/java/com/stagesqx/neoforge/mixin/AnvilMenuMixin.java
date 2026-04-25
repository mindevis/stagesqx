package com.stagesqx.neoforge.mixin;

import com.stagesqx.neoforge.PlayerStageStore;
import com.stagesqx.neoforge.StageFeedback;
import com.stagesqx.stage.StageAccess;
import com.stagesqx.stage.StageRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin {

	@Inject(method = "createResult", at = @At("TAIL"))
	private void stagesqx$clearBlockedResult(CallbackInfo ci) {
		Player player = ((ItemCombinerMenuAccessor) (Object) this).stagesqx$getPlayer();
		if (!(player instanceof ServerPlayer sp) || player.level().isClientSide()) {
			return;
		}
		if (StageAccess.bypassesLocks(sp)) {
			return;
		}
		AnvilMenu self = (AnvilMenu) (Object) this;
		ItemStack left = self.getSlot(AnvilMenu.INPUT_SLOT).getItem();
		ItemStack right = self.getSlot(AnvilMenu.ADDITIONAL_SLOT).getItem();
		ItemStack out = self.getSlot(AnvilMenu.RESULT_SLOT).getItem();
		var cat = StageRegistry.getCatalog();
		var owned = PlayerStageStore.get(sp);
		boolean blocked = (!left.isEmpty() && StageAccess.isItemBlocked(cat, owned, left))
			|| (!right.isEmpty() && StageAccess.isItemBlocked(cat, owned, right))
			|| (!out.isEmpty() && StageAccess.isItemBlocked(cat, owned, out));
		if (blocked) {
			if (!out.isEmpty()) {
				StageFeedback.notifyBlocked(sp, out);
			} else if (!left.isEmpty()) {
				StageFeedback.notifyBlocked(sp, left);
			}
			self.getSlot(AnvilMenu.RESULT_SLOT).set(ItemStack.EMPTY);
		}
	}
}
