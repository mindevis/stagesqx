package com.stagesqx.neoforge.integration.ftbquests;

/**
 * Implemented by FTB Quests mixins to store a StagesQX stage id requirement.
 */
public interface StagesQXFtbQuestsRequiredStageHolder {
	String stagesqx$getRequiredStageId();

	void stagesqx$setRequiredStageId(String stageId);
}

