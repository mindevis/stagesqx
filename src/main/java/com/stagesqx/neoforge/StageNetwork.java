package com.stagesqx.neoforge;

import com.stagesqx.StagesQXConstants;
import com.stagesqx.stage.StageCatalog;
import com.stagesqx.stage.StageCatalogSerialization;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.LinkedHashSet;
import java.util.Set;

public final class StageNetwork {
	public static final CustomPacketPayload.Type<SyncCatalogPayload> TYPE_CATALOG =
		new CustomPacketPayload.Type<>(StagesQXConstants.id("sync_catalog"));
	public static final CustomPacketPayload.Type<SyncOwnedPayload> TYPE_OWNED =
		new CustomPacketPayload.Type<>(StagesQXConstants.id("sync_owned"));

	public static final StreamCodec<RegistryFriendlyByteBuf, SyncCatalogPayload> CODEC_CATALOG =
		StreamCodec.composite(
			ByteBufCodecs.COMPOUND_TAG,
			SyncCatalogPayload::tag,
			SyncCatalogPayload::new
		);

	public static final StreamCodec<RegistryFriendlyByteBuf, SyncOwnedPayload> CODEC_OWNED =
		StreamCodec.composite(
			ByteBufCodecs.COMPOUND_TAG,
			SyncOwnedPayload::tag,
			SyncOwnedPayload::new
		);

	public record SyncCatalogPayload(CompoundTag tag) implements CustomPacketPayload {
		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE_CATALOG;
		}
	}

	public record SyncOwnedPayload(CompoundTag tag) implements CustomPacketPayload {
		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE_OWNED;
		}
	}

	private StageNetwork() {
	}

	public static void register(RegisterPayloadHandlersEvent event) {
		PayloadRegistrar reg = event.registrar(StagesQXConstants.MOD_ID);
		reg.playToClient(TYPE_CATALOG, CODEC_CATALOG, StageNetwork::handleCatalogClient);
		reg.playToClient(TYPE_OWNED, CODEC_OWNED, StageNetwork::handleOwnedClient);
	}

	private static void handleCatalogClient(SyncCatalogPayload payload, net.neoforged.neoforge.network.handling.IPayloadContext ctx) {
		ctx.enqueueWork(() -> ClientStageData.acceptCatalog(StageCatalogSerialization.readCatalog(payload.tag())));
	}

	private static void handleOwnedClient(SyncOwnedPayload payload, net.neoforged.neoforge.network.handling.IPayloadContext ctx) {
		var tag = payload.tag();
		boolean hideNames = tag != null && tag.getBoolean("hide_stage_names");
		ctx.enqueueWork(() -> ClientStageData.acceptOwned(readOwned(tag), hideNames));
	}

	public static void syncCatalog(ServerPlayer player) {
		CompoundTag tag = StageCatalogSerialization.writeCatalog(com.stagesqx.stage.StageRegistry.getCatalog());
		PacketDistributor.sendToPlayer(player, new SyncCatalogPayload(tag));
	}

	public static void syncPlayerStages(ServerPlayer player) {
		CompoundTag tag = writeOwned(PlayerStageStore.get(player));
		PacketDistributor.sendToPlayer(player, new SyncOwnedPayload(tag));
	}

	public static void syncAllTo(ServerPlayer player) {
		syncCatalog(player);
		syncPlayerStages(player);
	}

	public static void broadcastCatalog(net.minecraft.server.MinecraftServer server) {
		CompoundTag tag = StageCatalogSerialization.writeCatalog(com.stagesqx.stage.StageRegistry.getCatalog());
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			PacketDistributor.sendToPlayer(p, new SyncCatalogPayload(tag));
		}
	}

	public static CompoundTag writeOwned(Set<String> stages) {
		ListTag list = new ListTag();
		for (String s : stages.stream().sorted().toList()) {
			list.add(StringTag.valueOf(s));
		}
		CompoundTag root = new CompoundTag();
		root.put("stages", list);
		root.putBoolean("hide_stage_names", StagesQXModConfig.REVEAL_STAGE_NAMES_ONLY_TO_OPERATORS.get());
		return root;
	}

	public static Set<String> readOwned(CompoundTag root) {
		Set<String> out = new LinkedHashSet<>();
		if (root == null || !root.contains("stages")) {
			return out;
		}
		ListTag list = root.getList("stages", Tag.TAG_STRING);
		for (int i = 0; i < list.size(); i++) {
			String s = list.getString(i);
			if (!s.isBlank()) {
				out.add(s);
			}
		}
		return out;
	}

	public static Set<String> clientStagesFor(Player player) {
		if (player instanceof ServerPlayer sp) {
			return PlayerStageStore.get(sp);
		}
		return ClientStageData.getOwnedStages();
	}
}
