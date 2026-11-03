def generate(ctx):
    name = "essentia_cell_workbench"
    ctx.cube_bottom_top(name, "essentia.cell.workbench.top", "essentia.cell.workbench.bottom", "essentia.cell.workbench.side")
    ctx.simple_state(name)
    ctx.block_item(name)
    ctx.self_drop(name)
    ctx.mineable("pickaxe", name)
    ctx.shapeless(name, f"thaumicenergistics:{name}", 1, ["thaumicenergistics:essentia_storage_component_1k", "ae2:cell_workbench"])
