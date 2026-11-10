import shutil

import pngtint

HUE_SHIFT = 80
GLASS_TOP = 37
TINTED = {
    "item/knowledge.core": "item/infusion.core",
    "block/knowledge.inscriber.top": "block/infusion.inscriber.top",
    "block/knowledge.inscriber.side": "block/infusion.inscriber.side",
    "block/knowledge.inscriber.bottom": "block/infusion.inscriber.bottom",
    "gui/knowledge.inscriber": "gui/infusion.inscriber",
    "gui/arcane.assembler": "gui/infusion.assembler",
    "models/arcane.assembler": "models/infusion.assembler",
    "block/arcane.assembler.fallback": "block/infusion.assembler.fallback",
}


def tint_textures(ctx):
    for source, target in TINTED.items():
        destination = ctx.ASSETS / "textures" / f"{target}.png"
        pngtint.rotate_hue(ctx.SOURCE_TEXTURES / f"{source}.png", destination, HUE_SHIFT)
        meta = ctx.SOURCE_TEXTURES / f"{source}.png.mcmeta"
        if meta.exists():
            shutil.copyfile(meta, destination.with_name(destination.name + ".mcmeta"))


def restore_glass(ctx):
    source = ctx.SOURCE_TEXTURES / "models/arcane.assembler.png"
    destination = ctx.ASSETS / "textures" / "models/infusion.assembler.png"
    width, height, original = pngtint.read(source)
    _, _, tinted = pngtint.read(destination)
    pixels = []
    for index, pixel in enumerate(tinted):
        if index // width >= GLASS_TOP:
            pixel = original[index]
        pixels.append(pixel)
    pngtint.write(destination, width, height, pixels)


def generate(ctx):
    tint_textures(ctx)
    restore_glass(ctx)
    name = "infusion_inscriber"
    ctx.cube_bottom_top(name, "infusion.inscriber.top", "infusion.inscriber.bottom", "infusion.inscriber.side")
    ctx.simple_state(name)
    ctx.block_item(name)
    ctx.self_drop(name)
    ctx.mineable("pickaxe", name)
    ctx.generated_item("infusion_core", "infusion.core")
    name = "infusion_assembler"
    ctx.block_model(name, {"textures": {"particle": ctx.ref("infusion.assembler.fallback")}})
    ctx.write(ctx.ASSETS / "models" / "item" / f"{name}.json", {
        "parent": "minecraft:builtin/entity",
        "textures": {"particle": ctx.ref("infusion.assembler.fallback")},
        "display": {
            "gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [0.625, 0.625, 0.625]},
            "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.25, 0.25, 0.25]},
            "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.5, 0.5, 0.5]},
            "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.375, 0.375, 0.375]},
            "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [0.4, 0.4, 0.4]},
            "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 0, 0], "scale": [0.4, 0.4, 0.4]},
        },
    })
    model = ctx.ref(name)
    ctx.blockstate(name, {"variants": {f"facing={side}": {"model": model} for side in ("north", "south", "east", "west")}})
    ctx.self_drop(name)
    ctx.mineable("pickaxe", name)
