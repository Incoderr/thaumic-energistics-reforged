ROTATIONS = {
    "north": {},
    "east": {"y": 90},
    "south": {"y": 180},
    "west": {"y": 270},
    "up": {"x": 270},
    "down": {"x": 90},
}


def generate(ctx):
    name = "essentia_vibration_chamber"
    for state in ("off", "ignis", "potentia"):
        ctx.block_model(f"{name}_{state}", {
            "parent": "minecraft:block/orientable",
            "textures": {
                "front": ctx.ref(f"e.vibration.face.{state}"),
                "side": ctx.ref("e.vibration.input"),
                "top": ctx.ref("e.vibration.input"),
                "bottom": ctx.ref("e.vibration.input"),
                "particle": ctx.ref("e.vibration.input"),
            },
        })
    variants = {}
    for facing, rotation in ROTATIONS.items():
        for state in ("off", "ignis", "potentia"):
            variants[f"facing={facing},processing={state}"] = {"model": ctx.ref(f"{name}_{state}"), **rotation}
    ctx.blockstate(name, {"variants": variants})
    ctx.block_item(name, model=ctx.ref(f"{name}_off"))
    ctx.self_drop(name)
    ctx.mineable("pickaxe", name)
