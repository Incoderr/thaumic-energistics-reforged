def generate(ctx):
    parts = ctx.ASSETS / "models" / "part"
    lights = {
        "lightsBright": ctx.ref("parts/essentia.terminal.overlay.light"),
        "lightsMedium": ctx.ref("parts/essentia.terminal.overlay.medium"),
        "lightsDark": ctx.ref("parts/essentia.terminal.overlay.dark"),
    }
    ctx.write(parts / "essentia_terminal_base.json", {
        "parent": "ae2:part/display_base",
        "textures": {
            "sides": ctx.ref("parts/essentia.terminal.side"),
            "front": ctx.ref("parts/essentia.termina.border"),
            "particle": ctx.ref("parts/essentia.terminal.side"),
        },
    })
    ctx.write(parts / "essentia_terminal_on.json", {"parent": "ae2:part/terminal_on", "textures": lights})
    ctx.write(parts / "essentia_terminal_off.json", {"parent": "ae2:part/terminal_off", "textures": lights})
    side = ctx.ref("parts/essentia.terminal.side")
    ctx.item_model("essentia_terminal", {
        "parent": "ae2:item/part_base",
        "textures": {
            "sides": side,
            "back": side,
            "front": ctx.ref("parts/essentia.termina.border"),
            "dark": lights["lightsDark"],
            "medium": lights["lightsMedium"],
            "light": lights["lightsBright"],
            "particle": side,
        },
        "elements": [
            {
                "from": [2, 2, 7],
                "to": [14, 14, 9],
                "faces": {
                    "north": {"uv": [2, 2, 14, 14], "texture": "#front"},
                    "east": {"uv": [14, 2, 16, 14], "texture": "#sides"},
                    "south": {"uv": [2, 2, 14, 14], "texture": "#back"},
                    "west": {"uv": [0, 2, 2, 14], "texture": "#sides"},
                    "up": {"uv": [2, 0, 14, 2], "texture": "#sides"},
                    "down": {"uv": [2, 14, 14, 16], "texture": "#sides"},
                },
            },
            *[
                {
                    "from": [3, 3, depth],
                    "to": [13, 13, 7],
                    "faces": {"north": {"uv": [3, 3, 13, 13], "texture": f"#{layer}", "tintindex": tint}},
                }
                for layer, tint, depth in (("dark", 1, 6.98), ("medium", 2, 6.96), ("light", 3, 6.94))
            ],
            {
                "from": [4, 4, 9],
                "to": [12, 12, 10],
                "faces": {
                    "east": {"uv": [6, 4, 7, 12], "texture": "#sides"},
                    "south": {"uv": [4, 4, 12, 12], "texture": "#back"},
                    "west": {"uv": [9, 4, 10, 12], "texture": "#sides"},
                    "up": {"uv": [4, 9, 12, 10], "texture": "#sides"},
                    "down": {"uv": [4, 6, 12, 7], "texture": "#sides"},
                },
            },
        ],
    })
    ctx.item_definition("essentia_terminal", ctx.ref("essentia_terminal", "item"))
