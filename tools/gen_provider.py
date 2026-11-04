FACES = ("north", "south", "east", "west", "up", "down")


def cube(start, end, texture, extra=None, cull=False):
    face = {"texture": texture}
    if extra:
        face.update(extra)
    return {"from": start, "to": end, "faces": {name: {**face, "cullface": name} if cull else dict(face) for name in FACES}}


def generate(ctx):
    for kind in ("essentia", "infusion"):
        name = f"{kind}_provider"
        textures = {
            "base": ctx.ref(f"{kind}.provider"),
            "overlay": ctx.ref(f"{kind}.provider.overlay"),
            "particle": ctx.ref(f"{kind}.provider"),
        }
        for state, extra in (("off", {"tintindex": 0}), ("on", {"tintindex": 0, "neoforge_data": {"block_light": 15, "sky_light": 15}})):
            ctx.write(ctx.ASSETS / "models" / "block" / f"{name}_{state}.json", {
                "loader": "neoforge:composite",
                "parent": "minecraft:block/block",
                "textures": textures,
                "children": {
                    "base": {
                        "parent": "minecraft:block/block",
                        "render_type": "minecraft:solid",
                        "textures": textures,
                        "elements": [cube([0, 0, 0], [16, 16, 16], "#base", cull=True)],
                    },
                    "overlay": {
                        "parent": "minecraft:block/block",
                        "render_type": "minecraft:translucent",
                        "textures": textures,
                        "elements": [cube([-0.01, -0.01, -0.01], [16.01, 16.01, 16.01], "#overlay", extra)],
                    },
                },
            })
        ctx.cube_all(name, f"{kind}.provider")
        ctx.blockstate(name, {"variants": {
            "active=false": {"model": ctx.ref(f"{name}_off")},
            "active=true": {"model": ctx.ref(f"{name}_on")},
        }})
        ctx.block_item(name)
        ctx.self_drop(name)
        ctx.mineable("pickaxe", name)
