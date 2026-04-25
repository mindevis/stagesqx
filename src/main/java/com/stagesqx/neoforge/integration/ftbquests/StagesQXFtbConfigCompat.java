package com.stagesqx.neoforge.integration.ftbquests;

import com.stagesqx.neoforge.StagesQXModConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * FTB Quests config UI uses reflection-friendly config groups that may change method signatures across versions.
 * We keep integrations compile-only by interacting through reflection.
 */
public final class StagesQXFtbConfigCompat {
	private static final Logger LOG = LoggerFactory.getLogger("stagesqx/ftbquests/config");

	private StagesQXFtbConfigCompat() {
	}

	public static void tryAddRequiredStageString(Object configGroup, String currentValue, Consumer<String> setter) {
		if (configGroup == null) {
			return;
		}
		try {
			Object qx = getOrCreateSubgroup(configGroup, "stagesqx");
			if (qx == null) {
				return;
			}
			tryInvoke(qx, "setNameKey", new Class<?>[]{String.class}, new Object[]{"StagesQX"});
			addStringEntry(qx, "required_stage", currentValue, setter, "");
		} catch (Throwable t) {
			if (StagesQXModConfig.DEBUG.get()) {
				LOG.warn("Failed to add required_stage config entry (FTB Quests GUI signature mismatch?): {}", t.toString());
			}
		}
	}

	private static Object getOrCreateSubgroup(Object config, String key) {
		Object out = tryInvoke(config, "getOrCreateSubgroup", new Class<?>[]{String.class}, new Object[]{key});
		if (out != null) {
			return out;
		}
		out = tryInvoke(config, "getOrCreateGroup", new Class<?>[]{String.class}, new Object[]{key});
		if (out != null) {
			return out;
		}
		out = tryInvoke(config, "getOrCreate", new Class<?>[]{String.class}, new Object[]{key});
		return out;
	}

	private static void addStringEntry(Object group, String key, String currentValue, Consumer<String> setter, String def) {
		// Common signature in some FTB Library versions: addString(String, String, Consumer<String>, String)
		if (tryInvoke(group, "addString",
			new Class<?>[]{String.class, String.class, Consumer.class, String.class},
			new Object[]{key, currentValue, setter, def}) != null) {
			return;
		}

		// Another common signature: addString(String, Supplier<String>, Consumer<String>, String)
		Supplier<String> getter = () -> currentValue;
		if (tryInvoke(group, "addString",
			new Class<?>[]{String.class, Supplier.class, Consumer.class, String.class},
			new Object[]{key, getter, setter, def}) != null) {
			return;
		}

		// Fallback: search any addString overload with (String, ?, Consumer, String)
		for (Method m : group.getClass().getMethods()) {
			if (!"addString".equals(m.getName())) {
				continue;
			}
			Class<?>[] p = m.getParameterTypes();
			if (p.length != 4) {
				continue;
			}
			if (p[0] != String.class || p[3] != String.class) {
				continue;
			}
			if (!Consumer.class.isAssignableFrom(p[2])) {
				continue;
			}
			try {
				Object arg1;
				if (p[1] == String.class) {
					arg1 = currentValue;
				} else if (Supplier.class.isAssignableFrom(p[1])) {
					arg1 = (Supplier<String>) () -> currentValue;
				} else {
					continue;
				}
				m.invoke(group, key, arg1, setter, def);
				return;
			} catch (Throwable ignored) {
			}
		}
	}

	private static Object tryInvoke(Object target, String method, Class<?>[] paramTypes, Object[] args) {
		try {
			Method m = target.getClass().getMethod(method, paramTypes);
			return m.invoke(target, args);
		} catch (Throwable ignored) {
			return null;
		}
	}
}

