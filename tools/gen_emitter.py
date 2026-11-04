from gen_buses import convert, textured

ALL_FACES = ("north", "south", "east", "west", "up", "down")
FACES = {"north": "south", "south": "north", "east": "west", "west": "east", "up": "up", "down": "down"}
ITEM_SHIFT = 7


def box(bounds, texture, shift=0):
    return textured(bounds, {face: texture for face in ALL_FACES}, shift)


def glow(bounds, faces):
    start, end = convert(bounds)
    light = {"texture": "#active", "neoforge_data": {"block_light": 13, "sky_light": 13}}
    return {"from": start, "to": end, "shade": False, "faces": {FACES[face]: dict(light) for face in faces}}


def generate(ctx):
    parts = ctx.ASSETS / "models" / "part"
    textures = {
        "base": ctx.ref("parts/essentia.level.emitter.base"),
        "active": ctx.ref("parts/essentia.level.emitter.active"),
        "inactive": ctx.ref("parts/essentia.level.emitter.inactive"),
        "particle": ctx.ref("parts/essentia.level.emitter.active"),
    }
    head = (7, 7, 13, 9, 9, 15)
    ctx.write(parts / "essentia_level_emitter_base.json", {"textures": textures, "elements": [box((7, 7, 11, 9, 9, 13), "#base")]})
    ctx.write(parts / "essentia_level_emitter_off.json", {"textures": textures, "elements": [box(head, "#inactive")]})
    ctx.write(parts / "essentia_level_emitter_on.json", {
        "textures": textures,
        "elements": [
            box(head, "#active"),
            glow((6.999, 6, 12, 9.001, 10, 16), ("east", "west")),
            glow((6, 6.999, 12, 10, 9.001, 16), ("up", "down")),
        ],
    })
    ctx.item_model("essentia_level_emitter", {
        "parent": "ae2:item/part_base",
        "textures": textures,
        "elements": [
            box((7, 1, 14, 9, 7, 16), "#base", ITEM_SHIFT),
            box((7, 7, 14, 9, 9, 16), "#active", ITEM_SHIFT),
        ],
    })
    ctx.item_definition("essentia_level_emitter", ctx.ref("essentia_level_emitter", "item"))
