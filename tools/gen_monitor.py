from gen_buses import convert, textured, sides

ALL_FACES = ("north", "south", "east", "west", "up", "down")
ITEM_SHIFT = 6
FRONT = (2, 2, 14, 14, 14, 16)
LIGHTS = (5, 5, 13, 11, 11, 14)
PANELS = ["ae2:monitor", "ae2:semi_dark_monitor", "ae2:dark_monitor"]


def front(texture, tint, layer, light=None, shift=0):
    x1, y1, z1, x2, y2, z2 = FRONT
    start, end = convert((x1, y1, z1, x2, y2, z2 + 0.01 * layer))
    start[2] += shift
    end[2] += shift
    face = {"texture": texture, "tintindex": tint}
    if light is not None:
        face["neoforge_data"] = {"block_light": light, "sky_light": light}
    return {"from": start, "to": end, "faces": {"north": face}}


def lights(data, shift=0):
    x1, y1, z1, x2, y2, z2 = LIGHTS
    start, end = convert((x1 - 0.01, y1 - 0.01, z1 - 0.01, x2 + 0.01, y2 + 0.01, z2 + 0.01))
    start[2] += shift
    end[2] += shift
    return {"from": start, "to": end, "faces": {face: dict(data) for face in ALL_FACES}}


def lit_light():
    return {"texture": "#light", "tintindex": 1, "neoforge_data": {"block_light": 13, "sky_light": 13}}


def dark_light():
    return {"texture": "#light", "neoforge_data": {"color": "FF000000"}}


def geometry(shift=0):
    return [
        textured(FRONT, sides("#side"), shift),
        textured(LIGHTS, sides("#border", north="#lightside", south="#lightside"), shift),
    ]


def generate(ctx):
    parts = ctx.ASSETS / "models" / "part"
    textures = {
        "side": ctx.ref("parts/arcane.crafting.side"),
        "border": ctx.ref("parts/bus.color.border"),
        "lightside": ctx.ref("parts/bus.color.side"),
        "light": ctx.ref("parts/bus.color.light"),
        "medium": "ae2:part/conversion_monitor_medium",
        "bright": "ae2:part/conversion_monitor_bright",
        "empty": ctx.ref("parts/essentia.terminal.overlay.dark"),
        "particle": ctx.ref("parts/arcane.crafting.side"),
    }
    ctx.write(parts / "essentia_monitor_base.json", {"textures": textures, "elements": geometry()})
    ctx.write(parts / "essentia_monitor_empty_off.json", {"textures": textures, "elements": [front("#empty", 2, 3)]})
    ctx.write(parts / "essentia_monitor_empty_on.json", {"textures": textures, "elements": [front("#empty", 2, 3, 13)]})
    for name, medium_tint in (("storage", 2), ("conversion", 1)):
        ctx.write(parts / f"essentia_{name}_monitor_off.json", {
            "textures": textures,
            "elements": [front("#medium", medium_tint, 1), front("#bright", 3, 2), lights(dark_light())],
        })
        ctx.write(parts / f"essentia_{name}_monitor_on.json", {
            "textures": textures,
            "elements": [front("#medium", medium_tint, 1, 13), front("#bright", 3, 2, 13), lights(lit_light())],
        })
        ctx.item_model(f"essentia_{name}_monitor", {
            "parent": "ae2:item/part_base",
            "textures": textures,
            "elements": geometry(ITEM_SHIFT) + [
                front("#medium", medium_tint, 1, 15, ITEM_SHIFT),
                front("#bright", 3, 2, 15, ITEM_SHIFT),
                front("#empty", 2, 3, 15, ITEM_SHIFT),
                lights(lit_light(), ITEM_SHIFT),
            ],
        })
        ctx.item_definition(f"essentia_{name}_monitor", ctx.ref(f"essentia_{name}_monitor", "item"))
    level_emitter = "thaumicenergistics:essentia_level_emitter"
    storage_monitor = "thaumicenergistics:essentia_storage_monitor"
    ctx.recipe("essentia_storage_monitor", {
        "type": "minecraft:crafting_shapeless",
        "category": "misc",
        "ingredients": [{"item": level_emitter}, [{"item": panel} for panel in PANELS]],
        "result": {"id": storage_monitor},
    }, [storage_monitor, level_emitter])
    ctx.shapeless("essentia_conversion_monitor", "thaumicenergistics:essentia_conversion_monitor", 1, [
        "thaumicenergistics:coalescence_core", storage_monitor, "thaumicenergistics:diffusion_core",
    ])
