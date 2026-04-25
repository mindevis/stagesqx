package com.stagesqx.neoforge;

import com.stagesqx.stage.StageCatalog;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Client-side mirror of the stage catalog and the local player's granted stages.
 */
public final class ClientStageData {
	private static final AtomicReference<StageCatalog> CATALOG = new AtomicReference<>(StageCatalog.empty());
	private static volatile Set<String> OWNED = Set.of();
	/** Mirrored from server {@link StagesQXModConfig#REVEAL_STAGE_NAMES_ONLY_TO_OPERATORS}. */
	private static volatile boolean hideStageNamesFromNonOps = false;

	private ClientStageData() {
	}

	public static void acceptCatalog(StageCatalog catalog) {
		CATALOG.set(catalog);
		StagesQXClientHooks.onCatalogUpdated();
	}

	/**
	 * @param hideStageNamesFromNonOpsServer {@code hide_stage_names} from {@link StageNetwork#writeOwned(java.util.Set)}
	 */
	public static void acceptOwned(Set<String> owned, boolean hideStageNamesFromNonOpsServer) {
		OWNED = Collections.unmodifiableSet(new java.util.LinkedHashSet<>(owned));
		hideStageNamesFromNonOps = hideStageNamesFromNonOpsServer;
		StagesQXClientHooks.onCatalogUpdated();
	}

	public static StageCatalog getCatalog() {
		return CATALOG.get();
	}

	public static Set<String> getOwnedStages() {
		return OWNED;
	}

	/**
	 * When true, clients without operator permission should not show restricting stage names in tooltips.
	 */
	public static boolean hideStageNamesFromNonOps() {
		return hideStageNamesFromNonOps;
	}
}
