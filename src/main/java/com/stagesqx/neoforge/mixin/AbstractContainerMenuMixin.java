package com.stagesqx.neoforge.mixin;

import com.stagesqx.neoforge.PlayerStageStore;
import com.stagesqx.neoforge.StageFeedback;
import com.stagesqx.stage.StageAccess;
import com.stagesqx.stage.StageRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuMixin {
	@Shadow
	public abstract ItemStack getCarried();

	@Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
	private void stagesqx$clicked(int slotId, int button, ClickType clickType, Player player, CallbackInfo ci) {
		if (!(player instanceof ServerPlayer sp) || player.level().isClientSide()) {
			return;
		}
		if (StageAccess.bypassesLocks(sp)) {
			return;
		}
		ItemStack carried = getCarried();
		if (carried.isEmpty()) {
			return;
		}
		if (clickType == ClickType.PICKUP || clickType == ClickType.QUICK_MOVE || clickType == ClickType.SWAP || clickType == ClickType.THROW) {
			if (StageAccess.isItemBlocked(StageRegistry.getCatalog(), PlayerStageStore.get(sp), carried)) {
				StageFeedback.notifyBlocked(sp, carried);
				ci.cancel();
			}
		}
	}
}
