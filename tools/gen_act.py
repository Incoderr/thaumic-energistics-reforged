import shutil

from gen_buses import convert, textured, sides

GUI_HEIGHT = 243
TEXTURE = "guis/thaumicenergistics/arcane_crafting.png"
FRONT = (2, 2, 14, 14, 14, 16)
LIGHTS = (5, 5, 13, 11, 11, 14)
ALL_FACES = ("north", "south", "east", "west", "up", "down")
LIME_DARK = "FF4EC04E"


def bottom(y):
    return GUI_HEIGHT - y


def front(texture, layer, extra=None, shift=0):
    x1, y1, z1, x2, y2, z2 = FRONT
    start, end = convert((x1, y1, z1, x2, y2, z2 + 0.01 * layer))
    start[2] += shift
    end[2] += shift
    face = {"texture": texture}
    if extra:
        face.update(extra)
    return {"from": start, "to": end, "faces": {"north": face}}


def lights(data, shift=0):
    x1, y1, z1, x2, y2, z2 = LIGHTS
    start, end = convert((x1 - 0.01, y1 - 0.01, z1 - 0.01, x2 + 0.01, y2 + 0.01, z2 + 0.01))
    start[2] += shift
    end[2] += shift
    return {"from": start, "to": end, "faces": {face: dict(data) for face in ALL_FACES}}


def glow(light):
    return {"neoforge_data": {"block_light": light, "sky_light": light}}


def active_layers(shift=0, light=13):
    return [
        front("#overlay3", 2, {"tintindex": 1, **glow(light)}, shift),
        front("#overlay2", 3, {"tintindex": 2, **glow(light)}, shift),
        front("#overlay4", 4, {"neoforge_data": {"color": LIME_DARK, "block_light": 10, "sky_light": 10}}, shift),
    ]


def style():
    rect = lambda v, h: {"texture": TEXTURE, "srcRect": [0, v, 195, h]}
    return {
        "includes": ["../terminals/terminal.json"],
        "slots": {
            "CRAFTING_GRID": {"left": 44, "bottom": bottom(90), "grid": "BREAK_AFTER_3COLS"},
            "CRAFTING_RESULT": {"left": 116, "bottom": bottom(126)},
            "THAUMICENERGISTICS_WAND": {"left": 116, "bottom": bottom(90)},
            "THAUMICENERGISTICS_ARMOR": {"left": 8, "bottom": bottom(81), "grid": "VERTICAL"},
            "PLAYER_INVENTORY": {"left": 8, "bottom": bottom(162), "grid": "BREAK_AFTER_9COLS"},
            "PLAYER_HOTBAR": {"left": 8, "bottom": bottom(220), "grid": "HORIZONTAL"},
        },
        "text": {
            "dialog_title": {
                "text": {"translate": "thaumicenergistics.gui.arcane.crafting.terminal.title"},
                "position": {"left": 8, "top": 6},
            },
            "player_inventory_title": {"text": "", "position": {"left": 8, "bottom": 95}},
        },
        "terminalStyle": {
            "header": rect(0, 17),
            "firstRow": rect(17, 18),
            "row": rect(35, 18),
            "lastRow": rect(53, 18),
            "bottom": rect(71, GUI_HEIGHT - 71),
        },
        "widgets": {
            "scrollbar": {"left": 175, "top": 18},
            "search": {"left": 98, "top": 6, "width": 65, "height": 10},
            "viewCells": {"left": 205, "top": 2},
            "clearCraftingGrid": {"left": 98, "bottom": bottom(89)},
            "swapArmor": {"left": 26, "bottom": bottom(112)},
        },
    }


def generate(ctx):
    parts = ctx.ASSETS / "models" / "part"
    textures = {
        "side": ctx.ref("parts/arcane.crafting.side"),
        "overlay1": ctx.ref("parts/arcane.crafting.overlay1"),
        "overlay2": ctx.ref("parts/arcane.crafting.overlay2"),
        "overlay3": ctx.ref("parts/arcane.crafting.overlay3"),
        "overlay4": ctx.ref("parts/arcane.crafting.overlay4"),
        "border": ctx.ref("parts/bus.color.border"),
        "lightside": ctx.ref("parts/bus.color.side"),
        "light": ctx.ref("parts/bus.color.light"),
        "particle": ctx.ref("parts/arcane.crafting.side"),
    }
    geometry = lambda shift=0: [
        textured(FRONT, sides("#side"), shift),
        textured(LIGHTS, sides("#border", north="#lightside", south="#lightside"), shift),
        front("#overlay1", 1, None, shift),
    ]
    ctx.write(parts / "arcane_crafting_terminal_base.json", {"textures": textures, "elements": geometry()})
    ctx.write(parts / "arcane_crafting_terminal_off.json", {
        "textures": textures,
        "elements": [lights({"texture": "#light", "neoforge_data": {"color": "FF000000"}})],
    })
    ctx.write(parts / "arcane_crafting_terminal_on.json", {
        "textures": textures,
        "elements": active_layers() + [lights({"texture": "#light", "tintindex": 1, **glow(13)})],
    })
    shift = 6
    ctx.item_model("arcane_crafting_terminal", {
        "parent": "ae2:item/part_base",
        "textures": textures,
        "elements": geometry(shift) + active_layers(shift, 15) + [lights({"texture": "#light", "tintindex": 1, **glow(13)}, shift)],
    })
    ctx.item_definition("arcane_crafting_terminal", ctx.ref("arcane_crafting_terminal", "item"))
    ae2 = ctx.OUT / "assets" / "ae2"
    ctx.write(ae2 / "screens" / "thaumicenergistics" / "arcane_crafting_terminal.json", style())
    texture = ae2 / "textures" / TEXTURE
    texture.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(ctx.ROOT / "src" / "main" / "resources" / "assets" / "thaumicenergistics" / "textures" / "gui" / "arcane.crafting.png", texture)
