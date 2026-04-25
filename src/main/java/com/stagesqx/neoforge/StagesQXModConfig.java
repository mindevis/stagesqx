package com.stagesqx.neoforge;

import com.stagesqx.stage.StageTomlIo;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public final class StagesQXModConfig {
	public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

	public static final ModConfigSpec.BooleanValue LOG_OPERATIONS = BUILDER
		.comment("Log create, validate, reload operations to the server log.")
		.define("logOperations", true);

	public static final ModConfigSpec.BooleanValue DEBUG = BUILDER
		.comment(
			"When true, the client logs recipe viewer diagnostics (JEI and EMI): entries whose namespace is under stage",
			"[locks].mods (or minecraft) but are not pack-blocked, with a short reason (owned stages, gating vs effective locks).",
			"JEI: logger stagesqx/jei/debug — tune debug_jei_max_log_lines and debug_jei_log_vanilla.",
			"EMI: logger stagesqx/emi/debug — tune debug_emi_max_log_lines and debug_emi_log_vanilla."
		)
		.define("debug", false);

	public static final ModConfigSpec.IntValue DEBUG_JEI_MAX_LOG_LINES = BUILDER
		.comment("Max detail lines per JEI refresh when debug is true (0 = only summary header).")
		.defineInRange("debug_jei_max_log_lines", 500, 0, 50_000);

	public static final ModConfigSpec.BooleanValue DEBUG_JEI_LOG_VANILLA = BUILDER
		.comment("When debug is true, also log minecraft: namespace entries for JEI (very verbose if minecraft=true on a stage).")
		.define("debug_jei_log_vanilla", false);

	public static final ModConfigSpec.IntValue DEBUG_EMI_MAX_LOG_LINES = BUILDER
		.comment("Max detail lines per EMI index reload when debug is true (0 = only summary header).")
		.defineInRange("debug_emi_max_log_lines", 2000, 0, 50_000);

	public static final ModConfigSpec.BooleanValue DEBUG_EMI_LOG_VANILLA = BUILDER
		.comment("When debug is true, also log minecraft: namespace EMI entries (verbose if minecraft=true on a stage).")
		.define("debug_emi_log_vanilla", true);

	public static final ModConfigSpec.BooleanValue FTBQUESTS_TEAM_MODE = BUILDER
		.comment(
			"When true, FTB Quests stage requirements check FTB Teams team stages.",
			"When false, they check per-player StagesQX stages."
		)
		.define("ftbquests_team_mode", false);

	public static final ModConfigSpec.ConfigValue<List<? extends String>> STARTING_STAGES = BUILDER
		.comment(
			"Stage ids granted on login. While a player HAS a stage, that stage's [locks] in its .toml APPLY",
			"(content is restricted). Revoke the stage (e.g. /stagesqx revoke) to lift those locks; grant the next",
			"stage to switch which restrictions are active. Empty [] = no starting restrictions.",
			"Ids = .toml file name without extension (e.g. stage_1 for stage_1.toml)."
		)
		.defineListAllowEmpty("starting_stages", List.of(), StagesQXModConfig::isValidStartingStageEntry);

	public static final ModConfigSpec.BooleanValue PLAY_LOCK_SOUND = BUILDER
		.comment("Play a sound when the player hits a stage lock (item, fluid, dimension, container rules).")
		.define("play_lock_sound", true);

	public static final ModConfigSpec.ConfigValue<String> LOCK_SOUND = BUILDER
		.comment("Sound event id, e.g. minecraft:block.note_block.pling")
		.define("lock_sound", "minecraft:block.note_block.pling");

	public static final ModConfigSpec.DoubleValue LOCK_SOUND_VOLUME = BUILDER
		.comment("Lock sound volume.")
		.defineInRange("lock_sound_volume", 1.0, 0.0, 1.0);

	public static final ModConfigSpec.DoubleValue LOCK_SOUND_PITCH = BUILDER
		.comment("Lock sound pitch.")
		.defineInRange("lock_sound_pitch", 1.0, 0.5, 2.0);

	public static final ModConfigSpec.BooleanValue REVEAL_STAGE_NAMES_ONLY_TO_OPERATORS = BUILDER
		.comment(
			"If true, lock feedback (chat when blocked, item tooltips) shows stage display names only to players",
			"with permission level 2+ (operators). Others see generic messages without stage ids or names.",
			"The value is sent to clients when syncing stages so tooltips match the server."
		)
		.define("reveal_stage_names_only_to_operators", true);

	public static final ModConfigSpec.BooleanValue EJECT_BLOCKED_INVENTORY_ITEMS = BUILDER
		.comment(
			"If true, periodically remove item stacks that are stage-blocked for the player from main inventory,",
			"armor, and offhand, and drop them at the player's feet. Skips creative/spectator."
		)
		.define("eject_blocked_inventory_items", false);

	public static final ModConfigSpec.IntValue EJECT_BLOCKED_INVENTORY_INTERVAL_TICKS = BUILDER
		.comment("Ticks between inventory scans when eject_blocked_inventory_items is true (20 ticks ≈ 1 s).")
		.defineInRange("eject_blocked_inventory_interval_ticks", 40, 5, 1200);

	public static final ModConfigSpec SPEC = BUILDER.build();

	private static boolean isValidStartingStageEntry(Object o) {
		if (!(o instanceof String s)) {
			return false;
		}
		String t = s.trim();
		return !t.isEmpty() && StageTomlIo.isValidStageName(t);
	}

	private StagesQXModConfig() {
	}
}
