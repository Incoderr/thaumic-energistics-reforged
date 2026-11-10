def generate(ctx):
    name = "arcane_assembler"
    ctx.block_model(name, {"textures": {"particle": ctx.ref("arcane.assembler.fallback")}})
    ctx.write(ctx.ASSETS / "models" / "item" / f"{name}.json", {
        "parent": "minecraft:builtin/entity",
        "textures": {"particle": ctx.ref("arcane.assembler.fallback")},
        "display": {
            "gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [0.625, 0.625, 0.625]},
            "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.25, 0.25, 0.25]},
            "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.5, 0.5, 0.5]},
            "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.375, 0.375, 0.375]},
            "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [0.4, 0.4, 0.4]},
            "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 0, 0], "scale": [0.4, 0.4, 0.4]},
        },
    })
    ctx.simple_state(name)
    ctx.self_drop(name)
    ctx.mineable("pickaxe", name)
