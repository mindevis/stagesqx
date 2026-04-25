package com.stagesqx.neoforge.mixin.ftbquests;

import com.stagesqx.neoforge.integration.ftbquests.StagesQXFtbQuestsRequiredStageHolder;
import com.stagesqx.neoforge.integration.ftbquests.StagesQXFtbQuestsStageGate;
import com.stagesqx.neoforge.integration.ftbquests.StagesQXFtbConfigCompat;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.TeamData;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Quest.class)
public abstract class QuestStageGateMixin implements StagesQXFtbQuestsRequiredStageHolder {
	@Unique
	private static final String STAGESQX_REQUIRED_STAGE_KEY = "stagesqx_required_stage";

	@Unique
	private String stagesqx$requiredStageId = "";

	@Override
	public String stagesqx$getRequiredStageId() {
		return stagesqx$requiredStageId;
	}

	@Override
	public void stagesqx$setRequiredStageId(String stageId) {
		stagesqx$requiredStageId = stageId == null ? "" : stageId.trim();
	}

	@Inject(method = "writeData", at = @At("TAIL"))
	private void stagesqx$writeData(CompoundTag nbt, HolderLookup.Provider provider, CallbackInfo ci) {
		if (!stagesqx$requiredStageId.isBlank()) {
			nbt.putString(STAGESQX_REQUIRED_STAGE_KEY, stagesqx$requiredStageId.trim());
		}
	}

	@Inject(method = "readData", at = @At("TAIL"))
	private void stagesqx$readData(CompoundTag nbt, HolderLookup.Provider provider, CallbackInfo ci) {
		stagesqx$requiredStageId = nbt.contains(STAGESQX_REQUIRED_STAGE_KEY) ? nbt.getString(STAGESQX_REQUIRED_STAGE_KEY).trim() : "";
	}

	@Inject(method = "writeNetData", at = @At("TAIL"))
	private void stagesqx$writeNetData(RegistryFriendlyByteBuf buffer, CallbackInfo ci) {
		buffer.writeUtf(stagesqx$requiredStageId, Short.MAX_VALUE);
	}

	@Inject(method = "readNetData", at = @At("TAIL"))
	private void stagesqx$readNetData(RegistryFriendlyByteBuf buffer, CallbackInfo ci) {
		stagesqx$requiredStageId = buffer.readUtf(Short.MAX_VALUE).trim();
	}

	@Inject(method = "fillConfigGroup", at = @At("TAIL"))
	private void stagesqx$fillConfigGroup(@Coerce Object config, CallbackInfo ci) {
		java.util.function.Consumer<String> setter = v -> stagesqx$requiredStageId = v != null ? v.trim() : "";
		StagesQXFtbConfigCompat.tryAddRequiredStageString(config, stagesqx$requiredStageId, setter);
	}

	@Inject(method = "isVisible", at = @At("HEAD"), cancellable = true)
	private void stagesqx$isVisible(TeamData data, CallbackInfoReturnable<Boolean> cir) {
		if (!stagesqx$requiredStageId.isBlank() && !StagesQXFtbQuestsStageGate.requiredStageMetForVisibility(stagesqx$requiredStageId)) {
			cir.setReturnValue(false);
		}
	}
}

