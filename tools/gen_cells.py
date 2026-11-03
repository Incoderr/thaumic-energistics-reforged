SIZES = ["1k", "4k", "16k", "64k"]


def generate(ctx):
    ctx.generated_item("essentia_cell_housing", "essentia.cell.casing")
    ctx.generated_item("creative_essentia_cell", "essentia.cell.creative")
    for size in SIZES:
        ctx.generated_item(f"essentia_storage_component_{size}", f"storage.component.{size}")
        ctx.generated_item(f"essentia_cell_{size}", f"essentia.cell.{size}")

    casing = {"W": "thaumcraft:warded_glass", "R": "minecraft:redstone", "T": "thaumcraft:thaumium_ingot"}
    ctx.shaped("essentia_cell_housing", "thaumicenergistics:essentia_cell_housing", 1, ["WRW", "R R", "TTT"], casing)
    for size in SIZES:
        component = f"thaumicenergistics:essentia_storage_component_{size}"
        cell = f"thaumicenergistics:essentia_cell_{size}"
        ctx.shaped(f"essentia_cell_{size}", cell, 1, ["WRW", "RCR", "TTT"], {**casing, "C": component})
        ctx.shapeless(f"essentia_cell_{size}_shapeless", cell, 1, [component, "thaumicenergistics:essentia_cell_housing"])
