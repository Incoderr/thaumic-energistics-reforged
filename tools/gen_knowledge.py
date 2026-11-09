def generate(ctx):
    name = "knowledge_inscriber"
    ctx.cube_bottom_top(name, "knowledge.inscriber.top", "knowledge.inscriber.bottom", "knowledge.inscriber.side")
    ctx.simple_state(name)
    ctx.block_item(name)
    ctx.self_drop(name)
    ctx.mineable("pickaxe", name)
    ctx.generated_item("knowledge_core", "knowledge.core")
