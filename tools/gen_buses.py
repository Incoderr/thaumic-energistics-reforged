FACES = {"north": "south", "south": "north", "east": "west", "west": "east", "up": "up", "down": "down"}
ITEM_SHIFT = 6
OFFSET = 0.01


def convert(box):
    x1, y1, z1, x2, y2, z2 = box
    return [16 - x2, y1, 16 - z2], [16 - x1, y2, 16 - z1]


def element(box, faces, shift=0):
    start, end = convert(box)
    start[2] += shift
    end[2] += shift
    return {"from": start, "to": end, "faces": {FACES[face]: data for face, data in faces.items()}}


def textured(box, textures, shift=0):
    return element(box, {face: {"texture": texture} for face, texture in textures.items()}, shift)


def sides(side, **overrides):
    textures = {face: side for face in ("north", "south", "east", "west", "up", "down")}
    textures.update(overrides)
    return textures


def grow(box, faces):
    x1, y1, z1, x2, y2, z2 = box
    return (
        x1 - (OFFSET if "west" in faces else 0),
        y1 - (OFFSET if "down" in faces else 0),
        z1 - (OFFSET if "north" in faces else 0),
        x2 + (OFFSET if "east" in faces else 0),
        y2 + (OFFSET if "up" in faces else 0),
        z2 + (OFFSET if "south" in faces else 0),
    )


def overlay(box, faces, data, shift=0):
    return element(grow(box, faces), {face: dict(data) for face in faces}, shift)


def lit(texture, light):
    return {"texture": texture, "tintindex": 1, "neoforge_data": {"block_light": light, "sky_light": light}}


def tinted(texture):
    return {"texture": texture, "tintindex": 1}


def black(texture):
    return {"texture": texture, "neoforge_data": {"color": "FF000000"}}


ALL_FACES = ("north", "south", "east", "west", "up", "down")


def bus(ctx, name, body, overlay_box, overlay_faces, item_overlay_box, lights_box):
    parts = ctx.ASSETS / "models" / "part"
    textures = {
        "side": ctx.ref(f"parts/essentia.{name}.bus.side"),
        "face": ctx.ref(f"parts/essentia.{name}.bus.face"),
        "overlay": ctx.ref(f"parts/essentia.{name}.bus.overlay"),
        "border": ctx.ref("parts/bus.color.border"),
        "lightside": ctx.ref("parts/bus.color.side"),
        "light": ctx.ref("parts/bus.color.light"),
        "particle": ctx.ref(f"parts/essentia.{name}.bus.side"),
    }
    if name != "storage":
        textures["chamber"] = ctx.ref(f"parts/essentia.{name}.bus.chamber")
    light_textures = sides("#border", north="#lightside", south="#lightside")

    def geometry(shift=0):
        return [textured(box, faces, shift) for box, faces in body] + [textured(lights_box, light_textures, shift)]

    ctx.write(parts / f"essentia_{name}_bus_base.json", {"textures": textures, "elements": geometry()})
    ctx.write(parts / f"essentia_{name}_bus_off.json", {
        "textures": textures,
        "elements": [
            overlay(overlay_box, overlay_faces, tinted("#overlay")),
            overlay(lights_box, ALL_FACES, black("#light")),
        ],
    })
    ctx.write(parts / f"essentia_{name}_bus_on.json", {
        "textures": textures,
        "elements": [
            overlay(overlay_box, overlay_faces, lit("#overlay", 13)),
            overlay(lights_box, ALL_FACES, lit("#light", 13)),
        ],
    })
    ctx.item_model(f"essentia_{name}_bus", {
        "parent": "ae2:item/part_base",
        "textures": textures,
        "elements": geometry(ITEM_SHIFT) + [
            overlay(item_overlay_box, overlay_faces, lit("#overlay", 15), ITEM_SHIFT),
            overlay(lights_box, ALL_FACES, lit("#light", 13), ITEM_SHIFT),
        ],
    })
    ctx.item_definition(f"essentia_{name}_bus", ctx.ref(f"essentia_{name}_bus", "item"))


def generate(ctx):
    side = "#side"
    chamber = "#chamber"
    lights = (6, 6, 11, 10, 10, 12)
    bus(ctx, "import", [
        ((4, 4, 15, 12, 12, 16), sides(side, south="#face")),
        ((4, 4, 14, 12, 12, 15), sides(chamber)),
        ((5, 5, 13, 11, 11, 14), sides(chamber)),
        ((5, 5, 12, 11, 11, 13), sides(side)),
    ], (4, 4, 15, 12, 12, 16), ("south",), (4, 4, 15, 12, 12, 16), lights)
    bus(ctx, "export", [
        ((4, 4, 12, 12, 12, 12.5), sides(side)),
        ((4, 4, 12.5, 12, 12, 13.5), sides(chamber)),
        ((5, 5, 13.5, 11, 11, 14.5), sides(chamber)),
        ((5, 5, 14.5, 11, 11, 15), sides(side)),
        ((6, 6, 15, 10, 10, 16), sides(side, south="#face")),
    ], (6, 6, 15, 10, 10, 16), ("up", "down", "east", "west"), (6, 6, 15, 10, 10, 16), lights)
    bus(ctx, "storage", [
        ((1, 1, 15, 15, 15, 16), sides(side, south="#face")),
        ((4, 4, 14, 12, 12, 15), sides(side, south="#face")),
    ], (1, 1, 15, 15, 15, 16), ("south",), (2, 2, 15, 14, 14, 16), (5, 5, 13, 11, 11, 14))
