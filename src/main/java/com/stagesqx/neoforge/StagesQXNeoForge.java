package com.stagesqx.neoforge;

import com.stagesqx.StagesQXConstants;
import com.stagesqx.neoforge.integration.ftbquests.StagesQXFtbLibraryStageProvider;
import com.stagesqx.stage.StageRegistry;
import net.neoforged.bus.api.IEventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@Mod(StagesQXConstants.MOD_ID)
public final class StagesQXNeoForge {
	private static final Logger LOGGER = LoggerFactory.getLogger("stagesqx");

	public StagesQXNeoForge(IEventBus modBus, ModContainer modContainer) {
		modContainer.registerConfig(ModConfig.Type.COMMON, StagesQXModConfig.SPEC, "stagesqx/stagesqx.toml");

		PathSetup.init();

		modBus.addListener(this::commonSetup);
		modBus.addListener(StageNetwork::register);

		NeoForge.EVENT_BUS.addListener(this::registerCommands);
		NeoForge.EVENT_BUS.addListener(this::serverAboutToStart);

		if (FMLEnvironment.dist.isClient()) {
			StagesQXNeoForgeClient.init(modBus);
		}
	}

	private void commonSetup(FMLCommonSetupEvent event) {
		event.enqueueWork(() -> {
			StageRegistry.bootstrapFromDisk();
			if (net.neoforged.fml.ModList.get().isLoaded("curios")) {
				try {
					Class.forName("com.stagesqx.neoforge.integration.curios.CuriosStages")
						.getMethod("register")
						.invoke(null);
				} catch (Throwable t) {
					LOGGER.warn("StagesQX Curios integration failed: {}", t.toString());
				}
			}
			if (net.neoforged.fml.ModList.get().isLoaded("ftbquests") && net.neoforged.fml.ModList.get().isLoaded("ftblibrary")) {
				StagesQXFtbLibraryStageProvider.tryRegister();
			}
		});
	}

	private void registerCommands(RegisterCommandsEvent event) {
		StageCommands.register(event.getDispatcher(), event.getBuildContext(), event.getCommandSelection());
	}

	private void serverAboutToStart(ServerAboutToStartEvent event) {
		StageRegistry.reload(event.getServer());
	}

	public static final class PathSetup {
		private PathSetup() {
		}

		static void init() {
			var root = FMLPaths.CONFIGDIR.get().resolve("stagesqx");
			StageRegistry.setStagesDirectory(root);
			StageRegistry.setTriggersDirectory(root);
			try {
				java.nio.file.Files.createDirectories(root);
			} catch (java.io.IOException ignored) {
			}
		}
	}
}
