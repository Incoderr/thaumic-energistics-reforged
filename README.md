# Thaumic Energistics: Reforged

Unofficial port of **Thaumic Energistics 1.1.3.0** by Nividica from Minecraft 1.7.10 to **Minecraft 1.21.1** on **NeoForge 21.1**. It is an addon to [Thaumcraft 4: Reforged](https://github.com/notlikethiss/thaumcraft-4-reforged) (branch `mc/1.21.1`) and needs it and Applied Energistics 2 to run.

The port targets 1.21.1 instead of 26.3 because there is no stable Applied Energistics 2 release for Minecraft 26.3; 1.21.1 has AE2 `19.2.18`.

The goal is to recreate the original gameplay as closely as possible: essentia storage cells, import, export and storage buses, the essentia terminal, the arcane crafting terminal, the arcane assembler, the vis relay P2P tunnel, infusion provider and the rest of the bridge between Thaumcraft and Applied Energistics 2. Integrations with other mods (except REI and AE2) are not ported. The technical side is rebuilt on modern APIs, the original coremod (ASM transformers) is replaced by events or mixins.

## Ported features

Everything from Thaumic Energistics 1.1.3.0 is ported:

- Essentia as a native AE2 resource: essentia storage cells (1k to 64k and creative), essentia cell housing and the cell workbench
- Essentia terminal, wireless essentia terminal, import, export and storage buses, level emitter, storage and conversion monitors
- Essentia and infusion providers, the essentia vibration chamber (Ignis and Potentia to AE power)
- Arcane crafting terminal with wand and armor slots, REI recipe transfer
- Knowledge core, knowledge inscriber and the arcane assembler as an AE2 crafting provider
- Distillation pattern encoder for essentia autocrafting through ordinary pattern providers
- Vis relay interface, iron and thaumium gear boxes (crank AE2 chargers, the thaumium one by golems)
- Wireless golem backpack: gather, fill, decanting and alchemy golems work with the network through a wireless access point; facades change its look
- AE wrench wand focus, certus and nether quartz duplication, aspects for AE2 items, AE2 facades of Thaumcraft blocks
- Thaumonomicon tab with TE research and pages in English and Russian

## Additions (not in the original)

- Crucible assembler: crucible recipes as AE2 autocrafting. Recipes are written to an alchemy core in the alchemy inscriber; the network supplies the catalyst and the essentia.
- Infusion assembler: infusion recipes as AE2 autocrafting through an infusion core and inscriber; instability is ignored.

Deliberate differences from the original are listed in `CLAUDE.md` (missing AE2 1.21 content such as the grinder, security terminal and pure certus; fixed game-breaking TE bugs).

> **Personal use only.** The original Thaumic Energistics is licensed under the MIT License (Copyright (c) 2014 Chris). This port is not intended for redistribution.

## Requirements to build

- Java 21
- Python 3
- A checkout of Thaumcraft 4: Reforged on branch `mc/1.21.1`, either as a worktree in `../thaumcraft-4-reforged/.worktrees/mc-1.21.1` or as `../thaumcraft-4-reforged-1.21.1` (or pass `-PthaumcraftDir=/path`)
- The AE2 jar `appliedenergistics2-19.2.18.jar` and the GuideME jar `guideme-21.1.22.jar` in `libs/`: `python3 tools/fetch_libs.py` downloads them from Modrinth and checks their SHA-512 (they are git ignored)

Versions match the base mod's `mc/1.21.1` branch:

- Mod `1.0`
- [Thaumcraft 4: Reforged](https://github.com/notlikethiss/thaumcraft-4-reforged) `1.1.3`
- Minecraft `1.21.1`
- NeoForge `21.1.256`
- ModDevGradle `2.0.148`, Parchment `2024.11.17`
- Applied Energistics 2 `19.2.18` (required, from `libs/`)
- GuideME `21.1.22` (required by AE2, from `libs/`)
- TerraBlender `1.21.1-4.1.0.1` and Curios `9.5.1+1.21.1` (required by the base)
- REI `16.0.799` with Architectury and Cloth Config (optional at runtime; included in development runs, `-PwithoutRei` disables it)

## Building and running

The base mod is included as a Gradle composite build: `settings.gradle` picks the first checkout whose `minecraft_version` is `1.21.1`, Gradle builds it automatically and the base, AE2 and the addon are loaded in every run.

```sh
git -C ../thaumcraft-4-reforged worktree add --track -b mc/1.21.1 .worktrees/mc-1.21.1 upstream/mc/1.21.1
```

```sh
./gradlew build               # build the addon jar into build/libs
./gradlew runClient           # start a development client
./gradlew runServer           # start a development server (game folder: run/server)
./gradlew runGameTestServer   # run the addon game tests (game folder: run/gametest)
python3 tools/fetch_te.py     # download the original source into reference/te-src
python3 tools/check_seams.py  # check version-specific API boundaries
```

## Automatic releases

GitHub Actions builds pushes to `main` and `mc/**` and can also be started manually with **Run workflow**. Pushes that only change Markdown files or `tools/**` are skipped.

The workflow checks out Thaumcraft 4: Reforged alongside the addon, always using its `mc/1.21.1` branch. Java, Minecraft and mod versions are read from `gradle.properties`.

After the seam check and Gradle build pass, the workflow replaces the release and tag named after the Minecraft version (currently `1.21.1`), attaches the addon JAR and uses the latest commit as release notes. Releases from `main` are marked as latest. This follows the release process used by Thaumcraft 4: Reforged.

## Project layout

```text
src/main/java/thaumicenergistics/
  ThaumicEnergistics.java  mod entry point
  Config.java              synced config
  Features.java            feature list (load, research, recipes, aspects, tests)
  registry/                content registration
  network/                 payloads
  platform/                version-specific API implementations
  client/                  client entry point, screens, renderers
  test/                    automated game tests
src/main/templates/        mod metadata templates
src/main/resources/        textures, lang files and the game test structure
libs/                      Applied Energistics 2 and GuideME jars
tools/                     Python fetch, conversion, generation and validation scripts
reference/                 original mod source (git ignored)
```

## Tools

| Script                 | Purpose                                                                                 |
| ---------------------- | --------------------------------------------------------------------------------------- |
| `tools/fetch_te.py`    | Downloads the Thaumic Energistics source (branch `AE2-RV3`, MC 1.7.10) into `reference/te-src` |
| `tools/fetch_libs.py`  | Downloads the AE2 and GuideME jars into `libs/` and verifies their SHA-512 |
| `tools/check_seams.py` | Checks version-specific API boundaries                                                  |
| `tools/convert_assets.py` | Copies TE textures and converts TE lang files to JSON using `tools/id_map.json`      |
| `tools/gen_resources.py` | Writes models, blockstates, loot, recipes and tags into `src/generated/resources` from per-feature generators |
| `tools/id_map.json`    | Maps TE registry names and metadata to 1.21.1 ids                                       |
| `tools/features.txt`   | List of per-feature resource generators                                                 |

## Credits

- **Nividica** for the original Thaumic Energistics (MIT License)
- **CyanideX** for the original textures; **Mrkwtkr, alvin137, puyo061, Wuestengecko, TheVizzy, Adaptivity, Joccob and Keridos** for the original translations; **MKoanga** and **M3gaFr3ak**, credited by the original mod
- **incoderr** for the port
- **Azanor** for Thaumcraft
- **AlgorithmX2** and the AppliedEnergistics team for Applied Energistics 2
