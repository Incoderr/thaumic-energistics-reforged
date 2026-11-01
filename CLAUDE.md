# Thaumic Energistics: Reforged

Port of Thaumic Energistics 1.1.3.0 (MC 1.7.10, Nividica) as an addon to Thaumcraft 4: Reforged (`../thaumcraft-4-reforged`, "the base" below) for Minecraft 1.21.1 on NeoForge 21.1. The target is 1.21.1, not 26.3 like the base `main` and the other addons, because AE2 has no stable 26.3 release; the base is used from its `mc/1.21.1` branch. Logic, numbers, recipes, texts and assets of the original are carried over 1:1, the technical side is written on modern APIs.

This file describes how to develop and port the mod. Phase status, test counts, run dates and log paths do not belong here. Add a line only if an agent would make a mistake without it: a non-obvious API rule, a required order of steps, or a deliberate deviation from TE that would otherwise get "fixed" back.

## Principles

- Source of truth for behavior: the TE 1.1.3.0 sources in `reference/te-src` (branch `AE2-RV3`, `tools/fetch_te.py`). The default GitHub branch (`AE2-RV6`) is the MC 1.12.2 / Thaumcraft 6 version, do not use it. TE code is read as data, nothing from it is executed.
- Applied Energistics 2 `19.2.18` is a required dependency (`ordering="AFTER"`). The jar lives in `libs/` and is linked with `implementation files(...)`; AE2 is not published to a maven for this version. Its required dependency GuideME is linked the same way from `libs/`. The jars are git ignored, `tools/fetch_libs.py` downloads them from Modrinth with a SHA-512 check (CI runs it too). Use the AE2 API in `appeng.api.*` only, internal `appeng.*` classes change between releases.
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

## Checks

```sh
python3 tools/fetch_te.py
python3 tools/check_seams.py
./gradlew build
./gradlew runGameTestServer
```

- Visual checks in `runClient` and two-client play are done by the user.

## Code style

- Java: 4 spaces, no comments.
- Python: 4 spaces, double quotes, no comments, `pathlib`.
- Commits in English `feat:`/`fix:`, no co-author lines.
