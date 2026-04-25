package com.stagesqx.stage;

import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Set;

/**
 * Parsed stage file. {@link #locks()} defines content that <strong>requires</strong> this stage id to be accessible;
 * {@link #unlocks()} carve out exceptions (whitelists) for that same stage requirement.
 */
public record StageDefinition(
	String id,
	String displayName,
	String description,
	String icon,
	String unlockMessage,
	/** Logical prerequisites for progression; used by {@link StageAccess#effectiveAccessStages} to treat required stages as satisfied when only a leaf stage is granted. */
	List<String> dependency,
	boolean minecraft,
	StageGateLists locks,
	StageGateLists unlocks
) {
	public StageDefinition {
		dependency = List.copyOf(dependency);
	}

	public String effectiveDisplayName() {
		return displayName == null || displayName.isBlank() ? id : displayName;
	}

	public static StageDefinition empty(String id) {
		return new StageDefinition(
			id,
			"",
			"",
			"",
			"",
			List.of(),
			false,
			StageGateLists.empty(),
			StageGateLists.empty()
		);
	}

	public StageDefinition withLocksAndUnlocks(
		List<String> dependency,
		boolean minecraft,
		StageGateLists locks,
		StageGateLists unlocks
	) {
		return new StageDefinition(
			id,
			displayName,
			description,
			icon,
			unlockMessage,
			dependency,
			minecraft,
			locks,
			unlocks
		);
	}

	public StageDefinition withMeta(String displayName, String description, String icon, String unlockMessage) {
		return new StageDefinition(
			id,
			displayName != null ? displayName : "",
			description != null ? description : "",
			icon != null ? icon : "",
			unlockMessage != null ? unlockMessage : "",
			dependency,
			minecraft,
			locks,
			unlocks
		);
	}

	public static StageDefinition mergeFileId(String fileId, StageDefinition fromFile) {
		return new StageDefinition(
			fileId,
			fromFile.displayName,
			fromFile.description,
			fromFile.icon,
			fromFile.unlockMessage,
			fromFile.dependency,
			fromFile.minecraft,
			fromFile.locks,
			fromFile.unlocks
		);
	}
}
