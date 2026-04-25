# [1.2.0](https://github.com/mindevis/stagesqx/compare/v1.1.0...v1.2.0) (2026-04-25)


### Features

* **config:** keep stages and triggers in config/stagesqx ([c30e309](https://github.com/mindevis/stagesqx/commit/c30e30979c1169356b2d980e90e0be0200797ffd))

# [1.1.0](https://github.com/mindevis/stagesqx/compare/v1.0.2...v1.1.0) (2026-04-25)


### Features

* **config:** load stages from config/stagesqx/stages ([7deb117](https://github.com/mindevis/stagesqx/commit/7deb1174332030d7c7c0d4c19f07e74d9d7a361c))

## [1.0.1](https://github.com/mindevis/StagesQX/compare/v1.0.0...v1.0.1) (2026-04-25)


### Bug Fixes

* **ci:** support master branch in release push script ([1598b9f](https://github.com/mindevis/StagesQX/commit/1598b9fa2a5e96e0f485b90453d8858d05f1a4f0))

# 1.0.0 (2026-04-25)


### Bug Fixes

* EMI not syncing after stage grant/revoke until /reload ([c3aebf6](https://github.com/mindevis/StagesQX/commit/c3aebf6195915869da7aa6d2636342fcd6e71277))
* FTB Quests stage rewards not granting stages at all ([fbbdebc](https://github.com/mindevis/StagesQX/commit/fbbdebc594c8969b71807b1a35410a6aa7add21a))
* FTB Quests team rewards not syncing stages to EMI ([b6fc804](https://github.com/mindevis/StagesQX/commit/b6fc804f419d784f2b8c3c69bc7445a3388746cf))


### Features

* fix hotbar enforcement, add recipe-only locks, add per-stage enforcement exceptions ([4301597](https://github.com/mindevis/StagesQX/commit/43015978d6997de4c5d4930cb4e6cbf976a49ab3))
* initialize StagesQX fork ([247e636](https://github.com/mindevis/StagesQX/commit/247e636500a60f6bc89c6de2ad04d694e0b65eed))
* make all player-facing text configurable via progressivestages.toml ([1a2a46b](https://github.com/mindevis/StagesQX/commit/1a2a46b7394478de7ef7363304e63b816befd62d))
* rename config to progressivestages.toml, add soft item enforcement options, fix left-click gap ([29506da](https://github.com/mindevis/StagesQX/commit/29506da1b4e23de56ed5db0883a46d8e2ba47825))
* **v1.4:** make EVERY text message configurable with & color code support ([904d64b](https://github.com/mindevis/StagesQX/commit/904d64bd321bc16834f1f6ad00586a4387686c2f))

# [2.1.0](https://github.com/mindevis/stagesqx/compare/v2.0.1...v2.1.0) (2026-04-25)


### Features

* **ftbquests:** register FTB Library stage provider ([55a2871](https://github.com/mindevis/stagesqx/commit/55a287118d3ff03fc695095e3e69dce910adfeaf))

## [2.0.1](https://github.com/mindevis/stagesqx/compare/v2.0.0...v2.0.1) (2026-04-25)


### Bug Fixes

* **ftbquests:** show required_stage in editor across versions ([0e0eb19](https://github.com/mindevis/stagesqx/commit/0e0eb19f359e9a1562f695d61bedb683b67cd4c3))

# [2.0.0](https://github.com/mindevis/stagesqx/compare/v1.8.1...v2.0.0) (2026-04-25)


### Features

* require stage to unlock content ([ef1a5dd](https://github.com/mindevis/stagesqx/commit/ef1a5dd3a6565e0d089fcbf04c7f007d8a92fcec))


### BREAKING CHANGES

* Content listed under a stage's [locks] now requires that stage (missing stage => blocked). Packs authored for the old 'locks apply while stage is active' behavior must be updated.

Made-with: Cursor

# [Unreleased]

### ⚠ Breaking changes

* **stagesqx:** invert stage gating semantics — content listed under a stage's `[locks]` now **requires** that stage (missing stage => blocked). Packs authored for the previous "locks apply while stage is active" model must be updated.
* **stagesqx:** this is a breaking progression-model change; update all stage TOML files authored for the old "locks apply while stage is active" behavior.

## [1.8.1](https://github.com/mindevis/stagesqx/compare/v1.8.0...v1.8.1) (2026-04-24)


### Bug Fixes

* show FTB Quests required_stage in GUI when available ([7179f8d](https://github.com/mindevis/stagesqx/commit/7179f8ddc39c328cad319fde7eddf81e4900f8f6))

# [1.8.0](https://github.com/mindevis/stagesqx/compare/v1.7.4...v1.8.0) (2026-04-24)


### Features

* add FTB Quests stage-gated chapters and quests ([697bf50](https://github.com/mindevis/stagesqx/commit/697bf509f0dc1b9f875684a79469812c52adb48f))

## [1.7.4](https://github.com/mindevis/stagesqx/compare/v1.7.3...v1.7.4) (2026-04-24)


### Bug Fixes

* respect vanilla block locks for visual workbench replacement tables ([c2b0dfc](https://github.com/mindevis/stagesqx/commit/c2b0dfcb2154d219ffaf5832a3b5116ac94d4122))

## [1.7.3](https://github.com/mindevis/stagesqx/compare/v1.7.2...v1.7.3) (2026-04-24)


### Bug Fixes

* block use and mining of stage-locked blocks (empty hand gui open) ([dc186fe](https://github.com/mindevis/stagesqx/commit/dc186feac8e8cafebb8090d5b0f4d301d87c4c38))

## [1.7.2](https://github.com/mindevis/stagesqx/compare/v1.7.1...v1.7.2) (2026-04-24)


### Bug Fixes

* block left/right click use of stage-locked items on blocks and entities ([cab50c5](https://github.com/mindevis/stagesqx/commit/cab50c51b47fd6e24403cd9f90da250101f96b25))

## [1.7.1](https://github.com/mindevis/stagesqx/compare/v1.7.0...v1.7.1) (2026-04-24)


### Bug Fixes

* use mutable set when merging fluid and item gating stages ([2f97bf0](https://github.com/mindevis/stagesqx/commit/2f97bf08b82c46c8b86446ce62ea224cfe3c427d))

# [1.7.0](https://github.com/mindevis/stagesqx/compare/v1.6.0...v1.7.0) (2026-04-24)


### Features

* **config:** emi debug snapshot logging and shared explain helpers ([4a642bc](https://github.com/mindevis/stagesqx/commit/4a642bc30a08a94657382b96f08fef6569003e23))

# [1.6.0](https://github.com/mindevis/stagesqx/compare/v1.5.5...v1.6.0) (2026-04-24)


### Features

* **config:** debug flag and jei gating diagnostics log ([3875039](https://github.com/mindevis/stagesqx/commit/387503949683ec38723121ecdc5b807697b9b201))

## [1.5.5](https://github.com/mindevis/stagesqx/compare/v1.5.4...v1.5.5) (2026-04-24)


### Bug Fixes

* **jei:** block mod fluids from jei uid and non-item ingredient types ([70f7465](https://github.com/mindevis/stagesqx/commit/70f74653c12de67d37bf07e82f73438471995d2c))

## [1.5.4](https://github.com/mindevis/stagesqx/compare/v1.5.3...v1.5.4) (2026-04-24)


### Bug Fixes

* **jei:** hide mod fluids by jei id and display mod (create potions) ([a28e54f](https://github.com/mindevis/stagesqx/commit/a28e54f5ba6d549f7bbaf60a58b0dd66dd26fd00))

## [1.5.3](https://github.com/mindevis/stagesqx/compare/v1.5.2...v1.5.3) (2026-04-24)


### Bug Fixes

* **jei:** remove blocked stacks from ingredient manager and rebuild filter ([4b391eb](https://github.com/mindevis/stagesqx/commit/4b391ebf866c85304be6fc1731b786642d45a6fd))

## [1.5.2](https://github.com/mindevis/stagesqx/compare/v1.5.1...v1.5.2) (2026-04-24)


### Bug Fixes

* **jei:** hide staged items in list and skip lock overlay in jei ui ([01147e1](https://github.com/mindevis/stagesqx/commit/01147e15f1b84284710d7cd4326635251c357217))

## [1.5.1](https://github.com/mindevis/stagesqx/compare/v1.5.0...v1.5.1) (2026-04-24)


### Bug Fixes

* **jei:** stabilize stage hides after JEI bulk ingredient adds ([a4ecaea](https://github.com/mindevis/stagesqx/commit/a4ecaeade3881b62850addde9e3eefc59c7000ad))

# [1.5.0](https://github.com/mindevis/stagesqx/compare/v1.4.4...v1.5.0) (2026-04-23)


### Features

* hide restricting stage names from non-operator players ([83f27ad](https://github.com/mindevis/stagesqx/commit/83f27adc094c56ad69e399781a9fe05c4fa8ac53))

## [1.4.4](https://github.com/mindevis/stagesqx/compare/v1.4.3...v1.4.4) (2026-04-23)


### Bug Fixes

* **jei:** hide staged ingredients via JEI api blacklist ([6be78dc](https://github.com/mindevis/stagesqx/commit/6be78dced2ad4df5e6147a4b8a4f9592797c1223))

## [1.4.3](https://github.com/mindevis/stagesqx/compare/v1.4.2...v1.4.3) (2026-04-23)


### Bug Fixes

* **stagesqx:** hide JEI fluids for all FluidStack ingredient types ([e320492](https://github.com/mindevis/stagesqx/commit/e3204927d561ab725c7a62ef28b8fd322305c289))

## [1.4.2](https://github.com/mindevis/stagesqx/compare/v1.4.1...v1.4.2) (2026-04-23)


### Bug Fixes

* **stagesqx:** read minecraft flag under [locks]; hide EMI fluid-only stacks ([3763d12](https://github.com/mindevis/stagesqx/commit/3763d12dafe5926a736d673057893e1cceeea000))

## [1.4.1](https://github.com/mindevis/stagesqx/compare/v1.4.0...v1.4.1) (2026-04-23)


### Bug Fixes

* **stagesqx:** inherit minecraft=true locks for vanilla namespace with child stages ([0769c55](https://github.com/mindevis/stagesqx/commit/0769c558e5f5c70ffea90e362c8807239b032e68))

# [1.4.0](https://github.com/mindevis/stagesqx/compare/v1.3.0...v1.4.0) (2026-04-23)


### Features

* **stagesqx:** effective lock stages for dependencies and JEI ([828d8c4](https://github.com/mindevis/stagesqx/commit/828d8c4cb0ff0dde26f71f76ff1f7b1ec192b9fd))

# [1.3.0](https://github.com/mindevis/stagesqx/compare/v1.2.2...v1.3.0) (2026-04-23)


### Features

* **stagesqx:** apply [locks] while player has the stage (restrict-then-revoke) ([ccc2d81](https://github.com/mindevis/stagesqx/commit/ccc2d811a07947a3633d18fadb232b1d468df53f))

## [1.2.2](https://github.com/mindevis/stagesqx/compare/v1.2.1...v1.2.2) (2026-04-23)


### Bug Fixes

* **config:** clarify starting_stages grants unlocks; add eject blocked inventory ([2e9d12b](https://github.com/mindevis/stagesqx/commit/2e9d12b9da95c45b648feadd4d300eaede50958a))

## [1.2.1](https://github.com/mindevis/stagesqx/compare/v1.2.0...v1.2.1) (2026-04-23)


### Bug Fixes

* **stagesqx:** read [unlocks] when using legacy root lock keys ([3a2b235](https://github.com/mindevis/stagesqx/commit/3a2b2354d5aa3cf65849c22718a820a0db7fd720))

# [1.2.0](https://github.com/mindevis/stagesqx/compare/v1.1.0...v1.2.0) (2026-04-23)


### Features

* **stagesqx:** add locks/unlocks tables and minecraft gate in stage toml ([2d96a9a](https://github.com/mindevis/stagesqx/commit/2d96a9a2a3abb94c9735820f9ba7bc77d47b4a56))

# [1.1.0](https://github.com/mindevis/stagesqx/compare/v1.0.8...v1.1.0) (2026-04-23)


### Features

* **config:** starting_stages on login and lock sound options ([fa3cf1f](https://github.com/mindevis/stagesqx/commit/fa3cf1fced00f66bcce69cf061fa28fcc1a059d8))

## [1.0.8](https://github.com/mindevis/stagesqx/compare/v1.0.7...v1.0.8) (2026-04-23)


### Bug Fixes

* **client:** two-line blocked tooltip (red title + required stage) ([77d576b](https://github.com/mindevis/stagesqx/commit/77d576b99a65b272737a086cc1dde4ca090eaa51))

## [1.0.7](https://github.com/mindevis/stagesqx/compare/v1.0.6...v1.0.7) (2026-04-23)


### Bug Fixes

* **client:** draw lock overlay above item icon (gui z-order) ([fff5629](https://github.com/mindevis/stagesqx/commit/fff562946d55953a91e1708f02d836742539c256))

## [1.0.6](https://github.com/mindevis/stagesqx/compare/v1.0.5...v1.0.6) (2026-04-23)


### Bug Fixes

* **jei,emi:** hide library ingredients (e.g. experiencelib) via display mod id ([aea879c](https://github.com/mindevis/stagesqx/commit/aea879ca2b2dc81d86a0cdddca17e43ff54c5469))

## [1.0.5](https://github.com/mindevis/stagesqx/compare/v1.0.4...v1.0.5) (2026-04-23)


### Bug Fixes

* **jei,emi:** hide mod-gated custom ingredients (gases, pigments, etc.) ([abe673f](https://github.com/mindevis/stagesqx/commit/abe673ffefdd16b2714a581407a309d8d8cfafcc))

## [1.0.4](https://github.com/mindevis/stagesqx/compare/v1.0.3...v1.0.4) (2026-04-23)


### Bug Fixes

* hide gated fluids in jei/emi, align lock overlay on item icon ([647ef44](https://github.com/mindevis/stagesqx/commit/647ef445b64decc2029262faf5fd68ad79f497e0))

## [1.0.3](https://github.com/mindevis/stagesqx/compare/v1.0.2...v1.0.3) (2026-04-23)


### Bug Fixes

* **jei:** hide blocked items after ingredient load and restore on unlock ([54b5351](https://github.com/mindevis/stagesqx/commit/54b5351f4f00ff40f1e22855049b6e89b288069b))

## [1.0.2](https://github.com/mindevis/stagesqx/compare/v1.0.1...v1.0.2) (2026-04-23)


### Bug Fixes

* EMI stack hiding for non-item keys, JEI hide via ingredient list, lock icon above slot ([4ed87a8](https://github.com/mindevis/stagesqx/commit/4ed87a8e41a69444dfc7f9a3c87ea4e94a685da7))

## [1.0.1](https://github.com/mindevis/stagesqx/compare/v1.0.0...v1.0.1) (2026-04-22)


### Bug Fixes

* resolve AnvilMenu mixin player field via ItemCombinerMenu accessor ([b9e8c43](https://github.com/mindevis/stagesqx/commit/b9e8c43bc6ea670ec80b6cebdba3a9530cc2f1cf))

# 1.0.0 (2026-04-22)


### Features

* add StagesQX progressive stages mod (1.21.1 NeoForge) ([bbe9b50](https://github.com/mindevis/stagesqx/commit/bbe9b5049dabd38815507bf5905f9bd0d15a74a5))
