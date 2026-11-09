from gen_buses import convert, textured, sides

ALL_FACES = ("north", "south", "east", "west", "up", "down")
ITEM_SHIFT = 6

FRONT = (6, 6, 15, 10, 10, 16)
MID = (4, 4, 14, 12, 12, 15)
LIGHTS = (5, 5, 13, 11, 11, 14)


def box(bounds, texture, shift=0):
    return textured(bounds, {face: texture for face in ALL_FACES}, shift)


def lights(data, shift=0):
    x1, y1, z1, x2, y2, z2 = LIGHTS
    start, end = convert((x1 - 0.01, y1 - 0.01, z1 - 0.01, x2 + 0.01, y2 + 0.01, z2 + 0.01))
    start[2] += shift
    end[2] += shift
    return {"from": start, "to": end, "faces": {face: dict(data) for face in ALL_FACES}}


def glow(light):
    return {"neoforge_data": {"block_light": light, "sky_light": light}}


def generate(ctx):
    parts = ctx.ASSETS / "models" / "part"
    textures = {
        "side": ctx.ref("parts/vis.interface.side"),
        "border": ctx.ref("parts/bus.color.border"),
        "lightside": ctx.ref("parts/bus.color.side"),
        "light": ctx.ref("parts/bus.color.light"),
        "particle": ctx.ref("parts/vis.interface.side"),
    }
    geometry = lambda shift=0: [
        box(FRONT, "#side", shift),
        box(MID, "#side", shift),
        textured(LIGHTS, sides("#border", north="#lightside", south="#lightside"), shift),
    ]
    ctx.write(parts / "vis_relay_interface_base.json", {"textures": textures, "elements": geometry()})
    ctx.write(parts / "vis_relay_interface_off.json", {
        "textures": textures,
        "elements": [lights({"texture": "#light", "neoforge_data": {"color": "FF000000"}})],
    })
    ctx.write(parts / "vis_relay_interface_on.json", {
        "textures": textures,
        "elements": [lights({"texture": "#light", "tintindex": 1, **glow(13)})],
    })
    ctx.item_model("vis_relay_interface", {
        "parent": "ae2:item/part_base",
        "textures": textures,
        "elements": geometry(ITEM_SHIFT) + [lights({"texture": "#light", "tintindex": 1, **glow(13)}, ITEM_SHIFT)],
    })
    ctx.item_definition("vis_relay_interface", ctx.ref("vis_relay_interface", "item"))
