package com.stagesqx.neoforge.mixin;

import com.stagesqx.neoforge.PlayerStageStore;
import com.stagesqx.neoforge.StageFeedback;
import com.stagesqx.stage.StageAccess;
import com.stagesqx.stage.StageRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnchantmentMenu.class)
public abstract class EnchantmentMenuMixin {
	@Inject(method = "clickMenuButton", at = @At("HEAD"), cancellable = true)
	private void stagesqx$blockEnchant(net.minecraft.world.entity.player.Player player, int id, CallbackInfoReturnable<Boolean> cir) {
		if (!(player instanceof ServerPlayer sp) || player.level().isClientSide()) {
			return;
		}
		if (StageAccess.bypassesLocks(sp)) {
			return;
		}
		EnchantmentMenu self = (EnchantmentMenu) (Object) this;
		ItemStack item = self.getSlot(0).getItem();
		if (!item.isEmpty() && StageAccess.isItemBlocked(StageRegistry.getCatalog(), PlayerStageStore.get(sp), item)) {
			StageFeedback.notifyBlocked(sp, item);
			cir.setReturnValue(false);
		}
	}
}
