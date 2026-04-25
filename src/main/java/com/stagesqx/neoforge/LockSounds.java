package com.stagesqx.neoforge;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Optional lock denial sound (server notifies the client), with a short per-player cooldown to limit spam.
 */
public final class LockSounds {
	private static final Map<UUID, Long> LAST_PLAY_TICK = new ConcurrentHashMap<>();
	private static final long COOLDOWN_TICKS = 4L;

	private LockSounds() {
	}

	public static void tryPlayFor(ServerPlayer player) {
		if (!StagesQXModConfig.PLAY_LOCK_SOUND.get()) {
			return;
		}
		long now = player.level().getGameTime();
		UUID uuid = player.getUUID();
		long last = LAST_PLAY_TICK.getOrDefault(uuid, Long.MIN_VALUE);
		if (now - last < COOLDOWN_TICKS) {
			return;
		}
		LAST_PLAY_TICK.put(uuid, now);
		SoundEvent sound = resolveSound();
		if (sound == null) {
			return;
		}
		float volume = StagesQXModConfig.LOCK_SOUND_VOLUME.get().floatValue();
		float pitch = StagesQXModConfig.LOCK_SOUND_PITCH.get().floatValue();
		player.playNotifySound(sound, SoundSource.MASTER, volume, pitch);
	}

	private static SoundEvent resolveSound() {
		String raw = StagesQXModConfig.LOCK_SOUND.get();
		if (raw == null || raw.isBlank()) {
			return null;
		}
		try {
			ResourceLocation id = ResourceLocation.parse(raw.trim());
			return BuiltInRegistries.SOUND_EVENT.get(id);
		} catch (Exception e) {
			return null;
		}
	}
}
