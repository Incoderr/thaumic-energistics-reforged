BLOCKS = (
    "amber_block", "amber_bricks", "warded_glass", "obsidian_tile", "thaumium_block", "tallow_block",
    "arcane_stone", "arcane_stone_bricks", "ancient_stone", "ancient_rock", "crusted_stone",
    "greatwood_planks", "silverwood_planks", "crusted_taint", "flesh_block",
)


def generate(ctx):
    for name in BLOCKS:
        ctx.block_tags["ae2:whitelisted/facades"].add(f"thaumcraft:{name}")
