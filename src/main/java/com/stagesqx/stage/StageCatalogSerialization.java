package com.stagesqx.stage;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class StageCatalogSerialization {
	private StageCatalogSerialization() {
	}

	public static CompoundTag writeCatalog(StageCatalog catalog) {
		ListTag list = new ListTag();
		for (StageDefinition def : catalog.stagesById().values()) {
			list.add(writeStage(def));
		}
		CompoundTag root = new CompoundTag();
		root.put("stages", list);
		return root;
	}

	public static StageCatalog readCatalog(CompoundTag root) {
		if (root == null || !root.contains("stages")) {
			return StageCatalog.empty();
		}
		ListTag list = root.getList("stages", Tag.TAG_COMPOUND);
		Map<String, StageDefinition> map = new LinkedHashMap<>();
		for (int i = 0; i < list.size(); i++) {
			StageDefinition def = readStage(list.getCompound(i));
			map.put(def.id(), def);
		}
		return new StageCatalog(map);
	}

	public static ListTag writeStringList(Set<String> set) {
		ListTag list = new ListTag();
		for (String s : set.stream().sorted().toList()) {
			list.add(StringTag.valueOf(s));
		}
		return list;
	}

	public static Set<String> readStringList(ListTag list) {
		Set<String> out = new LinkedHashSet<>();
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i) instanceof StringTag st) {
				out.add(st.getAsString());
			}
		}
		return out;
	}

	private static CompoundTag writeStage(StageDefinition def) {
		CompoundTag t = new CompoundTag();
		t.putString("id", def.id());
		t.putString("displayName", def.displayName());
		t.putString("description", def.description());
		t.putString("icon", def.icon());
		t.putString("unlockMessage", def.unlockMessage());
		ListTag dep = new ListTag();
		for (String d : def.dependency()) {
			dep.add(StringTag.valueOf(d));
		}
		t.put("dependency", dep);
		t.putBoolean("minecraft", def.minecraft());
		t.put("locks", writeGateLists(def.locks()));
		t.put("unlocks", writeGateLists(def.unlocks()));
		return t;
	}

	private static CompoundTag writeGateLists(StageGateLists g) {
		CompoundTag t = new CompoundTag();
		t.put("items", writeRlList(g.items()));
		ListTag mods = new ListTag();
		for (String m : g.mods().stream().sorted().toList()) {
			mods.add(StringTag.valueOf(m));
		}
		t.put("mods", mods);
		t.put("fluids", writeRlList(g.fluids()));
		t.put("dimensions", writeRlList(g.dimensions()));
		t.put("entities", writeRlList(g.entities()));
		return t;
	}

	private static ListTag writeRlList(Set<ResourceLocation> rls) {
		ListTag list = new ListTag();
		for (ResourceLocation rl : rls.stream().sorted(Comparator.comparing(ResourceLocation::toString)).toList()) {
			list.add(StringTag.valueOf(rl.toString()));
		}
		return list;
	}

	private static Set<ResourceLocation> readRlList(ListTag list) {
		Set<ResourceLocation> out = new LinkedHashSet<>();
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i) instanceof StringTag st) {
				try {
					out.add(ResourceLocation.parse(st.getAsString()));
				} catch (Exception ignored) {
				}
			}
		}
		return out;
	}

	private static StageGateLists readGateLists(CompoundTag t) {
		if (t == null) {
			return StageGateLists.empty();
		}
		return new StageGateLists(
			readRlList(t.getList("items", Tag.TAG_STRING)),
			readStringList(t.getList("mods", Tag.TAG_STRING)),
			readRlList(t.getList("fluids", Tag.TAG_STRING)),
			readRlList(t.getList("dimensions", Tag.TAG_STRING)),
			readRlList(t.getList("entities", Tag.TAG_STRING))
		);
	}

	private static StageDefinition readStage(CompoundTag t) {
		String id = t.getString("id");
		List<String> dep = new ArrayList<>();
		ListTag depTag = t.getList("dependency", Tag.TAG_STRING);
		for (int i = 0; i < depTag.size(); i++) {
			dep.add(depTag.getString(i));
		}
		boolean minecraft = t.contains("minecraft", Tag.TAG_BYTE) && t.getBoolean("minecraft");
		if (t.contains("locks", Tag.TAG_COMPOUND)) {
			StageGateLists locks = readGateLists(t.getCompound("locks"));
			StageGateLists unlocks = t.contains("unlocks", Tag.TAG_COMPOUND)
				? readGateLists(t.getCompound("unlocks"))
				: StageGateLists.empty();
			return new StageDefinition(
				id,
				t.contains("displayName") ? t.getString("displayName") : "",
				t.contains("description") ? t.getString("description") : "",
				t.contains("icon") ? t.getString("icon") : "",
				t.contains("unlockMessage") ? t.getString("unlockMessage") : "",
				dep,
				minecraft,
				locks,
				unlocks
			);
		}
		StageGateLists locks = new StageGateLists(
			readRlList(t.getList("items", Tag.TAG_STRING)),
			readStringList(t.getList("mods", Tag.TAG_STRING)),
			readRlList(t.getList("fluids", Tag.TAG_STRING)),
			readRlList(t.getList("dimensions", Tag.TAG_STRING)),
			readRlList(t.getList("entities", Tag.TAG_STRING))
		);
		return new StageDefinition(
			id,
			t.contains("displayName") ? t.getString("displayName") : "",
			t.contains("description") ? t.getString("description") : "",
			t.contains("icon") ? t.getString("icon") : "",
			t.contains("unlockMessage") ? t.getString("unlockMessage") : "",
			dep,
			minecraft,
			locks,
			StageGateLists.empty()
		);
	}
}
