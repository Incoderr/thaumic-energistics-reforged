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
    ctx.item_model("essentia_terminal", {
        "parent": "ae2:item/display_base",
        "textures": {
            "sides": ctx.ref("parts/essentia.terminal.side"),
            "front_base": ctx.ref("parts/essentia.termina.border"),
            "front_bright": lights["lightsBright"],
            "front_medium": lights["lightsMedium"],
            "front_dark": lights["lightsDark"],
        },
    })
    ctx.item_definition("essentia_terminal", ctx.ref("essentia_terminal", "item"))
