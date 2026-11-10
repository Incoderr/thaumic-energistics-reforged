import math

SCALE = 0.047 * 0.625 * 16
HALF = math.pi / 2
FULL = math.pi

OUTER = ((-17, -17, -15), (34, 2, 2), (0, 0))
INNER = ((-15, -15, -15), (30, 2, 2), (0, 4))
GLASS = ((-13, -13, -14), (26, 26, 1), (0, 37))


def rotate(point, rx, ry, rz):
    x, y, z = point
    c, s = round(math.cos(rx)), round(math.sin(rx))
    y, z = y * c - z * s, y * s + z * c
    c, s = round(math.cos(ry)), round(math.sin(ry))
    x, z = x * c + z * s, -x * s + z * c
    c, s = round(math.cos(rz)), round(math.sin(rz))
    x, y = x * c - y * s, x * s + y * c
    return x, y, z


def face_uvs(u, v, dx, dy, dz):
    return {
        (1, 0, 0): (u + dz + dx, v + dz, u + dz + dx + dz, v + dz + dy),
        (-1, 0, 0): (u, v + dz, u + dz, v + dz + dy),
        (0, -1, 0): (u + dz, v, u + dz + dx, v + dz),
        (0, 1, 0): (u + dz + dx, v, u + dz + dx + dx, v + dz),
        (0, 0, -1): (u + dz, v + dz, u + dz + dx, v + dz + dy),
        (0, 0, 1): (u + dz + dx + dz, v + dz, u + dz + dx + dz + dx, v + dz + dy),
    }


DIRECTIONS = {
    (1, 0, 0): "east",
    (-1, 0, 0): "west",
    (0, 1, 0): "up",
    (0, -1, 0): "down",
    (0, 0, 1): "south",
    (0, 0, -1): "north",
}


def element(box, rx, ry, rz, texture):
    (x, y, z), (dx, dy, dz), (u, v) = box
    corners = [rotate(point, rx, ry, rz) for point in ((x, y, z), (x + dx, y + dy, z + dz))]
    low = [min(a, b) for a, b in zip(*corners)]
    high = [max(a, b) for a, b in zip(*corners)]
    faces = {}
    for normal, uv in face_uvs(u, v, dx, dy, dz).items():
        turned = tuple(int(value) for value in rotate(normal, rx, ry, rz))
        faces[DIRECTIONS[turned]] = {"uv": [round(value / 4, 4) for value in uv], "texture": texture}
    return {
        "from": [round(8 + value * SCALE, 4) for value in low],
        "to": [round(8 + value * SCALE, 4) for value in high],
        "faces": faces,
    }


def model_elements():
    elements = []
    for rotation in (0, HALF, -HALF, FULL):
        elements.append(element(OUTER, 0, rotation, 0, "#frame"))
        elements.append(element(((-17, 15, -15), (34, 2, 2), (0, 0)), 0, rotation, 0, "#frame"))
        elements.append(element(OUTER, -rotation, 0, -HALF, "#frame"))
        elements.append(element(OUTER, rotation, 0, HALF, "#frame"))
    for ry, rx in ((0, 0), (HALF, 0), (-HALF, 0), (FULL, 0), (0, HALF), (0, -HALF)):
        elements.append(element(INNER, rx, ry, 0, "#frame"))
        elements.append(element(((-15, 13, -15), (30, 2, 2), (0, 4)), rx, ry, 0, "#frame"))
        elements.append(element(INNER, rx, ry, -HALF, "#frame"))
        elements.append(element(INNER, rx, ry, HALF, "#frame"))
    for ry, rx in ((0, 0), (HALF, 0), (-HALF, 0), (FULL, 0), (0, HALF), (0, -HALF)):
        elements.append(element(GLASS, rx, ry, 0, "#frame"))
    return elements


def generate(ctx):
    name = "arcane_assembler"
    ctx.block_model(name, {
        "parent": "minecraft:block/block",
        "render_type": "minecraft:translucent",
        "textures": {"frame": ctx.ref("arcane.assembler", "models"), "particle": ctx.ref("arcane.assembler.fallback")},
        "elements": model_elements(),
    })
    ctx.write(ctx.OUT / "assets" / "minecraft" / "atlases" / "blocks.json", {
        "sources": [{"type": "single", "resource": ctx.ref("arcane.assembler", "models")}],
    })
    ctx.simple_state(name)
    ctx.block_item(name)
    ctx.self_drop(name)
    ctx.mineable("pickaxe", name)
