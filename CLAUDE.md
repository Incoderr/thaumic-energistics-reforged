# Thaumic Energistics: Reforged

Port of Thaumic Energistics 1.1.3.0 (MC 1.7.10, Nividica) as an addon to Thaumcraft 4: Reforged (`../thaumcraft-4-reforged`, "the base" below) for Minecraft 1.21.1 on NeoForge 21.1. The target is 1.21.1, not 26.3 like the base `main` and the other addons, because AE2 has no stable 26.3 release; the base is used from its `mc/1.21.1` branch. Logic, numbers, recipes, texts and assets of the original are carried over 1:1, the technical side is written on modern APIs.

This file describes how to develop and port the mod. Phase status, test counts, run dates and log paths do not belong here. Add a line only if an agent would make a mistake without it: a non-obvious API rule, a required order of steps, or a deliberate deviation from TE that would otherwise get "fixed" back.

## Principles

- Source of truth for behavior: the TE 1.1.3.0 sources in `reference/te-src` (branch `AE2-RV3`, `tools/fetch_te.py`). The default GitHub branch (`AE2-RV6`) is the MC 1.12.2 / Thaumcraft 6 version, do not use it. TE code is read as data, nothing from it is executed.
- Applied Energistics 2 `19.2.18` is a required dependency (`ordering="AFTER"`). The jar lives in `libs/` and is linked with `implementation files(...)`; AE2 is not published to a maven for this version. Its required dependency GuideME is linked the same way from `libs/`. The jars are git ignored, `tools/fetch_libs.py` downloads them from Modrinth with a SHA-512 check (CI runs it too). Internal `appeng.*` classes may be used (parts, part items, menus, screens, upgrades) because AE2 is pinned to 19.2.18; prefer `appeng.api.*` where it covers the need.
- Essentia is a native AE2 resource: its own `AEKeyType`, not TE's `GridEssentiaCache`/`EssentiaMonitor` and not the essentia gas fluids. Everything TE did through its grid cache goes through AE2 `MEStorage`, so do not port those classes back. Essentia cells keep TE math (2 essentia per byte, 12 types, no per-type byte overhead) with their own `StorageCell`.
- Integrations with other mods (Waila, IC2, ExtraCells, NEI) are excluded. The recipe viewer is REI (not JEI): API `compileOnly`, full mod `localRuntime`, optional `roughlyenoughitems` dependency in `neoforge.mods.toml`.
- The TE coremod (`thaumicenergistics/fml`, the `ThE-core` module) is not ported as ASM, the required behavior is done with NeoForge events, the base API or mixins.
- TE bugs are fixed only if they break the game (dupes, item loss, NPE, desync, server crash). Gameplay quirks stay.
- The server validates what TE trusted from the client. Client classes never leak to the server, everything must work on a dedicated server.
- Resources are made by Python generators in `tools/`, datagen is not used.
- The original is MIT licensed (Copyright (c) 2014 Chris), the port is `All Rights Reserved` for personal use.

## Environment

- Mod `thaumicenergistics`, package `thaumicenergistics`, asset namespace `thaumicenergistics`, archive `thaumic-energistics-reforged`, version 1.0.
- MC 1.21.1, NeoForge 21.1.256, ModDevGradle 2.0.148 with Parchment 2024.11.17, AE2 19.2.18, GuideME 21.1.22, REI 16.0.799, TerraBlender 1.21.1-4.1.0.1, Curios 9.5.1+1.21.1, Java 21. Versions in `gradle.properties` match the base `mc/1.21.1` branch.
- 1.21.1 API differs from the 26.3 code in the base `main` and the other addons: `ResourceLocation` instead of `Identifier`, `ModConfig.Type.SERVER` instead of `SYNCED`, `Item.Properties` passed directly to `registerSimpleBlockItem`, no `DeferredRegister.createEntities`. Port 26.3 snippets from the other addons with the `mc/1.21.1` branch of `../thaumic-tinkerer-reforged` as the reference.
- Python 3, `tools/` scripts have no external dependencies.
- The base is included as a composite build, dependency `implementation "thaumcraft:thaumcraft-4-reforged:1.0"`. `settings.gradle` takes the first of `../thaumcraft-4-reforged`, `../thaumcraft-4-reforged/.worktrees/mc-1.21.1`, `../thaumcraft-4-reforged-1.21.1` whose `minecraft_version` is `1.21.1`, or `-PthaumcraftDir`. Locally the base is a worktree of branch `mc/1.21.1` (tracks `upstream/mc/1.21.1`) in `.worktrees/mc-1.21.1`. The base is not edited from the addon.
- `neoforge.mods.toml`: the `thaumcraft` dependency is required with `ordering="AFTER"`, so the addon constructor runs after the base constructor. Research and recipes are registered from the addon constructor.
- GameTests use the 1.21.1 framework: `TeGameTestSuite` is a `@GameTestHolder` with a `@GameTestGenerator`, tests are filtered by `neoforge.enabledGameTestNamespaces`, base tests are not run. The structure `thaumicenergistics:test/platform` lives in `src/main/resources/data/thaumicenergistics/structure/test/platform.nbt`, `TeGameTestHelper` shifts positions one block up onto it. `runGameTestServer` fails unless the log reports a successful run.
- Run folders are `run/client`, `run/server`, `run/gametest`; the server ones need `eula.txt` with `eula=true` (gitignored).
- There is no root `icon.png` or `poster.png` yet, `logoFile`/`iconFile`/`bannerFile` in `neoforge.mods.toml` are added together with them.

## References (gitignored)

- `reference/te-src/` TE sources, `tools/fetch_te.py` downloads the archive into `tools/cache/` and extracts it without the top-level folder.
- MC, NeoForge sources and the decompiled TC4 live in `../thaumcraft-4-reforged/reference`.
- For the base addon API and layout examples see `../thaumic-tinkerer-reforged/CLAUDE.md`.

## Architecture

- `platform/` and `client/platform/` hold everything where version APIs diverge. The addon has its own seams in the `thaumicenergistics` namespace: `TeNetwork` (its own `RegisterPayloadHandlersEvent`, client handlers are dispatched through `setClientHandler` because 1.21.1 has no client payload registration event), `client/platform/TeClientNetwork`, `TeRegistration` (`Items`/`Blocks` with the 26.3-style `registerItem`/`registerBlock(name, factory, properties)`). Gameplay code goes only through them and through the base seams. `tools/check_seams.py` fails on forbidden imports outside the seams and on client class imports outside `client/`.
- Registration: `registry/` (`ModItems`, `ModBlocks` with `withItem`, `ModBlockEntities`, `ModMenus`, `ModEntities`, `ModEffects`, `ModSounds`, `ModCreativeTabs`), payloads in `network/ModPayloads`, client handlers in `client/ClientPayloadHandlers`, client entry point `client/ThaumicEnergisticsClient`.
- `Config` is SERVER only (synced to clients), keys and defaults are carried over from the TE config.
- The creative tab is the addon's own, its icon is the base's thaumium ingot for now.

## Feature layout

- A feature lives in `thaumicenergistics/<feature>/` and exposes one `<Feature>Feature` class: static `DeferredHolder`s in the shared registries, methods `load()` (empty, needed to load the class), `research()`, `recipes()`, and `aspects(AspectRegistry)` when needed.
- A feature adds one line each to the shared files: `Features.java`, `client/ClientFeatures.java`, `tools/features.txt`, `network/ModPayloads.java`, `client/ClientPayloadHandlers.java`. These files use `merge=union` in `.gitattributes`, write lines as separate statements.
- A feature's GameTests live in `test/<Feature>GameTests.java` on `TeGameTestSuite` (`new TeGameTestSuite("name").add("test", body)`, registered with one line in `Features.tests`).

## Research and recipes

- `research/TeResearch`: category `thaumicenergistics` (registered in `TeResearch.init()` from `CoreFeature.research()`, which must stay first in `Features.research`), keys `TeResearch.key("CORES")` = `thaumicenergistics.TECORES`, pages `TeResearch.text(key, n)`.
- The base scrolls the tab background in proportion to the category bounds, so `TeResearch.init()` registers two virtual research items at the corners of the full TE tree (-6,-7 and 6,2). Without them a partly ported tab scrolls its background far too fast.
- TE pseudo research: `TeResearch.pseudo("DISTILESSENTIA")` registers once a hidden stub `thaumicenergistics.Pseudo.<KEY>` in the TE tab at the TE position, mirrors the real research name and pages, and adds itself to the real research siblings, so it unlocks with it. Use it wherever TE used `PseudoResearchTypes`.
- `crafting/TeRecipes` over `TcRecipes.register`; recipe keys are TE `RecipeRegistry` field names with the mod prefix (`thaumicenergistics.ITEM_STORAGE_COMPONENT_1K`). Workbench recipes are JSON from the feature generator plus `TeRecipes.vanilla` refs for research pages.
- TE certus quartz groups (certus, charged, pure) become the `#ae2:all_certus_quartz` tag; AE2 1.21 has no pure certus.

## AE2 parts and menus

- TE parts are AE2 parts on AE2 base classes (`AbstractTerminalPart` and friends), items are `appeng.items.parts.PartItem`. Part models are JSON in `models/part/` written by the feature generator and must be listed in `PartModels.registerModels` from the feature `load()`. Part items need an `ItemColor` (`StaticItemColor(AEColor.TRANSPARENT)`) for the tinted cable-color layers.
- Menus opened from parts use `MenuTypeBuilder.create(Menu::new, HostInterface.class).buildUnregistered(id)` inside the addon `MENUS` register, and extend `AEBaseMenu` for `@GuiSync` fields and `registerClientAction`. Screens are plain `AbstractContainerScreen`, so slots are vanilla `Slot`s with TE coordinates: AE2 slots get positions only from AE2 screen style JSON.
- A part with dynamic rendering overrides `IPart.renderDynamic` and delegates to a renderer interface that `ClientFeatures.setup` assigns from `client/`. Only `PoseStack` and `MultiBufferSource` may be imported outside `client/` for that signature (`check_seams.py` allows exactly these two). The renderer field starts as `null`; a default lambda there loads `PoseStack` on a dedicated server and crashes it.
- Server work that TE did in `detectAndSendChanges` runs in `broadcastChanges`. Essentia lists go to the client as the addon's own payload with the menu `containerId`.
- `TeResearch.existing(pages...)` drops recipe pages whose recipes are not registered yet, so a research shared by several features (TEESSTERM) gets its pages as the features land; keep the full TE page order in the call.

## Arcane crafting terminal

- The terminal is an exception to the plain-screen rule: the menu extends AE2 `MEStorageMenu` and the screen `MEStorageScreen`, so the ME item list, search, sorting, view cells, terminal sizes and autocrafting come from AE2. Slot positions come from an AE2 screen style; AE2 loads styles and their textures only from the `ae2` namespace, so `gen_act.py` writes `assets/ae2/screens/thaumicenergistics/arcane_crafting_terminal.json` and copies the GUI texture to `assets/ae2/textures/guis/thaumicenergistics/`. Positions are TE coordinates turned into `bottom` anchors against the 243 px default height.
- The wand and armor slots use the addon's slot semantics `THAUMICENERGISTICS_WAND` and `THAUMICENERGISTICS_ARMOR`, registered in `ArcaneCraftingTermMenu`.
- The result follows TE: a vanilla recipe wins, otherwise the arcane recipe needs the full vis cost in the wand. A consumed grid item is refilled first from the player inventory, then from the network (exact item, then any item of the same type).
- TE refilled the wand from a linked vis relay interface; the link is a TE memory card carrying the `vis_source_link` component, loaded in `ArcaneCraftingTerminalPart.onUseItemOn`, and the wand is charged by the terminal's `IGridTickable` service.
- Menu-level tests must call `broadcastChanges()` once: AE2 updates the menu's grid link there, and without it every network action is refused.

## Vis relay interface

- `visrelay/` ports `PartVisInterface`: the part is a `DigiVisSource` (TE `IDigiVisSource`) and an `IGridTickable`; non-provider ends drain their facing vis relay for 4 AE per drain, provider ends pull vis through a `VisSourceLink` (TE `DigiVisSourceData`) from the linked source. Links live on memory cards as the `vis_source_link` data component (`VisRelayFeature.LINK`); export goes through `exportSettings(MEMORY_CARD)`, which also turns the exporting interface back into a plain end; a blank card on a provider clears the link, TE 1:1.
- `VisProviderNode extends VisNode` (TE `VisProviderProxy`) is the fake vis net source of a provider: never registered, never ticked by the level, and driven only from the part tick. Because 1.21 validates the block state in the `BlockEntity` constructor and forbids creating `BlockEntityType`s after registries freeze, it reuses AE2's `AEBlockEntities.CABLE_BUS` type; nothing ever saves or reads it.
- Port deviation: the proxy node sits at the cable bus block, one block behind TE's position (the block in front of the interface). The 1.21 vis net skips nodes at zero distance, so a proxy placed on the relay block could never be picked as a parent. When two interfaces on one bus block face different relays, `VisNet.addSource` keys by position and the later registration wins; stale parent chains keep their own provider, so both relays still drain.
- The arcane assembler pulls vis through the same `VisSourceLink` loaded from a memory card.

## Knowledge core and inscriber

- A knowledge core keeps up to 21 `ArcanePattern`s (grid as placed, result, base aspect cost) in the `knowledge_core_patterns` data component; patterns are unique by result stack, as in TE. TE stored per-slot recipe ingredient lists; here the pattern keeps the placed items, and the arcane assembler accepts only those items (TE `canSubItem` rules), not other tag members.
- The inscriber menu is a vanilla menu with fake grid, pattern and result slots handled in `clicked`; save/delete and clear are `clickMenuButton` ids, the save state and a save counter are `DataSlot`s.
- The arcane assembler research (`ARCANEASSEMBLER`) is registered with the knowledge feature because the inscriber research hangs off it; its recipe page comes from `AssemblerFeature.recipes()` through `TeResearch.existing`.

## Arcane assembler

- The assembler is an AE2 `ICraftingProvider` service on its grid node. Each knowledge core pattern becomes an `AssemblerPattern` whose definition is a knowledge core item carrying the single pattern in `arcane_assembler_pattern`; the decoder registered in `setup()` turns it back, so saved crafting jobs survive a restart.
- Inputs are the placed grid items; a slot accepts the same item with equal components, damageable items with any damage (TE `canSubItem`). On completion the result and the ingredients' crafting remainders (and a primordial pearl, TE's eldritch object 3) go into the network.
- TE quirk kept: crafting progresses even when the network gives less power than asked (`extracted - required <= 0`).
- The TE sceptre infusion component is `thaumcraft:wand` with data `cap`/`rod`/`sceptre`; the addon registers the `sceptre` data handler and matcher on `TcStack`. A crafted assembler starts with full vis (`arcane_assembler_vis` component), dismantling with a wrench keeps the stored vis.
- The assembler is drawn like TE: a block entity renderer (and a `BlockEntityWithoutLevelRenderer` for the item) builds TE's Techne cubes as vanilla `ModelPart`s with the same rotations; a JSON conversion of those rotated boxes mapped faces wrongly and z-fought.
- Not ported: the network tool panel of the GUI, the AE2 assembler animation and rune particles while crafting, the AE2 security check.

## Essentia autocrafting

- TE crafted essentia through a placeholder `crafting.aspect` item watched by its grid cache; here essentia is an AE2 key, so the distillation pattern encoder writes a plain AE2 processing pattern (one source item in, `EssentiaKey` x aspect amount out). Patterns go into ordinary pattern providers, and the essentia terminal's craft requests use AE2's own crafting.
- The encoder shows up to 6 aspects of the source item sorted by amount (`AspectHelper.getBonusTags`) only when the player has scanned the item; otherwise 6 unknown slots, TE 1:1. Aspects are recomputed on both sides from the synced source slot; only the selected index is a `DataSlot`.
- The encoder block always faces up (TE default meta); TE's wrench rotation and the GUI orb particles are not ported.

## Essentia vibration chamber

- The chamber is an `AENetworkedBlockEntity` (no channel) with an `IGridTickable` service, TE timing 10..40 ticks; it pulls Ignis/Potentia from neighbouring tubes like TE's `takeEssentiaFromTransportNeighbors` and injects power through the grid energy service.
- The facing and the processed aspect are block state properties (`facing`, `processing`), which pick the front texture and the light level 12; TE's wrench rotation is not ported.
- The chamber item is an essentia container for the terminal: `EssentiaContainers.registerRestricted` gives it 64 capacity and the Ignis/Potentia filter, contents live in the base `AspectKeys.ASPECTS` component and survive a wrench dismantle.
- `IEnergyService.getStoredPower()` is cached; tests read the energy cell's `getAECurrentPower()` instead.

## Gear boxes

- Gear boxes crank AE2 `ICrankable`s (`ICrankable.get`, the charger in AE2 1.21; there is no grindstone any more) with TE's power split: 6 per crank divided between turnable sides, a turn every 18.
- Iron and thaumium boxes share one block entity type; only the thaumium box takes clicks from the base golem fake player (`FakeThaumcraftGolem`). Nothing is saved, the crankable sides and the shaft spin go to clients in the update tag.
- Rendering is a block entity renderer plus item renderer over TE's Techne cubes as `ModelPart`s; ModelRenderer offsets are applied before the 0.625 cube scale, as in 1.7. TE's `disableGearboxModel` config is not ported.

## Small features

- Quartz dupe: crucible recipes on `#c:gems/certus_quartz` and `#c:gems/quartz`; AE2 1.21 has no pure certus, so only the plain crystal is duplicated.
- AE item aspects: `AeAspectsFeature` runs from the base `AspectSync` registrar with TE's `AEAspectRegister` rules: fixed tags for the TE base list, then every `ae2` item from its first inscriber or crafting recipe with the per-aspect maximum of its ingredients (not the base's 0.75 sum), TE bonuses and removals, at most 6 aspects. Items AE2 1.21 removed (pure crystals, seeds, grinder dusts) are skipped.
- AE wrench focus: holds a certus quartz wrench in the main hand for one `useItemOn` (sneak + right click dismantles; sneak + left click rotates parts with sneak released, other blocks through `BlockState.rotate`). Costs 10 Ignis + 10 Aer only when something happened; TE's wrench beam particles are not ported.
- Thaumcraft facades: TE forced AE facade config entries; here `gen_facades.py` adds the same blocks to `ae2:whitelisted/facades`.
- REI transfer: `client/rei` registers a transfer handler for the arcane terminal (vanilla crafting and arcane displays). The client sends every option of each slot in `ActFillGridPayload`; the server clears the grid into the network and fills each slot from the first option it finds (player inventory, then network), TE's NEI overlay rules.
- Inscriber save particles are drawn by the screen when the synced save counter changes, TE's `GuiParticleAnimator` timing (0.4 s, 50 ms stagger, additive blend, thaumcraft particle sheet row 64).

## Golem wireless backpack

- TE hooked `EntityGolemBase` through its coremod; here everything is NeoForge events on the base `GolemBase`: `EntityInteract` puts on the backpack (fill, gather, liquid, essentia cores only) or a facade, `AttackEntityEvent` with the golem bell drops the linked backpack and the facade before the bell removes the golem, and `EntityTickEvent` re-adds the network goal whenever `rebuildGoals` dropped it.
- The backpack keeps the AE2 link (`WIRELESS_LINK_TARGET`, linked in the wireless access point slot through `GridLinkables`; AE2 1.21 has no security terminal) and the facade in a persistent entity attachment (`platform/TeEntityKey`); in-range state and skin go to clients through a synced attachment, TE's 18 tick refresh.
- Deliberate fix of a TE bug: TE's `WirelessAELink` searched access points only after it already had one, so a fresh backpack never connected. The link now starts from the access point it was linked to and then follows the closest active one of that grid in range.
- Network goals follow TE `AIAENetworkGolem`: 20 tick cooldown, rates by order upgrades (items 8/24/32, fluids 100/250/500 mB, essentia 4/12/16, doubled for advanced golems), fluids cost mB/100 AE, essentia goes through `EssentiaNetwork`. The fill goal asks the network for what the home container misses (TC4's `itemWatched` has no 1.21 counterpart).
- The backpack is drawn in `RenderLivingEvent.Post` with TE's model, pearl quads and transforms.

## Essentia buses
- `essentia/EssentiaTileContainers` is TE's tile whitelist by exact block entity class with TE capacities (jar and void jar 64, reservoir 256, buffer 8, alembic 32). Later features add themselves with `extractOnly`/`injectOnly`/`both` (the vibration chamber is inject only).
- Network transfers of buses go through `essentia/EssentiaNetwork`, which charges TE's 0.3 AE per essentia on top of AE2 storage; plain `MEStorage` calls skip that cost.
- The storage bus mounts its own `MEStorage` with `IStorageProvider`; AE2 polls mounted storage every tick, so TE's cached diff and `postAlterationToHostGrid` are not ported. A neighbour exposing `AECapabilities.ME_STORAGE` (interface, condenser, ME chest) is used as is, filtered to essentia.
- Deliberate fixes of TE bugs: the import bus drains only what the network accepted (TE drained the full transfer amount and lost the rest), bus filters and the inverter apply to every storage bus target and to the listed contents, and hidden IO filter slots cannot be set from the client.
- Parts render in AE2's cutout layer only, so the half-transparent chamber glass of the IO buses renders opaque.

## Essentia monitors

- The storage and conversion monitors use AE2 1.21's `conversion_monitor_medium`/`_bright` textures for the front. TE used AE2 rv3's conversion-monitor textures, which no longer exist; the storage monitor tints the medium layer with the medium cable colour and the conversion monitor with the dark one, as TE swapped them.
- Shift + right-click with an essentia container on the conversion monitor needs the block to receive the click while sneaking: `MonitorFeature` sets `setUseBlock(TRUE)` in `RightClickBlock`.
- Empty warded and void jar items are essentia containers (TE `BlockJarItem`); filling them gives the filled jar item.

## Essentia providers

- Provider blocks are `AENetworkedBlockEntity` (channel required) on the addon's `AeWrenchableBlock`. `AEBaseBlockEntity.registerBlockEntityItem` runs in the feature `setup()`, otherwise the grid node shows no item.
- Every addon block entity that hosts an AE2 grid node registers `AECapabilities.IN_WORLD_GRID_NODE_HOST` through `platform/TeCapabilities.gridHost`. AE2 registers it only for its own types; without it cables placed next to the block never connect, while a block placed next to an existing cable still links one way, which hides the bug.
- Grid node callbacks also fire while the chunk unloads. Changing the block state then dirties the unloading chunk, and the server loops forever in `ChunkMap.scheduleUnload` on save; `ProviderBlockEntity` ignores node changes after `onChunkUnloaded`.
- The block has an `active` state property, set from `markForUpdate`, which picks the model whose overlay glows. The block model is a `neoforge:composite` of a solid base and a translucent overlay: the overlay texture is a soft glow with low alpha, and in the cutout layer it turns into a solid black blob. The overlay colour comes from the `RegisterColorHandlersEvent.Block` handler: medium variant of the provider colour when active, black otherwise. The provider colour follows the attached cable as in TE until a colour applicator forces it.
- TE's infusion provider runes were spawned on the server through the proxy; here they go to nearby clients as `ProviderRunesPayload`. Its `addToContainer` returns the full amount instead of TE's 0, so nothing pushed into it is destroyed.

## Essentia cells

- The 16k cell holds 16348 bytes, a TE typo kept on purpose. Bytes count as `ceil(essentia / 2)`.
- Cell contents live in the `thaumicenergistics:essentia_cell` data component (`CellsFeature.CELL_DATA`), written in `persist()` (AE2 calls it after `saveChanges`; without a save provider the inventory persists at once).
- The creative cell accepts nothing and gives an unlimited amount of each partitioned aspect (2,000,000,000 shown), as in TE.

## Generated files (do not edit by hand)

| File | Generator |
|---|---|
| `src/main/resources/assets/thaumicenergistics` (textures, lang) | `tools/convert_assets.py` (+ `tools/assets_extra/`, `tools/lang/`) |
| `src/generated/resources` (rewritten entirely) | `tools/gen_resources.py` (`tools/mcformat.py` with the 1.21.1 profile) |

- After a lang change run `convert_assets.py`, then `gen_resources.py`. Hand-written translations go only to en_us and ru_ru in `tools/lang/<feature>/`; missing ru_ru TE strings are translated by hand there.
- `tools/id_map.json` is edited by hand: `old` (TE registry name), `meta`, `lang` (en_US.lang key or null), `id`, `state` (blocks only), `skip` (reason or null). TE AE parts (`part.base` meta) and meta subitems become separate ids.
- Lang keys: blocks `block.thaumicenergistics.<id>`, items and parts `item.thaumicenergistics.<id>`. All other TE keys (`thaumicenergistics.*`, `tc.research_name/text.*`, `tc.research_category.*`) are kept as is: TE research keys are `thaumicenergistics.<KEY>` and its page texts are raw keys `thaumicenergistics.research_page.<KEY>.<N>` numbered from 1, the base `ResearchPage.Text` takes them directly. `%d` becomes `%s`.
- Textures keep TE names lowercased (`blocks` to `block`, `items` to `item`; `models`, `gui`, `research` keep their folders).

## Checks

```sh
python3 tools/fetch_te.py
python3 tools/convert_assets.py
python3 tools/gen_resources.py
python3 tools/check_seams.py
./gradlew build
./gradlew runGameTestServer
```

- Visual checks in `runClient` and two-client play are done by the user.

## Code style

- Java: 4 spaces, no comments.
- Python: 4 spaces, double quotes, no comments, `pathlib`.
- Commits in English `feat:`/`fix:`, no co-author lines.
