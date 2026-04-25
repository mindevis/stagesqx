package com.stagesqx.neoforge;

import com.stagesqx.stage.StageCatalog;
import com.stagesqx.stage.StageDefinition;
import com.stagesqx.stage.StageTomlIo;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class PlayerStageStore {
	private static final String ROOT = "stagesqx";
	private static final String STAGES = "granted_stages";

	private PlayerStageStore() {
	}

	public static Set<String> get(ServerPlayer player) {
		var root = player.getPersistentData().getCompound(ROOT);
		if (!root.contains(STAGES)) {
			return Set.of();
		}
		ListTag list = root.getList(STAGES, Tag.TAG_STRING);
		Set<String> out = new LinkedHashSet<>();
		for (int i = 0; i < list.size(); i++) {
			String s = list.getString(i);
			if (!s.isBlank()) {
				out.add(s);
			}
		}
		return out;
	}

	public static void set(ServerPlayer player, Set<String> stages) {
		var root = new net.minecraft.nbt.CompoundTag();
		ListTag list = new ListTag();
		for (String s : stages.stream().sorted().toList()) {
			list.add(StringTag.valueOf(s));
		}
		root.put(STAGES, list);
		player.getPersistentData().put(ROOT, root);
	}

	public static void grant(ServerPlayer player, String stageId) {
		Set<String> next = new LinkedHashSet<>(get(player));
		next.add(stageId);
		set(player, next);
		StageNetwork.syncPlayerStages(player);
	}

	public static void revoke(ServerPlayer player, String stageId) {
		Set<String> next = new LinkedHashSet<>(get(player));
		next.remove(stageId);
		set(player, next);
		StageNetwork.syncPlayerStages(player);
	}

	public static void removeStageFromAll(net.minecraft.server.MinecraftServer server, String stageId) {
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			Set<String> cur = get(p);
			if (cur.contains(stageId)) {
				Set<String> next = cur.stream().filter(s -> !s.equals(stageId)).collect(Collectors.toCollection(LinkedHashSet::new));
				set(p, next);
				StageNetwork.syncPlayerStages(p);
			}
		}
	}

	public static boolean dependenciesMet(StageCatalog catalog, Set<String> playerStages, String stageId) {
		StageDefinition def = catalog.get(stageId);
		if (def == null) {
			return false;
		}
		for (String d : def.dependency()) {
			if (d.isBlank()) {
				continue;
			}
			if (!playerStages.contains(d)) {
				return false;
			}
		}
		return true;
	}

	public static Set<String> missingDependencies(StageCatalog catalog, Set<String> playerStages, String stageId) {
		StageDefinition def = catalog.get(stageId);
		if (def == null) {
			return Set.of(stageId);
		}
		Set<String> miss = new LinkedHashSet<>();
		for (String d : def.dependency()) {
			if (!d.isBlank() && !playerStages.contains(d)) {
				miss.add(d);
			}
		}
		return miss;
	}

	public static void replaceAll(ServerPlayer player, Collection<String> stages) {
		set(player, new LinkedHashSet<>(stages));
		StageNetwork.syncPlayerStages(player);
	}

	/**
	 * Grants every id in {@link StagesQXModConfig#STARTING_STAGES} on login. While a stage id is present,
	 * that stage file's {@code [locks]} restrict the player; revoking the id removes those restrictions.
	 * Empty list → new players have no restricting stages until you /grant or another system adds one.
	 */
	public static void applyStartingStagesFromConfig(ServerPlayer player) {
		List<? extends String> raw = StagesQXModConfig.STARTING_STAGES.get();
		if (raw == null || raw.isEmpty()) {
			return;
		}
		Set<String> next = new LinkedHashSet<>(get(player));
		boolean changed = false;
		for (String s : raw) {
			if (s == null) {
				continue;
			}
			String t = s.trim();
			if (t.isEmpty() || !StageTomlIo.isValidStageName(t)) {
				continue;
			}
			if (next.add(t)) {
				changed = true;
			}
		}
		if (changed) {
			replaceAll(player, next);
		}
	}
}
