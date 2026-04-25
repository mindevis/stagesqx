package com.stagesqx.neoforge;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.stagesqx.stage.StageDefinition;
import com.stagesqx.stage.StageRegistry;
import com.stagesqx.stage.StageTomlIo;
import com.stagesqx.stage.StageValidator;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;

public final class StageCommands {
	private static final Logger LOGGER = LoggerFactory.getLogger("stagesqx/commands");

	private StageCommands() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext ctx, Commands.CommandSelection selection) {
		dispatcher.register(Commands.literal("stagesqx")
			.requires(s -> s.hasPermission(2))
			.then(Commands.literal("create")
				.then(Commands.argument("name", StringArgumentType.word())
					.executes(StageCommands::create)))
			.then(Commands.literal("delete")
				.then(Commands.argument("name", StringArgumentType.word())
					.executes(StageCommands::delete)))
			.then(Commands.literal("validate")
				.executes(StageCommands::validateAll)
				.then(Commands.literal("config")
					.executes(StageCommands::validateConfig))
				.then(Commands.argument("name", StringArgumentType.word())
					.executes(StageCommands::validateOne)))
			.then(Commands.literal("reload")
				.executes(StageCommands::reload))
			.then(Commands.literal("grant")
				.then(Commands.argument("player", StringArgumentType.string())
					.then(Commands.argument("stage", StringArgumentType.word())
						.executes(StageCommands::grant))))
			.then(Commands.literal("revoke")
				.then(Commands.argument("player", StringArgumentType.string())
					.then(Commands.argument("stage", StringArgumentType.word())
						.executes(StageCommands::revoke))))
			.then(Commands.literal("list")
				.then(Commands.literal("stages")
					.executes(StageCommands::listStages)
					.then(Commands.argument("player", StringArgumentType.string())
						.executes(StageCommands::listStagesForPlayer)))));
	}

	private static void logOp(String msg) {
		if (StagesQXModConfig.LOG_OPERATIONS.get()) {
			LOGGER.info("[StagesQX] {}", msg);
		}
	}

	private static int create(CommandContext<CommandSourceStack> ctx) {
		String name = StringArgumentType.getString(ctx, "name");
		if (!StageTomlIo.isValidStageName(name)) {
			ctx.getSource().sendFailure(Component.translatable("commands.stagesqx.create.invalid_name"));
			return 0;
		}
		Path dir = StageRegistry.getStagesDirectory();
		if (dir == null) {
			ctx.getSource().sendFailure(Component.literal("Stages directory not initialized."));
			return 0;
		}
		Path file = dir.resolve(name + ".toml");
		if (Files.exists(file)) {
			ctx.getSource().sendFailure(Component.translatable("commands.stagesqx.create.exists", name));
			return 0;
		}
		try {
			var content = StageTemplateFactory.collect(ctx.getSource().getServer());
			StageRegistry.writeTemplateFile(file, name, content);
			logOp("Created stage template file: " + file);
			ctx.getSource().sendSuccess(() -> Component.translatable("commands.stagesqx.create.ok", name), true);
			return 1;
		} catch (Exception e) {
			LOGGER.error("Create stage failed", e);
			ctx.getSource().sendFailure(Component.literal(e.getMessage()));
			return 0;
		}
	}

	private static int delete(CommandContext<CommandSourceStack> ctx) {
		String name = StringArgumentType.getString(ctx, "name");
		Path dir = StageRegistry.getStagesDirectory();
		Path file = dir.resolve(name + ".toml");
		if (!Files.exists(file)) {
			ctx.getSource().sendFailure(Component.translatable("commands.stagesqx.delete.missing", name));
			return 0;
		}
		try {
			Files.delete(file);
			PlayerStageStore.removeStageFromAll(ctx.getSource().getServer(), name);
			StageRegistry.reload(ctx.getSource().getServer());
			StageNetwork.broadcastCatalog(ctx.getSource().getServer());
			for (ServerPlayer p : ctx.getSource().getServer().getPlayerList().getPlayers()) {
				StageNetwork.syncPlayerStages(p);
			}
			logOp("Deleted stage " + name + " and reloaded.");
			ctx.getSource().sendSuccess(() -> Component.translatable("commands.stagesqx.delete.ok", name), true);
			ctx.getSource().sendSuccess(() -> Component.translatable("commands.stagesqx.delete.reload_hint"), false);
			return 1;
		} catch (Exception e) {
			ctx.getSource().sendFailure(Component.literal(e.getMessage()));
			return 0;
		}
	}

	private static int validateAll(CommandContext<CommandSourceStack> ctx) {
		Path dir = StageRegistry.getStagesDirectory();
		var res = StageValidator.validateAll(dir, ctx.getSource().getServer());
		for (String line : res.messages()) {
			ctx.getSource().sendSuccess(() -> Component.literal(line), false);
		}
		logOp("validate all: ok=" + res.ok());
		return res.ok() ? 1 : 0;
	}

	private static int validateConfig(CommandContext<CommandSourceStack> ctx) {
		Path cfg = net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get().resolve("stagesqx").resolve("stagesqx.toml");
		var res = StageValidator.validateMainConfig(cfg);
		for (String line : res.messages()) {
			ctx.getSource().sendSuccess(() -> Component.literal(line), false);
		}
		return res.ok() ? 1 : 0;
	}

	private static int validateOne(CommandContext<CommandSourceStack> ctx) {
		String name = StringArgumentType.getString(ctx, "name");
		if ("config".equals(name)) {
			return validateConfig(ctx);
		}
		Path dir = StageRegistry.getStagesDirectory();
		var res = StageValidator.validateOne(name, dir, ctx.getSource().getServer());
		for (String line : res.messages()) {
			if (res.ok()) {
				ctx.getSource().sendSuccess(() -> Component.literal(line), false);
			} else {
				ctx.getSource().sendFailure(Component.literal(line));
			}
		}
		return res.ok() ? 1 : 0;
	}

	private static int reload(CommandContext<CommandSourceStack> ctx) {
		StageRegistry.reload(ctx.getSource().getServer());
		StageNetwork.broadcastCatalog(ctx.getSource().getServer());
		for (ServerPlayer p : ctx.getSource().getServer().getPlayerList().getPlayers()) {
			StageNetwork.syncPlayerStages(p);
		}
		logOp("Configuration and stages reloaded.");
		ctx.getSource().sendSuccess(() -> Component.translatable("commands.stagesqx.reload.ok"), true);
		return 1;
	}

	private static int grant(CommandContext<CommandSourceStack> ctx) {
		String playerName = StringArgumentType.getString(ctx, "player");
		String stage = StringArgumentType.getString(ctx, "stage");
		ServerPlayer target = ctx.getSource().getServer().getPlayerList().getPlayerByName(playerName);
		if (target == null) {
			ctx.getSource().sendFailure(Component.translatable("commands.stagesqx.player_not_found", playerName));
			return 0;
		}
		var cat = StageRegistry.getCatalog();
		if (cat.get(stage) == null) {
			ctx.getSource().sendFailure(Component.translatable("commands.stagesqx.unknown_stage", stage));
			return 0;
		}
		var owned = PlayerStageStore.get(target);
		var missing = PlayerStageStore.missingDependencies(cat, owned, stage);
		if (!missing.isEmpty()) {
			ctx.getSource().sendFailure(Component.translatable("commands.stagesqx.grant.deps", String.join(", ", missing)));
			return 0;
		}
		if (owned.contains(stage)) {
			ctx.getSource().sendSuccess(() -> Component.translatable("commands.stagesqx.grant.already", playerName, stage), false);
			return 1;
		}
		PlayerStageStore.grant(target, stage);
		ctx.getSource().sendSuccess(() -> Component.translatable("commands.stagesqx.grant.ok", stage, playerName), true);
		return 1;
	}

	private static int revoke(CommandContext<CommandSourceStack> ctx) {
		String playerName = StringArgumentType.getString(ctx, "player");
		String stage = StringArgumentType.getString(ctx, "stage");
		ServerPlayer target = ctx.getSource().getServer().getPlayerList().getPlayerByName(playerName);
		if (target == null) {
			ctx.getSource().sendFailure(Component.translatable("commands.stagesqx.player_not_found", playerName));
			return 0;
		}
		PlayerStageStore.revoke(target, stage);
		ctx.getSource().sendSuccess(() -> Component.translatable("commands.stagesqx.revoke.ok", stage, playerName), true);
		return 1;
	}

	private static int listStages(CommandContext<CommandSourceStack> ctx) {
		var cat = StageRegistry.getCatalog();
		if (cat.stagesById().isEmpty()) {
			ctx.getSource().sendSuccess(() -> Component.translatable("commands.stagesqx.list.empty"), false);
			return 1;
		}
		for (StageDefinition def : cat.stagesById().values()) {
			String line = def.id() + " — " + def.effectiveDisplayName();
			ctx.getSource().sendSuccess(() -> Component.literal(line), false);
		}
		return 1;
	}

	private static int listStagesForPlayer(CommandContext<CommandSourceStack> ctx) {
		String playerName = StringArgumentType.getString(ctx, "player");
		ServerPlayer target = ctx.getSource().getServer().getPlayerList().getPlayerByName(playerName);
		if (target == null) {
			ctx.getSource().sendFailure(Component.translatable("commands.stagesqx.player_not_found", playerName));
			return 0;
		}
		var cat = StageRegistry.getCatalog();
		var have = PlayerStageStore.get(target);
		for (StageDefinition def : cat.stagesById().values()) {
			boolean ok = have.contains(def.id());
			ChatFormatting col = ok ? ChatFormatting.GREEN : ChatFormatting.GRAY;
			String mark = ok ? "\u2714" : "\u2718";
			final String msg = mark + " " + def.id() + " — " + def.effectiveDisplayName();
			ctx.getSource().sendSuccess(() -> Component.literal(msg).withStyle(col), false);
		}
		return 1;
	}
}
