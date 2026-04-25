package com.stagesqx.neoforge.mixin;

import com.stagesqx.neoforge.PlayerStageStore;
import com.stagesqx.neoforge.StageFeedback;
import com.stagesqx.stage.StageAccess;
import com.stagesqx.stage.StageRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Slot.class)
public abstract class SlotMixin {
	@Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
	private void stagesqx$mayPickup(Player player, CallbackInfoReturnable<Boolean> cir) {
		if (!(player instanceof ServerPlayer sp) || player.level().isClientSide()) {
			return;
		}
		if (StageAccess.bypassesLocks(sp)) {
			return;
		}
		Slot self = (Slot) (Object) this;
		ItemStack st = self.getItem();
		if (st.isEmpty()) {
			return;
		}
		if (StageAccess.isItemBlocked(StageRegistry.getCatalog(), PlayerStageStore.get(sp), st)) {
			StageFeedback.notifyBlocked(sp, st);
			cir.setReturnValue(false);
		}
	}
}
