package com.stagesqx.stage;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class StageValidator {
	private static final Logger LOGGER = LoggerFactory.getLogger("stagesqx/validate");

	private StageValidator() {
	}

	public record Result(boolean ok, List<String> messages) {
		public static Result ok(String msg) {
			return new Result(true, List.of(msg));
		}

		public static Result fail(List<String> messages) {
			return new Result(false, List.copyOf(messages));
		}

		public void sendTo(org.slf4j.Logger log) {
			for (String m : messages) {
				if (ok) {
					log.info(m);
				} else {
					log.warn(m);
				}
			}
		}
	}

	public static Result validateMainConfig(Path path) {
		List<String> msgs = new ArrayList<>();
		if (!Files.exists(path)) {
			return Result.fail(List.of("Main config not found: " + path + " (NeoForge may create it on first run)"));
		}
		try (CommentedFileConfig cfg = CommentedFileConfig.builder(path).sync().writingMode(WritingMode.REPLACE).build()) {
			cfg.load();
			msgs.add("Main config readable: " + path);
		} catch (Exception e) {
			return Result.fail(List.of("Main config invalid: " + e.getMessage()));
		}
		return new Result(true, msgs);
	}

	public static Result validateAll(Path stagesDir, MinecraftServer server) {
		List<String> msgs = new ArrayList<>();
		if (!Files.isDirectory(stagesDir)) {
			return Result.ok("No stages directory yet — nothing to validate.");
		}
		Map<String, Path> files = StageRegistry.discoverStageFiles(stagesDir);
		if (files.isEmpty()) {
			return Result.ok("No stage .toml files found.");
		}
		Map<String, StageDefinition> defs = new HashMap<>();
		boolean anyFail = false;
		for (Map.Entry<String, Path> e : files.entrySet()) {
			Result one = validateFile(e.getKey(), e.getValue(), server, false);
			msgs.addAll(one.messages());
			if (!one.ok()) {
				anyFail = true;
			} else {
				try (CommentedFileConfig cfg = CommentedFileConfig.builder(e.getValue()).sync().writingMode(WritingMode.REPLACE).build()) {
					cfg.load();
					defs.put(e.getKey(), StageTomlIo.parse(e.getKey(), cfg));
				} catch (Exception ex) {
					anyFail = true;
					msgs.add("Reload parse failed for " + e.getKey() + ": " + ex.getMessage());
				}
			}
		}
		Result cycles = detectCycles(defs);
		msgs.addAll(cycles.messages());
		if (!cycles.ok()) {
			anyFail = true;
		}
		return new Result(!anyFail, msgs);
	}

	public static Result validateOne(String stageId, Path stagesDir, MinecraftServer server) {
		Path file = stagesDir.resolve(stageId + ".toml");
		if (!Files.exists(file)) {
			return Result.fail(List.of("Stage file not found: " + file));
		}
		return validateFile(stageId, file, server, true);
	}

	private static Result validateFile(String stageId, Path path, MinecraftServer server, boolean logOk) {
		List<String> msgs = new ArrayList<>();
		try (CommentedFileConfig config = CommentedFileConfig.builder(path).sync().writingMode(WritingMode.REPLACE).build()) {
			config.load();
			StageDefinition def = StageTomlIo.parse(stageId, config);
			for (String dep : def.dependency()) {
				if (dep.isBlank()) {
					msgs.add("Empty dependency entry in " + stageId);
				}
			}
			if (server != null) {
				validateResources(def, server, msgs);
			}
			if (msgs.isEmpty()) {
				if (logOk) {
					msgs.add("Stage '" + stageId + "' is valid.");
				}
				return new Result(true, msgs);
			}
			return Result.fail(msgs);
		} catch (Exception e) {
			return Result.fail(List.of("Syntax/read error in " + path + ": " + e.getMessage()));
		}
	}

	private static void validateResources(StageDefinition def, MinecraftServer server, List<String> msgs) {
		validateGateLists(def.id(), def.locks(), server, msgs, "locks");
		validateGateLists(def.id(), def.unlocks(), server, msgs, "unlocks");
	}

	private static void validateGateLists(
		String stageId,
		StageGateLists lists,
		MinecraftServer server,
		List<String> msgs,
		String section
	) {
		for (ResourceLocation rl : lists.items()) {
			if (!BuiltInRegistries.ITEM.containsKey(rl)) {
				msgs.add("[" + stageId + "] " + section + " unknown item: " + rl);
			}
		}
		for (ResourceLocation rl : lists.fluids()) {
			if (!BuiltInRegistries.FLUID.containsKey(rl)) {
				msgs.add("[" + stageId + "] " + section + " unknown fluid: " + rl);
			}
		}
		for (ResourceLocation rl : lists.entities()) {
			if (!BuiltInRegistries.ENTITY_TYPE.containsKey(rl)) {
				msgs.add("[" + stageId + "] " + section + " unknown entity type: " + rl);
			}
		}
		var levelKeys = server.levelKeys();
		for (ResourceLocation rl : lists.dimensions()) {
			boolean found = false;
			for (var key : levelKeys) {
				if (key.location().equals(rl)) {
					found = true;
					break;
				}
			}
			if (!found) {
				msgs.add("[" + stageId + "] " + section + " dimension not loaded (may be dynamic): " + rl);
			}
		}
		for (String mod : lists.mods()) {
			if (mod.isBlank()) {
				msgs.add("[" + stageId + "] " + section + " empty mod id");
			} else if (!namespaceAppearsInRegistries(mod)) {
				msgs.add("[" + stageId + "] " + section + " mod namespace has no registered items/blocks/fluids: " + mod);
			}
		}
	}

	private static boolean namespaceAppearsInRegistries(String modId) {
		if (BuiltInRegistries.ITEM.keySet().stream().anyMatch(k -> k.getNamespace().equals(modId))) {
			return true;
		}
		if (BuiltInRegistries.BLOCK.keySet().stream().anyMatch(k -> k.getNamespace().equals(modId))) {
			return true;
		}
		return BuiltInRegistries.FLUID.keySet().stream().anyMatch(k -> k.getNamespace().equals(modId));
	}

	public static Result detectCycles(Map<String, StageDefinition> defs) {
		List<String> msgs = new ArrayList<>();
		Set<String> visiting = new HashSet<>();
		Set<String> done = new HashSet<>();
		for (String id : defs.keySet()) {
			if (findCycle(id, defs, visiting, done)) {
				return Result.fail(List.of("Dependency cycle detected in stage graph."));
			}
		}
		if (!defs.isEmpty()) {
			msgs.add("No dependency cycles detected (" + defs.size() + " stages).");
		}
		return new Result(true, msgs);
	}

	private static boolean findCycle(String id, Map<String, StageDefinition> defs, Set<String> visiting, Set<String> done) {
		if (done.contains(id)) {
			return false;
		}
		if (visiting.contains(id)) {
			return true;
		}
		visiting.add(id);
		StageDefinition def = defs.get(id);
		if (def != null) {
			for (String dep : def.dependency()) {
				if (dep.isBlank() || !defs.containsKey(dep)) {
					continue;
				}
				if (findCycle(dep, defs, visiting, done)) {
					return true;
				}
			}
		}
		visiting.remove(id);
		done.add(id);
		return false;
	}
}
