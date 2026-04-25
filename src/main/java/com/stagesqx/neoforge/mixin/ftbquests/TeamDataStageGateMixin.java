package com.stagesqx.neoforge.mixin.ftbquests;

import com.stagesqx.neoforge.integration.ftbquests.StagesQXFtbQuestsRequiredStageHolder;
import com.stagesqx.neoforge.integration.ftbquests.StagesQXFtbQuestsStageGate;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.TeamData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TeamData.class)
public abstract class TeamDataStageGateMixin {
	@Inject(method = "canStartTasks", at = @At("HEAD"), cancellable = true)
	private void stagesqx$canStartTasks(Quest quest, CallbackInfoReturnable<Boolean> cir) {
		Object q = quest;
		if (q instanceof StagesQXFtbQuestsRequiredStageHolder holder) {
			String req = holder.stagesqx$getRequiredStageId();
			if (req != null && !req.isBlank() && !StagesQXFtbQuestsStageGate.requiredStageMetForServerLogic(req)) {
				cir.setReturnValue(false);
			}
		}
	}
}

