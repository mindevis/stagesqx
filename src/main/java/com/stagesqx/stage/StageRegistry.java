package com.stagesqx.stage;

import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Thread-safe holder for the current {@link StageCatalog}. Reload replaces the snapshot.
 */
public final class StageRegistry {
	private static final Logger LOGGER = LoggerFactory.getLogger("stagesqx/registry");
	private static final Object LOCK = new Object();
	private static volatile StageCatalog catalog = StageCatalog.empty();
	private static Path stagesDirectory;

	private StageRegistry() {
	}

	public static void setStagesDirectory(Path dir) {
		synchronized (LOCK) {
			stagesDirectory = dir;
		}
	}

	public static Path getStagesDirectory() {
		synchronized (LOCK) {
			return stagesDirectory;
		}
	}

	public static StageCatalog getCatalog() {
		return catalog;
	}

	public static Map<String, Path> discoverStageFiles(Path stagesDir) {
		Map<String, Path> map = new LinkedHashMap<>();
		if (!Files.isDirectory(stagesDir)) {
			return map;
		}
		try (Stream<Path> stream = Files.list(stagesDir)) {
			stream.filter(p -> {
				String n = p.getFileName().toString();
				return n.endsWith(".toml") && !n.equalsIgnoreCase("stagesqx.toml");
			}).sorted().forEach(p -> {
				String name = p.getFileName().toString();
				String id = name.substring(0, name.length() - ".toml".length());
				map.put(id, p);
			});
		} catch (IOException e) {
			LOGGER.error("Failed to list stage files: {}", e.toString());
		}
		return map;
	}

	public static StageCatalog loadFromDisk(Path stagesDir) {
		Map<String, Path> files = discoverStageFiles(stagesDir);
		Map<String, StageDefinition> defs = new LinkedHashMap<>();
		for (Map.Entry<String, Path> e : files.entrySet()) {
			StageDefinition def = StageTomlIo.loadStageFile(e.getKey(), e.getValue());
			defs.put(e.getKey(), def);
		}
		return new StageCatalog(defs);
	}

	public static void reload(MinecraftServer server) {
		Path dir = getStagesDirectory();
		if (dir == null) {
			LOGGER.warn("Stages directory not set; reload skipped.");
			return;
		}
		try {
			Files.createDirectories(dir);
		} catch (IOException e) {
			LOGGER.error("Could not create stages directory: {}", e.toString());
		}
		StageCatalog next = loadFromDisk(dir);
		catalog = next;
		LOGGER.info("Stages reloaded: {} stage(s) active.", next.stagesById().size());
	}

	public static void bootstrapFromDisk() {
		Path dir = getStagesDirectory();
		if (dir == null) {
			return;
		}
		catalog = loadFromDisk(dir);
		LOGGER.info("Stages bootstrap: {} stage(s).", catalog.stagesById().size());
	}

	public static void writeTemplateFile(Path file, String stageId, TemplateContent content) throws IOException {
		Files.createDirectories(file.getParent());
		try (BufferedWriter w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
			w.write("# id = \"" + escapeTomlBasic(stageId) + "\"\n");
			w.write("# display_name = \"\"\n");
			w.write("# description = \"\"\n");
			w.write("# icon = \"\"\n");
			w.write("# unlock_message = \"\"\n");
			w.write("# dependency = []  # prerequisites; if you grant only a leaf stage, prerequisites are treated as satisfied for gating\n");
			w.write("# minecraft = false  # when true, this stage also gates vanilla namespace (minecraft: items/fluids/entities/dimensions)\n");
			w.write("#\n");
			w.write("# [locks] — content listed here REQUIRES this stage to be accessible (missing stage => blocked)\n");
			w.write("# minecraft = false  # may be set here or at file root; gates namespace minecraft (items, fluids, …)\n");
			w.write("# mods = " + formatStringList(content.allModIds()) + "\n");
			w.write("# items = " + formatResourceList(content.allItemIds()) + "\n");
			w.write("# fluids = " + formatResourceList(content.allFluidIds()) + "\n");
			w.write("# dimensions = " + formatResourceList(content.allDimensionIds()) + "\n");
			w.write("# entities = " + formatResourceList(content.allEntityIds()) + "\n");
			w.write("#\n");
			w.write("# [unlocks] — exceptions (whitelist) for this stage's requirements\n");
			w.write("# mods = []\n");
			w.write("# items = []\n");
			w.write("# fluids = []\n");
			w.write("# dimensions = []\n");
			w.write("# entities = []\n");
			w.write("#\n");
			w.write("# Legacy (no [locks] table): root keys items, mods, fluids, dimensions, entities are treated as locks.\n");
		}
	}

	private static String escapeTomlBasic(String s) {
		return s.replace("\\", "\\\\").replace("\"", "\\\"");
	}

	private static String formatStringList(java.util.Collection<String> c) {
		StringBuilder sb = new StringBuilder();
		sb.append('[');
		boolean first = true;
		for (String s : c) {
			if (!first) {
				sb.append(", ");
			}
			first = false;
			sb.append('"').append(escapeTomlBasic(s)).append('"');
		}
		sb.append(']');
		return sb.toString();
	}

	private static String formatResourceList(java.util.Collection<net.minecraft.resources.ResourceLocation> c) {
		StringBuilder sb = new StringBuilder();
		sb.append('[');
		boolean first = true;
		for (net.minecraft.resources.ResourceLocation rl : c) {
			if (!first) {
				sb.append(", ");
			}
			first = false;
			sb.append('"').append(escapeTomlBasic(rl.toString())).append('"');
		}
		sb.append(']');
		return sb.toString();
	}

	public record TemplateContent(
		java.util.List<String> allModIds,
		java.util.List<net.minecraft.resources.ResourceLocation> allItemIds,
		java.util.List<net.minecraft.resources.ResourceLocation> allFluidIds,
		java.util.List<net.minecraft.resources.ResourceLocation> allDimensionIds,
		java.util.List<net.minecraft.resources.ResourceLocation> allEntityIds
	) {
	}
}
