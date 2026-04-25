package com.stagesqx.stage;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class StageTomlIo {
	private static final Logger LOGGER = LoggerFactory.getLogger("stagesqx/config");
	private static final Pattern VALID_ID = Pattern.compile("^[a-z0-9._-]+$");

	private StageTomlIo() {
	}

	/**
	 * Heuristic filter for stage files when a single directory contains both stage and trigger toml files.
	 * <p>
	 * We treat a toml as a stage file when it declares any stage-related keys (locks/unlocks, legacy lock keys,
	 * dependency, minecraft flag). Trigger configs should not include those keys and will be ignored by the stage loader.
	 */
	public static boolean looksLikeStageFile(Path path) {
		if (path == null) {
			return false;
		}
		CommentedFileConfig config = CommentedFileConfig.builder(path)
			.writingMode(WritingMode.REPLACE)
			.build();
		try {
			config.load();
			return config.contains("locks")
				|| config.contains("unlocks")
				|| config.contains("items")
				|| config.contains("mods")
				|| config.contains("fluids")
				|| config.contains("dimensions")
				|| config.contains("entities")
				|| config.contains("minecraft")
				|| config.contains("dependency");
		} catch (Exception e) {
			return false;
		} finally {
			config.close();
		}
	}

	public static StageDefinition loadStageFile(String fileId, Path path) {
		CommentedFileConfig config = CommentedFileConfig.builder(path)
			.writingMode(WritingMode.REPLACE)
			.build();
		try {
			config.load();
			return parse(fileId, config);
		} catch (Exception e) {
			LOGGER.error("Failed to load stage file {}: {}", path, e.toString());
			return StageDefinition.empty(fileId);
		} finally {
			config.close();
		}
	}

	public static StageDefinition parse(String fileId, CommentedFileConfig config) {
		String displayName = string(config, "display_name");
		String description = string(config, "description");
		String icon = string(config, "icon");
		String unlockMessage = string(config, "unlock_message");
		List<String> dependency = stringList(config, "dependency");

		UnmodifiableConfig locksTable = subsection(config, "locks");
		UnmodifiableConfig unlocksTable = subsection(config, "unlocks");
		boolean lockMinecraft = bool(config, "minecraft");
		if (locksTable != null) {
			lockMinecraft = lockMinecraft || bool(locksTable, "minecraft");
		}
		StageGateLists locks;
		StageGateLists unlocks;
		if (locksTable != null) {
			locks = parseGateLists(locksTable, "locks");
			unlocks = unlocksTable != null ? parseGateLists(unlocksTable, "unlocks") : StageGateLists.empty();
		} else {
			locks = legacyLocksFromRoot(config);
			unlocks = unlocksTable != null ? parseGateLists(unlocksTable, "unlocks") : StageGateLists.empty();
		}

		String idFromFile = string(config, "id");
		String effectiveId = fileId;
		if (idFromFile != null && !idFromFile.isBlank() && !idFromFile.equals(fileId)) {
			LOGGER.warn("Stage file {} declares id '{}' — using filename id '{}'", fileId, idFromFile, fileId);
		}

		return new StageDefinition(
			effectiveId,
			displayName,
			description,
			icon,
			unlockMessage,
			dependency,
			lockMinecraft,
			locks,
			unlocks
		);
	}

	private static StageGateLists legacyLocksFromRoot(CommentedFileConfig config) {
		return new StageGateLists(
			resourceSet(config, "items"),
			stringSet(config, "mods"),
			resourceSet(config, "fluids"),
			resourceSet(config, "dimensions"),
			resourceSet(config, "entities")
		);
	}

	private static StageGateLists parseGateLists(UnmodifiableConfig table, String tableNameForLog) {
		return new StageGateLists(
			resourceSet(table, "items", tableNameForLog),
			stringSet(table, "mods"),
			resourceSet(table, "fluids", tableNameForLog),
			resourceSet(table, "dimensions", tableNameForLog),
			resourceSet(table, "entities", tableNameForLog)
		);
	}

	private static UnmodifiableConfig subsection(UnmodifiableConfig c, String key) {
		if (!c.contains(key)) {
			return null;
		}
		Object v = c.get(key);
		if (v instanceof UnmodifiableConfig uc) {
			return uc;
		}
		return null;
	}

	private static String string(UnmodifiableConfig c, String key) {
		if (!c.contains(key)) {
			return "";
		}
		Object v = c.get(key);
		return v == null ? "" : String.valueOf(v);
	}

	private static boolean bool(UnmodifiableConfig c, String key) {
		if (!c.contains(key)) {
			return false;
		}
		Object v = c.get(key);
		if (v instanceof Boolean b) {
			return b;
		}
		if (v instanceof Number n) {
			return n.intValue() != 0;
		}
		return Boolean.parseBoolean(String.valueOf(v));
	}

	private static List<String> stringList(UnmodifiableConfig c, String key) {
		if (!c.contains(key)) {
			return List.of();
		}
		Object v = c.get(key);
		if (v instanceof List<?> list) {
			List<String> out = new ArrayList<>();
			for (Object o : list) {
				if (o != null) {
					out.add(String.valueOf(o));
				}
			}
			return out;
		}
		return List.of();
	}

	private static Set<String> stringSet(UnmodifiableConfig c, String key) {
		return new LinkedHashSet<>(stringList(c, key));
	}

	private static Set<ResourceLocation> resourceSet(UnmodifiableConfig c, String key) {
		return resourceSet(c, key, null);
	}

	private static Set<ResourceLocation> resourceSet(UnmodifiableConfig c, String key, String ctx) {
		Set<ResourceLocation> out = new LinkedHashSet<>();
		for (String s : stringList(c, key)) {
			if (s == null || s.isBlank()) {
				continue;
			}
			String t = s.trim().toLowerCase(Locale.ROOT);
			try {
				out.add(ResourceLocation.parse(t));
			} catch (Exception e) {
				String where = ctx != null ? ctx + "." + key : key;
				LOGGER.warn("Invalid resource location in {}: {}", where, s);
			}
		}
		return out;
	}

	public static boolean isValidStageName(String name) {
		return name != null && VALID_ID.matcher(name).matches();
	}
}
